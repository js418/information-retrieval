package cs646.stats;

import cs646.utils.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ScoreUtils {

    public static double avgPrec(List<SearchResult> results, Set<String> relDocnos, int n ) {
        double numrel = 0;
        double sumprec = 0;
        int count = 0;
        if(results == null){
            return 0.0;
        }
        for ( SearchResult result : results ) {
            if( relDocnos == null){
                return 0.0;
            }
            if (relDocnos.contains( result.getDocno() ) ) {
                numrel++;
                sumprec += ( numrel / ( count + 1 ) );
            }
            count++;
            if ( count >= n ) {
                break;
            }
        }
        return sumprec / relDocnos.size();
    }


    public static double getnDCG(String qid, List<SearchResult> results, Map<String, Map<String, Double>> qrels_score, List<Double> ideal_list) throws IOException{
        double DCG = 0.0;
        double iDCG = 0.0;

        if(results == null){
            return 0.0;
        }

        if (qrels_score.get(qid)==null){
            return 0.0;
        }

        // store rank 10 docs for each query in the system
        List<Double> rank_10 = new ArrayList<>();
        if (results.size()<10){
            for (int m =0; m< results.size(); m++){
                String docno = results.get(m).getDocno();
                if (qrels_score.get(qid).keySet().contains(docno)){
                    rank_10.add(qrels_score.get(qid).get(docno));
                }
                else{
                    rank_10.add(0.0);
                }
            }
            int k = 10 - results.size();
            for (int r = 0; r<k;r++){
                rank_10.add(0.0);
            }
        }
        else{
            for (int r =0; r< 10; r++){
                String docno = results.get(r).getDocno();
                if (qrels_score.get(qid)!= null && qrels_score.get(qid).keySet().contains(docno)){
                    rank_10.add(qrels_score.get(qid).get(docno));
                }
                else{
                    rank_10.add(0.0);
                }
            }
        }

        for (int i =0; i<10; i++){
            DCG += (Math.pow(2.0,rank_10.get(i))-1.0)/log2(i+2.0);
            iDCG += (Math.pow(2.0,ideal_list.get(i))-1.0)/log2(i+2.0);
        }

        double nDCG = DCG/iDCG;
        return  nDCG;
    }

    public static double getERR(String qid, List<SearchResult> results, Map<String, Map<String, Double>> qrels_score, List<Double> ideal_list) throws IOException{
        double ERR = 0.0;
        double g = ideal_list.get(0);

        if(results == null){
            return 0.0;
        }

        if (qrels_score.get(qid)==null){
            return 0.0;
        }

        // store rank 10 docs for each query in the system
        List<Double> rank_10 = new ArrayList<>();
        if (results.size()<10){
            for (int m =0; m< results.size(); m++){
                String docno = results.get(m).getDocno();
                if (qrels_score.get(qid).keySet().contains(docno)){
                    rank_10.add(qrels_score.get(qid).get(docno));
                }
                else{
                    rank_10.add(0.0);
                }
            }
            int k = 10 - results.size();
            for (int r = 0; r<k;r++){
                rank_10.add(0.0);
            }
        }
        else{
            for (int r =0; r< 10; r++){
                String docno = results.get(r).getDocno();
                if (qrels_score.get(qid).keySet().contains(docno)){
                    rank_10.add(qrels_score.get(qid).get(docno));
                }
                else{
                    rank_10.add(0.0);
                }
            }
        }

        for (int i =0; i<10; i++){
            double product_r = 1.0;
            for(int j =0; j<i;j++){
                product_r = product_r*(1.0- (Math.pow(2.0,rank_10.get(j))-1.0)/(Math.pow(2.0,g)));
            }
            ERR += (1.0/(i+1.0))* (Math.pow(2.0,rank_10.get(i))-1.0)/(Math.pow(2.0,g)) * product_r;

        }
        return ERR;
    }

    public static double log2(double d){
        return Math.log(d)/Math.log(2);
    }

    public static List<Double> getIdealRank(Map<String, Map<String, Double>> qrels_score,String qid){
        List<Double> ideal_list = new ArrayList<>();
        if (qrels_score.get(qid)== null){
            for (int i =0;i<10;i++){
                ideal_list.add(0.0);
            }
        }
        else{
            ideal_list = new ArrayList<>(qrels_score.get(qid).values());
            if(ideal_list.size()<10){
                int k = 10-ideal_list.size();
                for (int i = 0; i<k;i++){
                    ideal_list.add(0.0);
                }
            }
        }
        return  ideal_list;
    }



}
