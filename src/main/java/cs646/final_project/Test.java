package cs646.final_project;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Test {
    public static void main( String[] args ) throws IOException{
        String wordweights_path = "C:\\mallet\\results\\robust04_400_30_wordweights";
        BufferedReader ww_reader = new BufferedReader( new InputStreamReader(new FileInputStream(new File(wordweights_path))));
        String line;
        Map<Integer,Map<String,Double>> wordweight_topic = new HashMap<>();
        Map<String, Double> wd;
        Map<Integer,Double> sum_wordweight_topic = new HashMap<>();
        int ln =0;
        while ((line = ww_reader.readLine()) != null){
            System.out.println("line "+ln);
            String[] s = line.split("\\s+");
            int i = Integer.parseInt(s[0]);
            String w = s[1];
            double score = Double.parseDouble(s[2]);
            sum_wordweight_topic.put(i,sum_wordweight_topic.getOrDefault(i,0.0)+score);
            wd = wordweight_topic.getOrDefault(i,new HashMap<>());
            wd.put(w,score);
            wordweight_topic.put(i,wd);
            ln++;
        }
        Map<Integer,Map<String,Double>> phi = new HashMap<>();
        for (int t: wordweight_topic.keySet()){
            System.out.println("topic "+ t);
            wd = wordweight_topic.get(t);
            for (String str : wd.keySet()){
                wd.put(str,wd.get(str)/sum_wordweight_topic.get(t));
            }
            phi.put(t,wd);
        }
        ww_reader.close();

        System.out.println("topic num: "+ phi.size());
        System.out.println(phi.get(0).size());
        System.out.println(phi.get(1).size());
    }
}
