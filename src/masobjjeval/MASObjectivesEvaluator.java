package masobjjeval;

import simulation.DNFNorm;
import simulation.Disjunct;
import simulation.MaxSpeedNorm;
import simulation.Trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.stream.Collectors;

public class MASObjectivesEvaluator {
    private double ttt_indiv;
    private double tco2_indiv;
    private boolean isNormObj;
    private DNFNorm normObj;
    private Random r;
    double t_oa;

    public MASObjectivesEvaluator(boolean isNormObj, double tco2_indiv, double ttt_indiv, double  t_oa, Random r) {
        this.r = r;
        this.isNormObj = isNormObj;
        if (isNormObj) {
            this.normObj = getPredefNorm();
        }
        else {
            this.tco2_indiv = tco2_indiv;
            this.ttt_indiv = ttt_indiv;
        }

        this.t_oa = t_oa;
    }
    public boolean isObjAchieved(Trace trace) {
        if(isNormObj) {
            return normObj.isViol(trace)==-1;
        }
        else {
            return trace.getCo2_eval()<=tco2_indiv && trace.getTraveltime_eval()<=ttt_indiv;
        }
    }
    public ArrayList<Trace> labelTraces(ArrayList<Trace> traces) {
        /**
         * Function that labels a dataset of traces w.r.t. the MAS objectives.
         * The function returns a copy of the dataset, where for each new (now labeled) trace,
         * the evaluation is stored directly in the traces
         */
        ArrayList<Trace> labeledTraces = new ArrayList<>();
        for (Trace t: traces) {
            Trace newTrace = (Trace) t.clone();
            evalObjectives(newTrace);
            labeledTraces.add(newTrace);
        }
        return labeledTraces;
    }

    public void evalObjectives(Trace t) {
        /**
         * Function that labels a trace wr.t. the MAS objectives
         * The evaluation will be stored directly in the traces
         */
        double eval_co2 = 0.0;
        double eval_time = 0.0;
        for(int i=0; i<t.getStates().size()-1; i++) { //up to -1 below because the last km of the highway I cannot control it
            eval_co2 = Math.max(eval_co2, t.getStates().get(i).getCo2emission());
            eval_time = Math.max(eval_time, t.getStates().get(i).getTime());
        }
        //note I store also these info but only for logging purposes
        t.setCo2_eval(eval_co2);
        t.setTraveltime_eval(eval_time);
        //this is the actual boolean evaluation
        t.setObjEval(t.getCo2_eval()<=tco2_indiv && t.getTraveltime_eval()<=ttt_indiv);
    }

    public double getCurrOA(ArrayList<Trace> labeledTraces) {
        /*THE CURRENT OA IS CALCULATED ONLY ON THE TRACES GENERATED WITH THE CURRENT CONFIGURATION AND NOT ON THE PAST DATA*/
        double curr_oa = 0.0;
        if(labeledTraces.size()>0)
            curr_oa = labeledTraces.stream().filter(t -> (isObjAchieved(t))).count()/(labeledTraces.size()*1.0);
        return curr_oa;
    }


    public MaxSpeedNorm getPredefNorm() {
        /**
         *  Function that returns a predefined MaxSpeed norm (truck and km3, P(truck and sp19), km9)
         **/

        LinkedHashMap<String, String> hm_cond = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> hm_proh = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> hm_dead = new LinkedHashMap<String, String>();
        //applicability
        hm_cond.put(MaxSpeedNorm.COND_APPL+"1", "truck");
        hm_proh.put(MaxSpeedNorm.PROH_APPL+"1", "truck");
        //condition
        hm_cond.put(MaxSpeedNorm.COND_POS, "km3");
        Disjunct ran_c = new Disjunct(hm_cond);
        //prohibition (same applicability as condition)
        hm_proh.put(MaxSpeedNorm.PROH_SPEED, "19");
        Disjunct ran_p = new Disjunct(hm_proh);
        //deadline
        hm_dead.put(MaxSpeedNorm.DEAD_POS, "km9");
        Disjunct ran_d = new Disjunct(hm_dead);
        return new MaxSpeedNorm("FMSN", ran_c, ran_p, ran_d, r);
    }

    public boolean areObjAchieved(double curr_oa) {
        return curr_oa >= t_oa;
    }

}
