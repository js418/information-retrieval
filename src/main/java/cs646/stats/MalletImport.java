package cs646.stats;

/**
 * Create Mallet input text file
 */

import cs646.utils.LuceneQLSearcher;
import cs646.utils.LuceneUtils;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MalletImport {
    public static void main( String[] args ) throws Exception{
        String trec_path = "index_trec123";
        String robust04_path = "index_robust04";
        String pathStopwords = "stopwords_inquery";
        String[] index_pathname = new String[]{trec_path,robust04_path};
        String[] file_name = new String[]{"sample-data/trec123","sample-data/robust04"};

        // store the content information in .txt file as the input of Mallet
        for (int i=0; i<2; i++){
            LuceneQLSearcher searcher = new LuceneQLSearcher(index_pathname[i]);
            searcher.setStopwords(pathStopwords);
            File fout = new File(file_name[i]);
            FileOutputStream fos = new FileOutputStream(fout);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            Random r = new Random();
            List<Integer> doc_index = new ArrayList<>();
            for (int j = 0; j<searcher.getIndex().numDocs(); j++){
                doc_index.add(r.nextInt(searcher.getIndex().numDocs()));
            }
            for (int docid : doc_index){
                String docno = LuceneUtils.getDocno( searcher.getIndex(), "docno", docid );
                Terms t = searcher.getIndex().getTermVector( docid, "content" );
                String content = "";
                if (t == null){
                    content = "";
                }
                else{
                    TermsEnum iterator = t.iterator();
                    BytesRef br;
                    while ( ( br = iterator.next() ) != null ) {
                        String w =br.utf8ToString();
                        if(!searcher.isStopwords(w)){
                            content = content +  w + " ";
                        }
                    }
                }
                bw.write(docno + " " + "content"+ " " + content);
                bw.newLine();
            }
            searcher.close();
            bw.close();
        }

    }
}
