package cs646.final_project;

import cs646.stats.DL2;
import cs646.stats.PL2;
import cs646.stats.ScoreUtils;
import cs646.stats.ScoringFunction;
import cs646.utils.EvalUtils;
import cs646.utils.LuceneQLSearcher;
import cs646.utils.LuceneUtils;
import cs646.utils.SearchResult;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.lucene.analysis.Analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class robust04 {
    public static void main( String[] args ) throws Exception{
        String trec_path = "C:\\Users\\tensa\\Desktop\\courses\\information retrieval\\final project\\Mallet-master\\Mallet-master\\index_robust04";
        String pathStopwords = "C:\\Users\\tensa\\Desktop\\courses\\information retrieval\\final project\\Mallet-master\\Mallet-master\\stopwords_inquery";
        String pathQueries = "C:\\Users\\tensa\\Desktop\\courses\\information retrieval\\final project\\Mallet-master\\Mallet-master\\queries_robust04"; // change it to your query file path
        String pathQrels = "C:\\Users\\tensa\\Desktop\\courses\\information retrieval\\final project\\Mallet-master\\Mallet-master\\qrels_robust04";

        LuceneQLSearcher searcher = new LuceneQLSearcher(trec_path);
        searcher.setStopwords(pathStopwords);
        Analyzer analyzer = LuceneUtils.getAnalyzer( LuceneUtils.Stemming.Krovetz );
        Map<String, String> queries = EvalUtils.loadQueries( pathQueries );
        Map<String, Set<String>> qrels = EvalUtils.loadQrels( pathQrels );
        Map<String, Map<String, Double>> qrels_score = EvalUtils.loadQrels_score( pathQrels );


        String fieldDocno = "docno";
        String fieldSearch = "content";
        ScoringFunction pl2 = new PL2(7.0);
        ScoringFunction dl2 = new DL2(7.0);
        int top = 1000;
        double mu_QL = 1000;

        double[] ql_ap = new double[queries.size()];
        double[] ql_nDCG = new double[queries.size()];
        double[] ql_ERR = new double[queries.size()];
        double[] pl2_ap = new double[queries.size()];
        double[] pl2_nDCG = new double[queries.size()];
        double[] pl2_ERR = new double[queries.size()];
        double[] dl2_ap = new double[queries.size()];
        double[] dl2_nDCG = new double[queries.size()];
        double[] dl2_ERR = new double[queries.size()];

        int ix = 0;
        for ( String qid : queries.keySet() ){
            System.out.println("qid: " + qid);
            String query = queries.get( qid );
            List<String> terms = LuceneUtils.tokenize( query, analyzer );

            // get ideal rank@10
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

            //get QL scores
            List<SearchResult> ql_results = searcher.search( fieldSearch, terms, mu_QL, top );
            SearchResult.dumpDocno( searcher.getIndex(), fieldDocno, ql_results );
            ql_ap[ix] = ScoreUtils.avgPrec( ql_results, qrels.get( qid ), top );
            ql_nDCG[ix] = ScoreUtils.getnDCG(qid,ql_results,qrels_score,ideal_list);
            ql_ERR[ix] = ScoreUtils.getERR(qid,ql_results,qrels_score,ideal_list);
            System.out.printf("%-10s%-15.4f%-15.4f%-15.4f\n","QL",ql_ap[ix],ql_nDCG[ix],ql_ERR[ix]);
            //get DFR PL2 scores
            List<SearchResult> pl2_results = searcher.DocSearch(fieldSearch,terms,pl2,top);
            SearchResult.dumpDocno( searcher.getIndex(), fieldDocno, pl2_results );
            pl2_ap[ix] = ScoreUtils.avgPrec( pl2_results, qrels.get( qid ), top );
            pl2_nDCG[ix] = ScoreUtils.getnDCG(qid,pl2_results,qrels_score,ideal_list);
            pl2_ERR[ix] = ScoreUtils.getERR(qid,pl2_results,qrels_score,ideal_list);
            System.out.printf("%-10s%-15.4f%-15.4f%-15.4f\n","PL2",pl2_ap[ix],pl2_nDCG[ix],pl2_ERR[ix]);
            //get DFR PL2 scores
            List<SearchResult> dl2_results = searcher.DocSearch(fieldSearch,terms,dl2,top);
            SearchResult.dumpDocno( searcher.getIndex(), fieldDocno, dl2_results );
            dl2_ap[ix] = ScoreUtils.avgPrec( dl2_results, qrels.get( qid ), top );
            dl2_nDCG[ix] = ScoreUtils.getnDCG(qid,dl2_results,qrels_score,ideal_list);
            dl2_ERR[ix] = ScoreUtils.getERR(qid,dl2_results,qrels_score,ideal_list);
            System.out.printf("%-10s%-15.4f%-15.4f%-15.4f\n","DL2",dl2_ap[ix],dl2_nDCG[ix],dl2_ERR[ix]);

            ix++;
        }

        System.out.println();
        System.out.println("For robust04 data collection:");
        String[] model_name = new String[]{"QL","DFR-PL2","DFR-DL2"};
        double[] ap = new double[] {StatUtils.mean(ql_ap),StatUtils.mean(pl2_ap),StatUtils.mean(dl2_ap)};
        double[] dcg = new double[] {StatUtils.mean(ql_nDCG),StatUtils.mean(pl2_nDCG),StatUtils.mean(dl2_nDCG)};
        double[] err = new double[] {StatUtils.mean(ql_ERR),StatUtils.mean(pl2_ERR),StatUtils.mean(dl2_ERR)};
        System.out.printf("%-10s%-15s%-15s%-15s\n","model","mean AP","mean nDCG@10","mean ERR@10");
        for (int k =0; k< model_name.length;k++){
            System.out.printf("%-10s%-15.4f%-15.4f%-15.4f\n",model_name[k],ap[k],dcg[k],err[k]);
        }
        System.out.println();

    }
}
