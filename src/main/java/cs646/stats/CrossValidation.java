package cs646.stats;

import cs646.utils.LuceneQLSearcher;
import cs646.utils.LuceneUtils;
import cs646.utils.SearchResult;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.lucene.analysis.Analyzer;

import java.io.IOException;
import java.util.*;

public class CrossValidation {
    protected List<String> training_set;
    protected List<String> testing_set;

    public void set(Map<String,String> queries, List<String> list,int key){
        int l = queries.size();
        int k = l/5;
        testing_set = new ArrayList<>();
        switch (key){
            case 1:{
                for (int i = 0; i<k; i++){
                    testing_set.add(list.get(i));
                }
                training_set = new ArrayList<>(list);
                training_set.removeAll(testing_set);
                break;
            }
            case 2:{
                for (int i = k; i< 2*k; i++){
                    testing_set.add(list.get(i));
                }
                training_set = new ArrayList<>(list);
                training_set.removeAll(testing_set);
                break;
            }
            case 3:{
                for (int i = 2*k; i< 3*k; i++){
                    testing_set.add(list.get(i));
                }
                training_set = new ArrayList<>(list);
                training_set.removeAll(testing_set);
                break;
            }
            case 4:{
                for (int i = 3*k; i< 4*k; i++){
                    testing_set.add(list.get(i));
                }
                training_set = new ArrayList<>(list);
                training_set.removeAll(testing_set);
                break;
            }
            case 5:{
                for (int i = 4*k; i< l; i++){
                    testing_set.add(list.get(i));
                }
                training_set = new ArrayList<>(list);
                training_set.removeAll(testing_set);
                break;
            }
            default:
                break;

        }
    }

    public List<String> getTraining_set() {
        return training_set;
    }

    public List<String> getTesting_set() {
        return testing_set;
    }

    public void QL_perform(LuceneQLSearcher searcher, Map<String, String> queries, Analyzer analyzer, Map<String, Set<String>> qrels, Map<String, Map<String, Double>> qrels_score, double[] mu, int top)throws IOException{
        // training set
        System.out.println("Start to training ...");
        double[][] ql_ap = new double[mu.length][training_set.size()];
        double[][] ql_nDCG = new double[mu.length][training_set.size()];
        double[][] ql_ERR = new double[mu.length][training_set.size()];
        int ix =0;
        for(String qid : training_set){
            //System.out.println("training qid: " + qid);
            String query = queries.get( qid );
            List<String> terms = LuceneUtils.tokenize( query, analyzer );
            List<Double> ideal_list = ScoreUtils.getIdealRank(qrels_score,qid);
            for (int i = 0; i<mu.length; i++){
                List<SearchResult> ql_results = searcher.search( "content", terms, mu[i], top );
                SearchResult.dumpDocno( searcher.getIndex(), "docno", ql_results );
                ql_ap[i][ix] = ScoreUtils.avgPrec( ql_results, qrels.get( qid ), top );
                ql_nDCG[i][ix] = ScoreUtils.getnDCG(qid,ql_results,qrels_score,ideal_list);
                ql_ERR[i][ix] = ScoreUtils.getERR(qid,ql_results,qrels_score,ideal_list);
            }
            ix++;
        }
        double[] mean_ap = new double[mu.length];
        double[] mean_dcg = new double[mu.length];
        double[] mean_err = new double[mu.length];
        for (int i = 0; i< mu.length; i++){
            mean_ap[i] = StatUtils.mean(ql_ap[i]);
            mean_dcg[i] = StatUtils.mean(ql_nDCG[i]);
            mean_err[i] = StatUtils.mean(ql_ERR[i]);
        }
        List<Integer> ap_train_index = getMaxIndex(mean_ap);
        List<Integer> dcg_train_index = getMaxIndex(mean_dcg);
        List<Integer> err_train_index = getMaxIndex(mean_err);

        // testing set to validate
        System.out.println("Start to testing ...");
        double[][] ql_ap_test = new double[testing_set.size()][mu.length];
        double[][] ql_nDCG_test = new double[testing_set.size()][mu.length];
        double[][] ql_ERR_test = new double[testing_set.size()][mu.length];
        double[][] ap = new double[mu.length][testing_set.size()];
        double[][] nDCG = new double[mu.length][testing_set.size()];
        double[][] ERR = new double[mu.length][testing_set.size()];
        ix =0;
        for(String qid : testing_set){
            //System.out.println("testing qid: " + qid);
            String query = queries.get( qid );
            List<String> terms = LuceneUtils.tokenize( query, analyzer );
            List<Double> ideal_list = ScoreUtils.getIdealRank(qrels_score,qid);
            for (int i = 0; i<mu.length; i++){
                List<SearchResult> ql_results = searcher.search( "content", terms, mu[i], top );
                SearchResult.dumpDocno( searcher.getIndex(), "docno", ql_results );
                ql_ap_test[ix][i] = ScoreUtils.avgPrec( ql_results, qrels.get( qid ), top );
                ap[i][ix] = ql_ap_test[ix][i];
                ql_nDCG_test[ix][i] = ScoreUtils.getnDCG(qid,ql_results,qrels_score,ideal_list);
                nDCG[i][ix] = ql_nDCG_test[ix][i];
                ql_ERR_test[ix][i] = ScoreUtils.getERR(qid,ql_results,qrels_score,ideal_list);
                ERR[i][ix] = ql_ERR_test[ix][i];
            }
            ix++;
        }
        for (int i = 0; i< mu.length; i++){
            mean_ap[i] = StatUtils.mean(ap[i]);
            mean_dcg[i] = StatUtils.mean(nDCG[i]);
            mean_err[i] = StatUtils.mean(ERR[i]);
        }
        List<Integer> ap_test_index = getMaxIndex(mean_ap);
        List<Integer> dcg_test_index = getMaxIndex(mean_dcg);
        List<Integer> err_test_index = getMaxIndex(mean_err);


        System.out.println("Cross validation result:");
        List<Integer> ap_result = isResultTure(ap_train_index,ap_test_index,ql_ap_test,testing_set.size());
        if (ap_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean AP");
        }
        else{
            System.out.print("Best mu for mean AP is: ");
            for (int i : ap_result){
                System.out.print(mu[i] + "\t");
            }
            System.out.println();
        }
        List<Integer> dcg_result = isResultTure(dcg_train_index,dcg_test_index,ql_nDCG_test,testing_set.size());
        if (dcg_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean nDCG@10");
        }
        else{
            System.out.print("Best mu for mean nDCG@10 is: ");
            for (int i : dcg_result){
                System.out.print(mu[i] + "\t");
            }
            System.out.println();
        }
        List<Integer> err_result = isResultTure(err_train_index,err_test_index,ql_ERR_test,testing_set.size());
        if (err_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean nERR@10");
        }
        else{
            System.out.print("Best mu for mean nERR@10 is: ");
            for (int i : err_result){
                System.out.print(mu[i] + "\t");
            }
            System.out.println();
        }
        System.out.println("*******************************************************");

    }

    public static List<Integer> getMaxIndex(double[] list){
        List<Double> new_list = new ArrayList<>();
        List<Integer> index_list = new ArrayList<>();
        for (int i =0;i<list.length;i++){
            new_list.add(list[i]);
        }
        double max = Collections.max(new_list);
        while(new_list.contains(max))
        {
            int i = new_list.indexOf(max);
            index_list.add(i);
            new_list.set(i, -1.0);
        }
        return index_list;
    }

    public static List<Integer> isResultTure(List<Integer> train_index,List<Integer> test_index,double[][] test_result,int q_size){
        List<Integer> result = new ArrayList<>();
        double[][] train = new double[train_index.size()][q_size];
        double[][] test = new double[test_index.size()][q_size];
        for(int i = 0; i< train_index.size();i++){
            for(int j =0; j<q_size; j++ ){
                train[i][j] = test_result[j][train_index.get(i)];
            }
        }
        for(int i = 0; i< test_index.size();i++){
            for(int j =0; j<q_size; j++ ){
                test[i][j] = test_result[j][test_index.get(i)];
            }
        }
        for(int i = 0; i< train_index.size();i++) {
            for (int j = 0; j < test_index.size(); j++) {
                TTest t_test = new TTest();
                double p_value = t_test.pairedTTest(train[i],test[j]);
                if(p_value>0.05){
                    result.add(train_index.get(i));
                }
            }
        }
        return result;
    }

    public void DL2_perform(LuceneQLSearcher searcher, Map<String, String> queries, Analyzer analyzer, Map<String, Set<String>> qrels, Map<String, Map<String, Double>> qrels_score, double[] c, int top)throws IOException{
        // training set
        System.out.println("Start to training ...");
        double[][] ql_ap = new double[c.length][training_set.size()];
        double[][] ql_nDCG = new double[c.length][training_set.size()];
        double[][] ql_ERR = new double[c.length][training_set.size()];
        int ix =0;
        for(String qid : training_set){
            //System.out.println("training qid: " + qid);
            String query = queries.get( qid );
            List<String> terms = LuceneUtils.tokenize( query, analyzer );
            List<Double> ideal_list = ScoreUtils.getIdealRank(qrels_score,qid);
            for (int i = 0; i<c.length; i++){
                ScoringFunction s = new DL2(c[i]);
                List<SearchResult> ql_results = searcher.DocSearch( "content", terms, s, top );
                SearchResult.dumpDocno( searcher.getIndex(), "docno", ql_results );
                ql_ap[i][ix] = ScoreUtils.avgPrec( ql_results, qrels.get( qid ), top );
                ql_nDCG[i][ix] = ScoreUtils.getnDCG(qid,ql_results,qrels_score,ideal_list);
                ql_ERR[i][ix] = ScoreUtils.getERR(qid,ql_results,qrels_score,ideal_list);
            }
            ix++;
        }
        double[] mean_ap = new double[c.length];
        double[] mean_dcg = new double[c.length];
        double[] mean_err = new double[c.length];
        for (int i = 0; i< c.length; i++){
            mean_ap[i] = StatUtils.mean(ql_ap[i]);
            mean_dcg[i] = StatUtils.mean(ql_nDCG[i]);
            mean_err[i] = StatUtils.mean(ql_ERR[i]);
        }
        List<Integer> ap_train_index = getMaxIndex(mean_ap);
        List<Integer> dcg_train_index = getMaxIndex(mean_dcg);
        List<Integer> err_train_index = getMaxIndex(mean_err);

        // testing set to validate
        System.out.println("Start to testing ...");
        double[][] ql_ap_test = new double[testing_set.size()][c.length];
        double[][] ql_nDCG_test = new double[testing_set.size()][c.length];
        double[][] ql_ERR_test = new double[testing_set.size()][c.length];
        double[][] ap = new double[c.length][testing_set.size()];
        double[][] nDCG = new double[c.length][testing_set.size()];
        double[][] ERR = new double[c.length][testing_set.size()];
        ix =0;
        for(String qid : testing_set){
            //System.out.println("testing qid: " + qid);
            String query = queries.get( qid );
            List<String> terms = LuceneUtils.tokenize( query, analyzer );
            List<Double> ideal_list = ScoreUtils.getIdealRank(qrels_score,qid);
            for (int i = 0; i<c.length; i++){
                ScoringFunction s = new DL2(c[i]);
                List<SearchResult> ql_results = searcher.DocSearch( "content", terms, s, top );
                SearchResult.dumpDocno( searcher.getIndex(), "docno", ql_results );
                ql_ap_test[ix][i] = ScoreUtils.avgPrec( ql_results, qrels.get( qid ), top );
                ap[i][ix] = ql_ap_test[ix][i];
                ql_nDCG_test[ix][i] = ScoreUtils.getnDCG(qid,ql_results,qrels_score,ideal_list);
                nDCG[i][ix] = ql_nDCG_test[ix][i];
                ql_ERR_test[ix][i] = ScoreUtils.getERR(qid,ql_results,qrels_score,ideal_list);
                ERR[i][ix] = ql_ERR_test[ix][i];
            }
            ix++;
        }
        for (int i = 0; i< c.length; i++){
            mean_ap[i] = StatUtils.mean(ap[i]);
            mean_dcg[i] = StatUtils.mean(nDCG[i]);
            mean_err[i] = StatUtils.mean(ERR[i]);
        }
        List<Integer> ap_test_index = getMaxIndex(mean_ap);
        List<Integer> dcg_test_index = getMaxIndex(mean_dcg);
        List<Integer> err_test_index = getMaxIndex(mean_err);


        System.out.println("Cross validation result:");
        List<Integer> ap_result = isResultTure(ap_train_index,ap_test_index,ql_ap_test,testing_set.size());
        if (ap_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean AP");
        }
        else{
            System.out.print("Best c of DL2 for mean AP is: ");
            for (int i : ap_result){
                System.out.print(c[i] + "\t");
            }
            System.out.println();
        }
        List<Integer> dcg_result = isResultTure(dcg_train_index,dcg_test_index,ql_nDCG_test,testing_set.size());
        if (dcg_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean nDCG@10");
        }
        else{
            System.out.print("Best c of DL2 for mean nDCG@10 is: ");
            for (int i : dcg_result){
                System.out.print(c[i] + "\t");
            }
            System.out.println();
        }
        List<Integer> err_result = isResultTure(err_train_index,err_test_index,ql_ERR_test,testing_set.size());
        if (err_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean nERR@10");
        }
        else{
            System.out.print("Best c of DL2 for mean nERR@10 is: ");
            for (int i : err_result){
                System.out.print(c[i] + "\t");
            }
            System.out.println();
        }
        System.out.println("*******************************************************");

    }

    public void PL2_perform(LuceneQLSearcher searcher, Map<String, String> queries, Analyzer analyzer, Map<String, Set<String>> qrels, Map<String, Map<String, Double>> qrels_score, double[] c, int top)throws IOException{
        // training set
        System.out.println("Start to training ...");
        double[][] ql_ap = new double[c.length][training_set.size()];
        double[][] ql_nDCG = new double[c.length][training_set.size()];
        double[][] ql_ERR = new double[c.length][training_set.size()];
        int ix =0;
        for(String qid : training_set){
            //System.out.println("training qid: " + qid);
            String query = queries.get( qid );
            List<String> terms = LuceneUtils.tokenize( query, analyzer );
            List<Double> ideal_list = ScoreUtils.getIdealRank(qrels_score,qid);
            for (int i = 0; i<c.length; i++){
                ScoringFunction s = new PL2(c[i]);
                List<SearchResult> ql_results = searcher.DocSearch( "content", terms, s, top );
                SearchResult.dumpDocno( searcher.getIndex(), "docno", ql_results );
                ql_ap[i][ix] = ScoreUtils.avgPrec( ql_results, qrels.get( qid ), top );
                ql_nDCG[i][ix] = ScoreUtils.getnDCG(qid,ql_results,qrels_score,ideal_list);
                ql_ERR[i][ix] = ScoreUtils.getERR(qid,ql_results,qrels_score,ideal_list);
            }
            ix++;
        }
        double[] mean_ap = new double[c.length];
        double[] mean_dcg = new double[c.length];
        double[] mean_err = new double[c.length];
        for (int i = 0; i< c.length; i++){
            mean_ap[i] = StatUtils.mean(ql_ap[i]);
            mean_dcg[i] = StatUtils.mean(ql_nDCG[i]);
            mean_err[i] = StatUtils.mean(ql_ERR[i]);
        }
        List<Integer> ap_train_index = getMaxIndex(mean_ap);
        List<Integer> dcg_train_index = getMaxIndex(mean_dcg);
        List<Integer> err_train_index = getMaxIndex(mean_err);

        // testing set to validate
        System.out.println("Start to testing ...");
        double[][] ql_ap_test = new double[testing_set.size()][c.length];
        double[][] ql_nDCG_test = new double[testing_set.size()][c.length];
        double[][] ql_ERR_test = new double[testing_set.size()][c.length];
        double[][] ap = new double[c.length][testing_set.size()];
        double[][] nDCG = new double[c.length][testing_set.size()];
        double[][] ERR = new double[c.length][testing_set.size()];
        ix =0;
        for(String qid : testing_set){
            //System.out.println("testing qid: " + qid);
            String query = queries.get( qid );
            List<String> terms = LuceneUtils.tokenize( query, analyzer );
            List<Double> ideal_list = ScoreUtils.getIdealRank(qrels_score,qid);
            for (int i = 0; i<c.length; i++){
                ScoringFunction s = new PL2(c[i]);
                List<SearchResult> ql_results = searcher.DocSearch( "content", terms, s, top );
                SearchResult.dumpDocno( searcher.getIndex(), "docno", ql_results );
                ql_ap_test[ix][i] = ScoreUtils.avgPrec( ql_results, qrels.get( qid ), top );
                ap[i][ix] = ql_ap_test[ix][i];
                ql_nDCG_test[ix][i] = ScoreUtils.getnDCG(qid,ql_results,qrels_score,ideal_list);
                nDCG[i][ix] = ql_nDCG_test[ix][i];
                ql_ERR_test[ix][i] = ScoreUtils.getERR(qid,ql_results,qrels_score,ideal_list);
                ERR[i][ix] = ql_ERR_test[ix][i];
            }
            ix++;
        }
        for (int i = 0; i< c.length; i++){
            mean_ap[i] = StatUtils.mean(ap[i]);
            mean_dcg[i] = StatUtils.mean(nDCG[i]);
            mean_err[i] = StatUtils.mean(ERR[i]);
        }
        List<Integer> ap_test_index = getMaxIndex(mean_ap);
        List<Integer> dcg_test_index = getMaxIndex(mean_dcg);
        List<Integer> err_test_index = getMaxIndex(mean_err);


        System.out.println("Cross validation result:");
        List<Integer> ap_result = isResultTure(ap_train_index,ap_test_index,ql_ap_test,testing_set.size());
        if (ap_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean AP");
        }
        else{
            System.out.print("Best c of PL2 for mean AP is: ");
            for (int i : ap_result){
                System.out.print(c[i] + "\t");
            }
            System.out.println();
        }
        List<Integer> dcg_result = isResultTure(dcg_train_index,dcg_test_index,ql_nDCG_test,testing_set.size());
        if (dcg_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean nDCG@10");
        }
        else{
            System.out.print("Best c of PL2 for mean nDCG@10 is: ");
            for (int i : dcg_result){
                System.out.print(c[i] + "\t");
            }
            System.out.println();
        }
        List<Integer> err_result = isResultTure(err_train_index,err_test_index,ql_ERR_test,testing_set.size());
        if (err_result.size() ==0){
            System.out.println(" no queries in testing set match the result of training set for mean nERR@10");
        }
        else{
            System.out.print("Best c of PL2 for mean nERR@10 is: ");
            for (int i : err_result){
                System.out.print(c[i] + "\t");
            }
            System.out.println();
        }
        System.out.println("*******************************************************");

    }


}
