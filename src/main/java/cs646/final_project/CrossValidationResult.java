package cs646.final_project;

import cs646.stats.CrossValidation;
import cs646.stats.DL2;
import cs646.stats.PL2;
import cs646.stats.ScoringFunction;
import cs646.utils.EvalUtils;
import cs646.utils.LuceneQLSearcher;
import cs646.utils.LuceneUtils;
import org.apache.lucene.analysis.Analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CrossValidationResult {
    public static void main( String[] args ) throws Exception {
        String[] index_path = new String[]{"index_trec123", "index_robust04"};
        String pathStopwords = "stopwords_inquery";
        String[] pathQueries = new String[]{"queries_trec1-3", "queries_robust04"};
        String[] pathQrels = new String[]{"qrels_trec1-3", "qrels_robust04"};
        Analyzer analyzer = LuceneUtils.getAnalyzer(LuceneUtils.Stemming.Krovetz);

        int top = 1000;
        double[] mu_QL = new double[]{500, 1000, 1500, 2000, 2500, 3000, 3500, 4000};
        double[] c = new double[]{1.0,2.0,3.0,4.0,5.0,6.0,7.0,8.0,9.0};
        String fieldDocno = "docno";
        String fieldSearch = "content";

        for (int data = 0; data < 2; data++) {
            System.out.println("data collection: " + index_path[data]);
            LuceneQLSearcher searcher = new LuceneQLSearcher(index_path[data]);
            searcher.setStopwords(pathStopwords);
            Map<String, String> queries = EvalUtils.loadQueries(pathQueries[data]);
            Map<String, Set<String>> qrels = EvalUtils.loadQrels(pathQrels[data]);
            Map<String, Map<String, Double>> qrels_score = EvalUtils.loadQrels_score(pathQrels[data]);
            List<String> list = new ArrayList<>();
            for (String q: queries.keySet()){
                list.add(q);
            }
            CrossValidation cv_dfr = new CrossValidation();
            for (int k =1; k<6; k++){
                System.out.println("round "+ k);
                cv_dfr.set(queries,list,k);
                cv_dfr.DL2_perform(searcher,queries,analyzer,qrels,qrels_score,c,top);
                cv_dfr.PL2_perform(searcher,queries,analyzer,qrels,qrels_score,c,top);
            }


        }

    }
}
