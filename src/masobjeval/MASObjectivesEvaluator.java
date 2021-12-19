package masobjeval;

import simulation.Conjunction;
import simulation.DNFNorm;
import simulation.MaxSpeedNorm;
import simulation.Trace;

import java.util.*;

public class MASObjectivesEvaluator {
    private double ttt_indiv;
    private double tco2_indiv;
    private final boolean isNormObj;
    private DNFNorm normObj;
    private final Random r;
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

        Conjunction hm_cond = new Conjunction();
        Conjunction hm_proh = new Conjunction();
        Conjunction hm_dead = new Conjunction();
        //applicability
        hm_cond.addLiteral(MaxSpeedNorm.COND_APPL, "truck");
        hm_proh.addLiteral(MaxSpeedNorm.PROH_APPL, "truck");
        //condition
        hm_cond.addLiteral(MaxSpeedNorm.COND_POS, "km3");
//        Conjunction dc = new Conjunction(hm_cond);
        List<Conjunction> ran_c = new ArrayList<>();
        ran_c.add(hm_cond);
        //prohibition (same applicability as condition)
        hm_proh.addLiteral(MaxSpeedNorm.PROH_SPEED, "19");
        List<Conjunction> ran_p = new ArrayList<>();
        ran_p.add(hm_proh);
        //deadline
        hm_dead.addLiteral(MaxSpeedNorm.DEAD_POS, "km9");
        List<Conjunction> ran_d = new ArrayList<>();
        ran_d.add(hm_dead);
        return new MaxSpeedNorm("FMSN", ran_c, ran_p, ran_d, r);
    }

    public boolean areObjAchieved(double curr_oa) {
        return curr_oa >= t_oa;
    }

}
