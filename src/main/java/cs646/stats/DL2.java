package cs646.stats;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.List;

import static cs646.stats.ScoreUtils.log2;

public class DL2 implements ScoringFunction {
    protected double c;

    public DL2(double c){
        this.c = c;
    }

    public double score(List<Double> weights, List<Double> tfs, List<Double> tfcs, double dl, int docid, IndexReader index) throws IOException{
        double N = (double)index.numDocs();
        double p = 1.0/N;
        double avg_dl = (double)index.getSumTotalTermFreq("content" )/N;
        double sum =0.0;
        for (int i = 0; i<weights.size();i++){
            if(tfs.get(i)!=0.0){
                double tfn = tfs.get(i)*log2(1.0+ c*avg_dl/dl);
                //System.out.print("tfn: " +tfn +" ");
                double theta = tfn/tfcs.get(i);
                //System.out.print("theta: " + theta +" ");
                double Inf = tfcs.get(i)*(theta*log2(theta/p)+(1.0-theta)*log2((1.0-theta)/(1.0-p)))+0.5*log2(2.0*Math.PI*tfn*(1.0-theta));
                //System.out.print("inf: " +Inf +" ");
                double dl2 = Inf/(tfn+1.0);
                //System.out.print("pl: " +pl +" \n");
                sum += dl2;
            }
        }
        //System.out.println("sum score: "+ sum);
        return  sum;
    }

}