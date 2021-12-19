package dnr;

import simulation.*;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class DNR {
    /**
     * Class implementing the Data-Driven Norm Revision Module aimed at revising a configuration of norms
     * in order to improve its alignment with the MAS objectives w.r.t. a dataset of traces labeled as positive or
     * negative w.r.t. the MAS objectives
     */
    private final Random r;
    String  metric;
    int samples;
    String idMaxSpeedNorm;
    String idMinDistNorm;

    public DNR(int samples, String metric, String idMaxSpeedNorm, String idMinDistNorm, Random r) {
        this.idMaxSpeedNorm = idMaxSpeedNorm;
        this.idMinDistNorm = idMinDistNorm;
        this.samples = samples;
        this.metric = metric;
        this.r = r;
    }

    public Configuration runDNR(Configuration systemConfig, ArrayList<Trace> labeledTraces, String revStrategy) {
        /**
         * Function to execute the data-driven norm revision.
         * It performs two steps:
         * Synthesis step. In this step, DNR synthesises a set of candidate revisions of the norms
         * Selection step. In this step, DNR selects a revised set of norms from the candidate
         * norms obtained in the synthesis step.
         * Note that the Monte Carlo approach is hidden in the selection step just for practical purposes
         *
         */
        Configuration revisedConf = systemConfig;
        /*Synthesis Step */
        LinkedHashMap<String, ArrayList<DNFNorm>> candidateNorms =
                synthesis(revStrategy, systemConfig, labeledTraces); //traces used for the suggestion, traces used for generating the new simulation.norms, traces used for choosing between different simulation.norms
        /*Selection Step */
        LinkedHashMap<Configuration, Double> new_configs_with_quality = selection(systemConfig, candidateNorms, labeledTraces); //they are already sorted
        //return the best conf
        return getBestConfigFromSelStep(new_configs_with_quality);
    }

    public LinkedHashMap<String, ArrayList<DNFNorm>> synthesis(String revision_strategy, Configuration config, ArrayList<Trace> traces) {
        /**
         * The synthesis step of DNR
         */
        LinkedHashMap<String, ArrayList<DNFNorm>> new_possible_norms = new LinkedHashMap<>();
        if(config!=null) {
            Iterator<Map.Entry<String, DNFNorm>> iter = config.getMap().entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, DNFNorm> n_conf = iter.next();
                new_possible_norms.put(n_conf.getKey(), new ArrayList<>(reviseNorm(n_conf.getValue(), n_conf.getKey(), config.getNormsTypes(), revision_strategy, traces)));
            }
        }
        return new_possible_norms;
    }

    public LinkedHashMap<Configuration, Double> selection (Configuration currConfig, LinkedHashMap<String, ArrayList<DNFNorm>> candidateNorms, ArrayList<Trace> traces) {
        /**
         * The selection step of DNR
         */
        LinkedHashMap<Configuration, Double> new_configs_with_quality = new LinkedHashMap<>();
        if(currConfig!=null && !candidateNorms.isEmpty()) {
            if (this.samples == -1) {
                /*
                  Case where we do not want to adopt a Monte Carlo approach but we want to consider all possible cominations of configurations
                 */
                ArrayList<Configuration> new_possible_configurations = new ArrayList<>();
                generateCombinations(candidateNorms, new ArrayList<>(candidateNorms.keySet()), new_possible_configurations, 0, currConfig);
                for (Configuration possible_conf : new_possible_configurations) {
                    if (!possible_conf.isEmpty())
                        new_configs_with_quality.put(possible_conf, getConfigQuality(possible_conf, traces));
                }
            } else {
                /**here the case of Monte Carlo
                 * Note: instead of keeping just the best I store all of them in new_configs_with_quality, and then sort them later*/
                for (int k = 0; k < samples; k++) {
                    //sample each possible norm
                    LinkedHashMap<String, DNFNorm> map = new LinkedHashMap<>();
                    for (String normid : candidateNorms.keySet()) {
                        ArrayList<DNFNorm> norm_candidate_list = candidateNorms.get(normid);
                        DNFNorm sampled_norm = norm_candidate_list.get(r.nextInt(norm_candidate_list.size()));
                        map.put(normid, sampled_norm);
                    }
                    //create a configuration
                    Configuration k_conf = new Configuration(map, currConfig.getNormsTypes());
                    //compute accuracy of configuration
                    if (!k_conf.isEmpty())
                        new_configs_with_quality.put(k_conf, getConfigQuality(k_conf, traces));
                    //if new accuracy is better than best_conf_accuracy update the bests
                    // note I don't do the above comment here, and I sort them later (this way I can keep track of all the
                    //sampled configurations
                }
            }
        }
        //here I have now all config with their quality, I sort them
        List<Map.Entry<Configuration, Double>> entries = new ArrayList<>(new_configs_with_quality.entrySet());
        Collections.sort(entries,
                (lhs, rhs) -> (rhs.getValue() - lhs.getValue()) > 0 ? 1 : ((rhs.getValue() - lhs.getValue()) == 0 ? 0 : -1));
        new_configs_with_quality.clear();
        for(Map.Entry<Configuration, Double> e : entries) { //now I put, sorted by value
            new_configs_with_quality.put(e.getKey(), e.getValue());
        }
        return new_configs_with_quality;
    }

    public Configuration getBestConfigFromSelStep(LinkedHashMap<Configuration, Double> new_configs_with_quality) {
        /**
         * Returns the best configuration from the set of configurations obtained with selection
         * Note: important, they are already sorted
         */
        Configuration best_conf = null;
        if(new_configs_with_quality.size()>0)
            best_conf = new_configs_with_quality.keySet().iterator().next(); //return the first
        //return the best conf
        return best_conf;
    }

    void generateCombinations(LinkedHashMap<String, ArrayList<DNFNorm>> lists, ArrayList<String> keys, ArrayList<Configuration> result, int depth, Configuration current ) {
        /**
         * Function used to generate all possible combinations of revised norms in case of multiple norms
         */
        if (depth == lists.size()) {
            result.add(current);
            return;
        }
        for (int i = 0; i < lists.get(keys.get(depth)).size(); i++) {
            LinkedHashMap<String, DNFNorm> new_current_map = new LinkedHashMap<>(current.getMap());
            new_current_map.put(keys.get(depth), lists.get(keys.get(depth)).get(i));
            Configuration new_current = new Configuration(new_current_map, current.getNormsTypes());
            generateCombinations(lists, keys, result, depth + 1, new_current);
        }
    }

    ArrayList<Trace> getViolatingTraces( ArrayList<Trace> traces, DNFNorm n ) {
        return traces.stream()
                .filter(t -> (n.isViol(t)>-1))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    ArrayList<Trace> getObeyingTraces( ArrayList<Trace> traces, DNFNorm n ) {
        return traces.stream()
                .filter(t -> (n.isViol(t)==-1))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    LinkedHashMap<String, Set<State>> getStates(ArrayList<Trace> traces, DNFNorm norm) {
        /**
         * Function to get six different types of states form the traces in the dataset
         */
        ArrayList<Trace> N = getViolatingTraces(traces, norm);
        ArrayList<Trace> P = getObeyingTraces(traces, norm);

        Set<State> CS = new HashSet<>();
        Set<State> CPS = new HashSet<>();
        Set<State> PS = new HashSet<>();
        Set<State> OPS = new HashSet<>();
        Set<State> IPS = new HashSet<>();
        Set<State> DS = new HashSet<>();
        for(Trace t : N) {
            CS.addAll(norm.getDetachmentStates(t));
            CPS.addAll(norm.getStatesBetweenCondandProh(t));
            PS.addAll(norm.getViolatingStates(t));
        }
        for(Trace t : P) {
            OPS.addAll(norm.getStatesOutsideWindowWhereProhHold(t));
            IPS.addAll(norm.getStatesBetweenCondandDead(t));
            DS.addAll(norm.getStatesWhereDeadlineHolds(t));
        }

        LinkedHashMap<String, Set<State>> states_sets = new LinkedHashMap<>();
        states_sets.put("CS", CS);
        states_sets.put("OPS", OPS);
        states_sets.put("IPS", IPS);
        states_sets.put("PS", PS);
        states_sets.put("CPS", CPS);
        states_sets.put("DS", DS);
        return states_sets;
    }

    Set<DNFNorm> reviseNorm( DNFNorm norm, String normID, LinkedHashMap<String, Class> normsTypes, String revision_type, ArrayList<Trace> traces ) {
        /**
         * Function that invokes the correct revisionOperations based on the revision_type
         */
        try {
            Constructor<DNFNorm> cons = normsTypes.get(normID).getConstructor(String.class, List.class, List.class, List.class, Random.class);

            LinkedHashMap<String, Set<State>> states = getStates(traces, norm);

            switch(revision_type) {
                case "strengthening":
                    System.out.println("Searching for strengthening of "+norm);
                    return strengthenNorm(norm, cons, normID, states);
//                    return strengthenNorm(norm, cons, normID, TP, FP, TN, FN, P, N);
                case "weakening":
                    System.out.println("Searching for weakening of "+norm);
                    return weakenNorm(norm, cons, normID, states);
//                return weakenNorm(norm, cons, normID, TP, FP, TN, FN, P, N);
                case "alteration":
                    System.out.println("Searching for alterations of "+norm);
                    return alterNorm(norm, cons, normID, states);
//                    return alterNorm(norm, cons, normID, TP, FP, TN, FN, P, N);
                case "-":
                    /*Leave the norm as it is */
                    System.out.println("Leaving "+norm+" as it is.");
                    return new HashSet<>(List.of(norm));
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return null;
    }

    Set<DNFNorm> alterNorm(DNFNorm norm, Constructor<DNFNorm> cons, String normID, LinkedHashMap<String, Set<State>> states) {
        /**
         * Function to synthesise alterations of norm norm
         */
        try {
            Set<DNFNorm> possibleAlterations = new HashSet<>();
            Set<List<Conjunction>> new_possible_cond = norm.getMoreSpecificFormulas(norm.getCondition(), states.get("CS"), "cond");
            new_possible_cond.addAll(norm.getLessSpecificFormulas(norm.getCondition(), states.get("OPS"), "cond"));

            Set<List<Conjunction>> new_possible_proh = norm.getMoreSpecificFormulas(norm.getProhibition(), states.get("PS"), "proh");
            new_possible_proh.addAll(norm.getLessSpecificFormulas(norm.getProhibition(), states.get("IPS"), "proh"));

            Set<List<Conjunction>> new_possible_dead = norm.getMoreSpecificFormulas(norm.getDeadline(), states.get("DS"), "dead");
            new_possible_dead.addAll(norm.getLessSpecificFormulas(norm.getDeadline(), states.get("CPS"), "dead"));

            for(List<Conjunction> phi_c_1 : new_possible_cond) {
                for(List<Conjunction> phi_p_1 : new_possible_proh) {
                    for(List<Conjunction> phi_d_1 : new_possible_dead) {
                        DNFNorm n1 = cons.newInstance(normID, phi_c_1, phi_p_1, phi_d_1, r);
                        if(!n1.isEmpty()) {
                            possibleAlterations.add(n1);
                        }
                    }
                }
            }
            return possibleAlterations;
        } catch(Exception e) {
            System.out.println("ERROR IN CREATING A NEW INSTANCE OF NORM DYNAMICALLY during alteration");
            e.printStackTrace();
        }
        return null;

    }

    Set<DNFNorm> strengthenNorm(DNFNorm norm, Constructor<DNFNorm> cons, String normID, LinkedHashMap<String, Set<State>> states) {
        /**
         * Function to synthesize strengthenings of the norm norm
         */
        try {
            Set<DNFNorm> possibleStrengthening = new HashSet<>();
            Set<List<Conjunction>> less_spec_cond = norm.getLessSpecificFormulas(norm.getCondition(), states.get("OPS"), "cond");
            Set<List<Conjunction>> less_spec_proh = norm.getLessSpecificFormulas(norm.getProhibition(), states.get("IPS"), "proh");
            Set<List<Conjunction>> more_spec_dead = norm.getMoreSpecificFormulas(norm.getDeadline(), states.get("DS"), "dead");

            for(List<Conjunction> phi_c_1 : less_spec_cond) {
                for(List<Conjunction> phi_p_1 : less_spec_proh) {
                    for(List<Conjunction> phi_d_1 : more_spec_dead) {
                        DNFNorm n1 = cons.newInstance(normID, phi_c_1, phi_p_1, phi_d_1, r);
                        if(!n1.isEmpty()) {
                            possibleStrengthening.add(n1);
                        }
                    }
                }
            }
            return possibleStrengthening;
        } catch(Exception e) {
            System.out.println("ERROR IN CREATING A NEW INSTANCE OF NORM DYNAMICALLY during strengthening");
            e.printStackTrace();
        }
        return null;
    }

    Set<DNFNorm> weakenNorm(DNFNorm norm, Constructor<DNFNorm> cons, String normID, LinkedHashMap<String, Set<State>> states) {
        /**
         * Function to synthesise weakenings of norm norm
         */
        try {
            Set<DNFNorm> possibleWeakening = new HashSet<>();
            Set<List<Conjunction>> more_spec_cond = norm.getMoreSpecificFormulas(norm.getCondition(), states.get("CS"), "cond");
            Set<List<Conjunction>> more_spec_proh = norm.getMoreSpecificFormulas(norm.getProhibition(), states.get("PS"), "proh");
            Set<List<Conjunction>> less_spec_dead = norm.getLessSpecificFormulas(norm.getDeadline(), states.get("CPS"), "dead");
            for(List<Conjunction> phi_c_1 : more_spec_cond) {
                for(List<Conjunction> phi_p_1 : more_spec_proh) {
                    for(List<Conjunction> phi_d_1 : less_spec_dead) {
                        //create the new norm
                        DNFNorm n1 = cons.newInstance(normID, phi_c_1, phi_p_1, phi_d_1, r);
                        if(!n1.isEmpty()) {
                            possibleWeakening.add(n1);
                        }
                    }
                }
            }
            return possibleWeakening;
        } catch(Exception e) {
            System.out.println("ERROR IN CREATING A NEW INSTANCE OF NORM DYNAMICALLY during weakening");
            e.printStackTrace();
        }
        return null;
    }

    ArrayList<Integer> getConfusionMatrix(DNFNorm n, List<Trace> traces ) {
        /**
         * Returns a list composed of the 4 elements (TP, FP, TN, FN) composing a confusion matrix which describes
         * how well norm n characterizes the traces w.r.t. their labeling
         */
        ArrayList<Integer> conf_matrix = new ArrayList<>();
        if(n==null) {
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            conf_matrix.add(-1);
        }
        else {
            int tp = (int) traces.stream().filter(t -> (t.getObjEval() && n.isViol(t) == -1)).count();
            int fp = (int) traces.stream().filter(t -> (!t.getObjEval() && n.isViol(t) == -1)).count();
            int tn = (int) traces.stream().filter(t -> (!t.getObjEval() && n.isViol(t) > -1)).count();
            int fn = (int) traces.stream().filter(t -> (t.getObjEval() && n.isViol(t) > -1)).count();
            conf_matrix.add(tp);
            conf_matrix.add(fp);
            conf_matrix.add(tn);
            conf_matrix.add(fn);
        }
        return conf_matrix;
    }
    
    ArrayList<Integer> getConfusionMatrices(Configuration c, int nr_norms, List<Trace> traces ) {
        /**
         * Returns a confusion matrix for every norm in the configuration c
         */
        ArrayList<Integer> conf_matrices = new ArrayList<>();

        if(c==null) {
            for(int i=0;i<nr_norms;i++)
                conf_matrices.addAll(getConfusionMatrix(null, traces));
        }
        else {
            Iterator<Map.Entry<String, DNFNorm>> iter = c.getMap().entrySet().iterator();
            while (iter.hasNext()) { //this loops for all simulation.norms
                Map.Entry<String, DNFNorm> n_conf = iter.next();
                conf_matrices.addAll(getConfusionMatrix(n_conf.getValue(), traces));
            }
        }
        return conf_matrices;
    }

    ArrayList<Integer> getTwoLabelConfusionMatrix(Configuration c, List<Trace> traces ) {
        /**
         * Returns a two-labels confusion matrix which considers both norms (assumes 2 norms) at the same time
         */
        ArrayList<Integer> conf_matrix = new ArrayList<>();

        if(c==null) {
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            conf_matrix.add(-1);
            return conf_matrix;
        }

        DNFNorm n1 = c.get(idMaxSpeedNorm);
        DNFNorm n2 = c.get(idMinDistNorm);
        //positive
        //n1 ob
        int pfc =  (int) traces.stream().filter(t -> (t.getObjEval() && n1.isViol(t)==-1 && n2.isViol(t)==-1)).count(); //positive fully correct
        int ppc1 =  (int) traces.stream().filter(t -> (t.getObjEval() && n1.isViol(t)==-1 && n2.isViol(t)>-1)).count(); //positive partly correct (n1)
        //n1 viol
        int ppc2 =  (int) traces.stream().filter(t -> (t.getObjEval() && n1.isViol(t)>-1 && n2.isViol(t)==-1)).count(); //positive partly correct (n2)
        int pfw =  (int) traces.stream().filter(t -> (t.getObjEval() && n1.isViol(t)>-1 && n2.isViol(t)>-1)).count(); //positive fully wrong
        //negative
        //n1 ob
        int nfw =  (int) traces.stream().filter(t -> (!t.getObjEval() && n1.isViol(t)==-1 && n2.isViol(t)==-1)).count(); //negative fully wrong
        int npc2 =  (int) traces.stream().filter(t -> (!t.getObjEval() && n1.isViol(t)==-1 && n2.isViol(t)>-1)).count(); //negative partly correct (n2)
        //n1 viol
        int npc1 =  (int) traces.stream().filter(t -> (!t.getObjEval() && n1.isViol(t)>-1 && n2.isViol(t)==-1)).count(); //negative partly correct (n1)
        int nfc = (int)  traces.stream().filter(t -> (!t.getObjEval() && n1.isViol(t)>-1 && n2.isViol(t)>-1)).count(); //negative fully correct
        conf_matrix.add(pfc);
        conf_matrix.add(ppc1);
        conf_matrix.add(ppc2);
        conf_matrix.add(pfw);
        conf_matrix.add(nfw);
        conf_matrix.add(npc2);
        conf_matrix.add(npc1);
        conf_matrix.add(nfc);

        return conf_matrix;
    }

    double getMultiLabelAccuracy(Configuration c, List<Trace> traces ) {
        /**
         * Returns the multi-label accuracy for a given two-norms configuration
         */
        if(traces.size()>0) {
            int n = c.getMap().size();
            double ml_acc = 0.0;
            for(Trace tr : traces) {
                //the actual values
                int obj_eval = tr.getObjEval()?1:0;
                int [] y = new int[n];
                for(int i=0; i<n;i++) y[i]=obj_eval;//the actual value for the classification, I repeat it n times, with n =nr simulation.norms

                //the predicted values
                int [] y_pred = new int[n];
                Iterator<Map.Entry<String, DNFNorm>> iter = c.getMap().entrySet().iterator();
                int i=0;
                while (iter.hasNext()) { //this loops for all simulation.norms
                    Map.Entry<String, DNFNorm> n_conf = iter.next();
                    y_pred[i] = n_conf.getValue().isViol(tr)>-1?0:1; //if viol I give class 0, otherwise class 1
                    i++;
                }
                int union = 0;
                int inter = 0;
                for(int j = 0; j < y.length; j++) {
                    if (y[j] == 1 || y_pred[j] == 1)
                        union++;
                    if (y[j] == 1 && y_pred[j] == 1)
                        inter++;
                }
                ml_acc = ml_acc + ((union > 0) ? (double)inter / (double)union : 1.0);
            }
            return ml_acc/(double)traces.size();
        }
        return -1;
    }


    double getNormQuality(String metric, DNFNorm n, List<Trace> traces) {
        /**
         * Returns the quality of norm n w.r.t. the metric metric
         */
        switch(metric) {
            case "accuracy":
                return getAccuracy(n,traces);
        }
        System.out.println("WARNING: metric not found.");
        return -1.0;
    }

    double getAccuracy( DNFNorm n, List<Trace> traces ) {
        /**
         * Returns the accuracy of norm n
         */
        if(traces.size()>0) {
            double correct_traces = traces.stream().filter(t -> (t.getObjEval() && n.isViol(t)==-1) || (!t.getObjEval() && n.isViol(t)>-1)).count();
            return correct_traces/traces.size();
        }
        return -1;
    }


    double getConfigQuality(Configuration c, List<Trace> traces) {
        /**
         * Returns the quality of a configuration w.r.t. a  given metric
         */
        if(c==null)
            return -1.0;
        double quality = 0.0;
        /* Case when I'm not interested in the selection step but only in the synthesis step*/
        if(metric.equals("random"))
            return quality;
        /*CASE MULTILABEL*/
        if(metric.equals("mlacc")) {
            quality = getMultiLabelAccuracy(c, traces);
        }
        else { /*this is the case for the normal metrics, like accuracy, f1, etc.*/
            Iterator<Map.Entry<String, DNFNorm>> iter = c.getMap().entrySet().iterator();
            while (iter.hasNext()) { //loops for all simulation.norms
                Map.Entry<String, DNFNorm> n_conf = iter.next(); //get the norm
                double norm_quality = getNormQuality(metric, n_conf.getValue(), traces); //calculate the quality
                quality = quality + norm_quality;
            }
            if(c.getMap().size()>0) { //if there is more than one norm I calculate the final quality as the average (so I just divide by the number)
                quality = quality/c.getMap().size();
            }
        }
        return quality;
    }

    public LinkedHashMap <String, ArrayList<String>> evalConfiguration(int nr_norms, Configuration config, ArrayList<Trace> labeledTraces, boolean traintestsplit, boolean independent_set_test, ArrayList<Trace> independent_labeledTraces, boolean twoLabelConfMatr) {
        /**
         * Function that evaluates a configuration of norms w.r.t. a labeled dataset of traces.
         * It returns an arraylist of strings, where each string contains a representation of the confusion matrices for the norms
         */
        ArrayList<Trace> train_traces = labeledTraces;
        ArrayList<Trace> test_traces = labeledTraces;
        if(traintestsplit) {
            Collections.shuffle(labeledTraces);
            int train_size = (int) Math.round(labeledTraces.size() * 0.75);
            train_traces = new ArrayList<>(labeledTraces.subList(0, train_size));
            test_traces = new ArrayList<>(labeledTraces.subList(train_size, labeledTraces.size()));
        }
        else {
            if(independent_set_test) {
                test_traces = independent_labeledTraces;
            }
        }
        LinkedHashMap <String, ArrayList<String>> eval = new LinkedHashMap<>();
        ArrayList<String> l = new ArrayList<>();
        /** CONFIG **/
        /** (1) the quality of the config on the train traces **/
        l.add(getConfigQuality(config, train_traces)+""); //1element
        if (nr_norms == 2) {
            if (twoLabelConfMatr) {
                for (int v : getTwoLabelConfusionMatrix(config, train_traces)) //8elements in total
                    l.add(v + "");
            } else {
                for (int v : getConfusionMatrices(config, nr_norms, train_traces)) //4elements for each norm
                    l.add(v + "");
            }
        }
        else {
            for(int v : getConfusionMatrix(config.get("MSN"), train_traces))
                l.add(v+"");
        }
        /** (2) the quality of the config on the test traces **/
        l.add(getConfigQuality(config, test_traces)+"");
        if (nr_norms == 2) {
            if (twoLabelConfMatr) {
                for (int v : getTwoLabelConfusionMatrix(config, test_traces)) //8elements in total
                    l.add(v + "");
            } else {
                for (int v : getConfusionMatrices(config,nr_norms, test_traces)) //4elements for each norm
                    l.add(v + "");
            }
        } else {
            for(int v : getConfusionMatrix(config.get("MSN"), test_traces))
                l.add(v+"");
        }
        //putting everything in the eval map that associates it with the metric
        eval.put(metric, l);
        return eval;
    }


    public LinkedHashMap <String, ArrayList<String>> evalConfigurations(int nr_norms, Configuration initConfig, Configuration revConfig, ArrayList<Trace> labeledTraces, boolean traintestsplit, boolean independent_set_test, ArrayList<Trace> independent_labeledTraces, boolean twoLabelConfMatr) {
        /**
         * Function that compares two different configurations
         */
        ArrayList<Trace> train_traces = labeledTraces;
        ArrayList<Trace> test_traces = labeledTraces;
        if(traintestsplit) {
            Collections.shuffle(labeledTraces);
            int train_size = (int) Math.round(labeledTraces.size() * 0.75);
            train_traces = new ArrayList<>(labeledTraces.subList(0, train_size));
            test_traces = new ArrayList<>(labeledTraces.subList(train_size, labeledTraces.size()));
        }
        else {
            if(independent_set_test) {
                test_traces = independent_labeledTraces;
            }
        }

        LinkedHashMap <String, ArrayList<String>> eval = new LinkedHashMap<>();
        ArrayList<String> l = new ArrayList<>();
        /**INITIAL CONFIG **/
        DNFNorm msnnorm = null;
        if(initConfig!=null) {
            msnnorm = initConfig.get("MSN");
        }
        /** (1) the quality of the initial config on the train traces **/
        l.add(getConfigQuality(initConfig, train_traces) + ""); //1element
        if (nr_norms == 2) {
            if (twoLabelConfMatr) {
                for (int v : getTwoLabelConfusionMatrix(initConfig, train_traces)) //8elements in total
                    l.add(v + "");
            } else {
                for (int v : getConfusionMatrices(initConfig, nr_norms, train_traces)) //4elements for each norm
                    l.add(v + "");
            }
        } else {
            for (int v : getConfusionMatrix(msnnorm, train_traces))
                l.add(v + "");
        }
        /** (2) the quality of the initial config on the test traces **/
        l.add(getConfigQuality(initConfig, test_traces) + "");
        if (nr_norms == 2) {
            if (twoLabelConfMatr) {
                for (int v : getTwoLabelConfusionMatrix(initConfig, test_traces)) //8elements in total
                    l.add(v + "");
            } else {
                for (int v : getConfusionMatrices(initConfig,nr_norms, test_traces)) //4elements for each norm
                    l.add(v + "");
            }
        } else {
            for (int v : getConfusionMatrix(msnnorm, test_traces))
                l.add(v + "");
        }


        /**REVISED CONFIG **/
        /** (3) the quality of the new config on the train traces **/
        if(revConfig != null) {
            l.add(getConfigQuality(revConfig, train_traces) + ""); //here I use the test_traces
            if (nr_norms == 2) {
                if (twoLabelConfMatr) {
                    for (int v : getTwoLabelConfusionMatrix(revConfig, train_traces)) //8elements in total
                        l.add(v + "");
                } else {
                    for (int v : getConfusionMatrices(revConfig, nr_norms,train_traces)) //4elements for each norm
                        l.add(v + "");
                }
            } else {
                for (int v : getConfusionMatrix(revConfig.get("MSN"), train_traces))
                    l.add(v + "");
            }
        }
        else { /** (or the same quality of the initial config if no new config is found) **/
            l.add(getConfigQuality(initConfig, test_traces)+"");
            if (nr_norms == 2) {
                for (int v : getConfusionMatrices(initConfig, nr_norms,train_traces))
                    l.add(v + "");
            }
            else
                for(int v : getConfusionMatrix(msnnorm, train_traces))
                    l.add(v+"");
        }
        /** (4) the quality of the new config on the test traces (or the same quality of the initial config if no new config is found) **/
        if(revConfig != null) {
            l.add(getConfigQuality(revConfig, test_traces)+""); //here I use the test_traces
            if (nr_norms == 2) {
                if (twoLabelConfMatr) {
                    for (int v : getTwoLabelConfusionMatrix(revConfig, test_traces)) //8elements in total
                        l.add(v + "");
                } else {
                    for (int v : getConfusionMatrices(revConfig, nr_norms,test_traces)) //4elements for each norm
                        l.add(v + "");
                }
            } else {
                for(int v : getConfusionMatrix(revConfig.get("MSN"), test_traces))
                    l.add(v+"");
            }
        }
        else {
            l.add(getConfigQuality(initConfig, test_traces)+"");
            if (nr_norms == 2) {
                for (int v : getConfusionMatrices(initConfig, nr_norms,test_traces))
                    l.add(v + "");
            }
            else
                for(int v : getConfusionMatrix(msnnorm, test_traces))
                    l.add(v+"");
        }
         //putting everything in the eval map that associates it with the metric
        eval.put(metric, l);
        return eval;
    }


    public void setMetric(String metric) {
        /**
         * Sets the metric to be used to evaluate configurations (accuracy)
         */
        this.metric = metric;
    }

}
