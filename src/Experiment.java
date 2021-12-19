import dnr.DNR;
import masobjeval.MASObjectivesEvaluator;
import simulation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.text.SimpleDateFormat;
import java.util.*;

public class Experiment {
    public static void main (String [] args) {
        /**
         * Creating folder for experiments results
         */
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String folder_name = "output"+System.getProperty("file.separator")+"experiments_"+timestamp+System.getProperty("file.separator");
        File folder = new File(folder_name);
        folder.mkdirs();

        /**
         * Creating and running the simulation for every trial
         */
        LinkedHashMap <String, ArrayList<Trace>> triedConfigsSimTraces = new LinkedHashMap<String, ArrayList<Trace>>();

        String idMaxSpeedNorm = "MSN";
        String idMinDistNorm = "MDN";
        int dnr_samples = -1;

        boolean logTwoLabelConfMatr = true;
        boolean isNormObj = true;
        double  tco2_indiv = 100;
        double  ttt_indiv = 45;
        double t_oa = 1.1;
        double  trucks = 0.5;
        double  viol = 0.25;
        int  observationPeriod = 1500;
        double  vehiclesRate = 2;
//        boolean [] traintestsplits = {false, true};
        boolean [] traintestsplits = {false};
//        boolean independent_set_test = true;
        boolean independent_set_test = false;
        int trials = 100;
//        int trials = 1;
        String [] rev_types = {"weakening", "strengthening", "alteration"};
//        String [] rev_types = {"alteration"};
//        int [] nrs_norms = {1, 2};
        int [] nrs_norms = {1};
//        int [] nrs_norms = {2};
        String dnr_metric = "accuracy"; // "mlacc", "accuracy"; //note mlacc works only if nr_norms is 2
        int nr_repeated_revision = 4; //4
        boolean log_synth = false;

        for(int nr_norms: nrs_norms) {
            Random r = new Random();
            String filename = "results_" + nr_norms + "norms_";
            File resultsfile = new File(folder_name + System.getProperty("file.separator") + filename + ".metrics.csv");
            initCSV(resultsfile, nr_norms, logTwoLabelConfMatr);
            for (int trial = 1; trial <= trials; trial++) {
                System.out.println("Trial "+ trial);
                r.setSeed((long) trial * 12345789);

                /** Define the initial simulation.norms (max 2 types supported for now)
                 * **/
                LinkedHashMap<String, Class> normsTypes = new LinkedHashMap<>();
                normsTypes.put(idMaxSpeedNorm, MaxSpeedNorm.class);
                if(nr_norms>1) {
                    normsTypes.put(idMinDistNorm, MinDistNorm.class);
                }
                /**
                 * Create the initial configuration of simulation.norms by randomly sampling .
                 * Each norm is randomly sampled from its language and added to the configuration
                 * systemConfig ccontains the system configuration
                 */
                Configuration systemConfig = new Configuration();
                LinkedHashMap<String, DNFNorm> configMap = new LinkedHashMap<>();
                try {
                    for (String normID : normsTypes.keySet()) {
                        Constructor<DNFNorm> cons = normsTypes.get(normID).getConstructor(String.class, Random.class);
                        configMap.put(normID, cons.newInstance(normID, r));
                    }
                    systemConfig = new Configuration(configMap, normsTypes);
                    //                System.out.println(exp_metric_type+"_"+exp_rev_type+"_"+iteration+": currConfig = "+systemConfig.toString());
                } catch (Exception e) {
                    System.out.println("ERROR IN CREATING A NEW INSTANCE OF NORM DYNAMICALLY");
                }

                /**
                 * Run the simulation with the given parameters, and retrieve the dataset of traces
                 */
                MASObjectivesEvaluator maoe = new MASObjectivesEvaluator(isNormObj, tco2_indiv, ttt_indiv, t_oa, r);
                System.out.println("Running the simulation ...");
                ArrayList<Trace> independentLabeledTraces = new ArrayList<>();
                if(independent_set_test) {
                    System.out.println("Running first the simulation with no norms, so I will use this as an independent test set");
                    HighwaySimulation s = new HighwaySimulation(new Configuration(), trucks, viol, observationPeriod, vehiclesRate, r);
                    ArrayList<Trace> independentTraces = s.run();
                    independentLabeledTraces = maoe.labelTraces(independentTraces);
                }

                HighwaySimulation s = new HighwaySimulation(systemConfig, trucks, viol, observationPeriod, vehiclesRate, r);
                ArrayList<Trace> simTraces = s.run();
                systemConfig.evalTraces(simTraces); //adds to the traces an evaluation of the simulation.norms w.r.t. the systemConfig
                triedConfigsSimTraces.put(systemConfig.toString(), new ArrayList<>(simTraces));
                /** Evaluation of the traces via the MAS objectives evaluator **/
                System.out.println("Evaluating the MAS objectives ...");
                ArrayList<Trace> labeledTraces = maoe.labelTraces(simTraces);

                /** Data-Driven Norm Revision **/
                System.out.println("Creating and running Data-Driven Norm Revision ...");
                //create DNR
                DNR dnr = new DNR(dnr_samples, dnr_metric, idMaxSpeedNorm, idMinDistNorm, r);
                //Create results file
                /** For all different types of experiments**/
                for (boolean traintestsplit : traintestsplits) {
                    for (String rev_type : rev_types) {
                        System.out.println("... TrainTest Split "+traintestsplit);
                        System.out.println("... Revision Type "+rev_type);

                        System.out.println(" - Computing Results Synthesis Step - "+rev_type);

                        /**First I log all info about the synthesised sets (RQ1) **/
                        String exp_type = "synth";
                        LinkedHashMap<String, ArrayList<DNFNorm>> rn = dnr.synthesis(rev_type, systemConfig, labeledTraces);
                        LinkedHashMap<Configuration, Double> rneval = dnr.selection(systemConfig, rn, labeledTraces);
                        System.out.println("Number of new configurations synthesised " + rneval.size());


                        /** Then I analyze the selected configurations (RQ2) **/
                        /**
                         * Here the results for the "random" metric, used as a baseline against accuracy
                         */
                        System.out.println(" - Computing Results Selection Step - "+rev_type);
                        exp_type = "sel";
                        String metric = "random";
                        System.out.println("... metric "+metric);

                        int random_pick_id = -1;
                        if (rneval.size() > 0)
                            random_pick_id = r.nextInt(rneval.size());
                        Configuration c_randomly_picked = null;
                        int k = 0;
                        for (Configuration c : rneval.keySet()) {
                            LinkedHashMap<String, ArrayList<String>> c_eval = dnr.evalConfiguration(nr_norms, c, labeledTraces, traintestsplit, independent_set_test, independentLabeledTraces, logTwoLabelConfMatr);
                            if(log_synth) {
                                writeMetricsData(resultsfile, "synth" + ";-;-;" + traintestsplit + ";-;" + rev_type + ";" + trial + ";", c_eval, logTwoLabelConfMatr);
                            }

                            if (k == random_pick_id) {
                                c_randomly_picked = c;
                                if(!log_synth)
                                    break;
                            }
                            k++;
                        }
                        LinkedHashMap<String, ArrayList<String>> eval_random = dnr.evalConfigurations(nr_norms, systemConfig, c_randomly_picked, labeledTraces, traintestsplit, independent_set_test, independentLabeledTraces, logTwoLabelConfMatr);
                        System.out.println("Random selection: " + c_randomly_picked);
                        writeMetricsData(resultsfile, exp_type + ";" + metric + ";0;" + traintestsplit + ";" + independent_set_test + ";" + rev_type + ";" + trial + ";", eval_random, logTwoLabelConfMatr);

                        /**
                         * Then the results obtained actually with the whole DNR (I rerun for simplicity)
                         */
                        metric = dnr_metric;
                        System.out.println("... metric "+metric);
                        Configuration tempSysConfig = systemConfig;
                        for (int i = 0; i < nr_repeated_revision; i++) { //checking also what happens after 4 repeated revisions
                            if(i>0) {//I synthesise again
                                rn = dnr.synthesis(rev_type, tempSysConfig, labeledTraces);
                                rneval = dnr.selection(tempSysConfig, rn, labeledTraces);
                            }
                            Configuration newConfig = dnr.getBestConfigFromSelStep(rneval);
                            System.out.println("DNR selection: " + newConfig);
                            LinkedHashMap<String, ArrayList<String>> eval = dnr.evalConfigurations(nr_norms, tempSysConfig, newConfig, labeledTraces, traintestsplit, independent_set_test, independentLabeledTraces, logTwoLabelConfMatr);
                            writeMetricsData(resultsfile, exp_type + ";" + metric + ";" + i + ";" + traintestsplit + ";" + independent_set_test + ";" + rev_type + ";" + trial + ";", eval, logTwoLabelConfMatr);
                            tempSysConfig = newConfig;
                        }
                    }
                }
            }
        }

    }

    static void initCSV(File filename, int nr_norms, boolean logTwoLabelConfMatr) {
        FileWriter csvWriter = null;
        try {
            csvWriter = new FileWriter(filename, false);
            int info_size = 5; //5 because it is: quality val + the 4 val of the confusion matrix

            String header = "exp_type;metric;revision_nr;traintestsplit;independent_test;revtype;trial;";
            for (int j = 0; j < 4; j++) { //4 times: init norm on train, init on test, new on train, new on test
                String suffix = "";
                switch (j) {
                    case 0:
                        suffix = "init_train";
                        break;
                    case 1:
                        suffix = "init_test";
                        break;
                    case 2:
                        suffix = "new_train";
                        break;
                    case 3:
                        suffix = "new_test";
                        break;
                }
                header = header + suffix + "_val;";
                if (nr_norms > 1 && logTwoLabelConfMatr) {
                    header = header + "pfc;ppc1;ppc2;pfw;nfw;npc2;npc1;nfc;";
                } else {
                    for (int n = 0; n < nr_norms; n++) //for each norm
                        header = header + "tp_" + n + ";fp_" + n + ";tn_" + n + ";fn_" + n + ";";
                }
            }
            csvWriter.append(header + "\n");
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void writeMetricsData(File resultsfile, String param, LinkedHashMap<String, ArrayList<String>> logMetr, boolean logTwoLabelConfMatr) {
        /**
         * Function to write the results concerning the metrics
         */
        try {
            FileWriter csvWriter = new FileWriter(resultsfile, true);
            for (Map.Entry<String, ArrayList<String>> metric : logMetr.entrySet()) {
                csvWriter.append(param+"");
                for(int i=0;i<metric.getValue().size();i++) {
                    csvWriter.append(metric.getValue().get(i)+";");
                }
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
