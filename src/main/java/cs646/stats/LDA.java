package cs646.stats;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LDA implements ScoringFunction {
    protected double mu;
    protected double lamba;
    protected Map<Integer,List<Double>> lda_score;

    public LDA(Map<Integer,List<Double>> lda_score, double mu, double lamba){
        this.lda_score = lda_score;
        this.mu = mu;
        this.lamba =lamba;
    }


    public double score(List<Double> weights, List<Double> tfs, List<Double> tfcs, double dl,int docid,IndexReader index) throws IOException{
        double cl = index.getSumTotalTermFreq("content");
        double sum_score = 0.0;
        double qld_score;

        for (int i = 0; i<weights.size();i++){
            qld_score = (tfs.get(i)+ mu*tfcs.get(i)/cl)/(dl+mu);
            double p_w_D = lamba*qld_score+ (1.0-lamba)*lda_score.get(docid).get(i);
            sum_score += weights.get(i)*Math.log(p_w_D);
        }
        return sum_score;
    }



}
