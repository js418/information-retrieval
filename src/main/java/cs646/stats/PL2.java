package cs646.stats;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.List;

import static cs646.stats.ScoreUtils.log2;

public class PL2 implements ScoringFunction{
    protected double c;

    public PL2(double c){
        this.c = c;
    }

    public double score(List<Double> weights, List<Double> tfs, List<Double> tfcs, double dl, int docid, IndexReader index) throws IOException{
        double N = (double)index.numDocs();
        double p = 1.0/N;
        double avg_dl = (double)index.getSumTotalTermFreq("content" )/N;

        double e = Math.E;
        double sum =0.0;
        for (int i = 0; i<weights.size();i++){
            if ( tfs.get(i) != 0.0){
                double lambda = p*tfcs.get(i);
                //System.out.print("lambda: " +lambda +" ");
                double tfn = tfs.get(i)*log2(1.0+ c*avg_dl/dl);
                //System.out.print("tfn: " +tfn +" ");
                double Inf = tfn*log2(tfn/lambda)+ (lambda+1.0/(12.0*tfn)-tfn)*log2(e)+0.5*log2(2.0*Math.PI*tfn);
                //System.out.print("inf: " +Inf +" ");
                double pl = Inf/(tfn+1.0);
                //System.out.print("pl: " +pl +" ");
                sum += pl;
            }
        }
        //System.out.println("sum score: "+ sum);
        return sum;
    }

}
