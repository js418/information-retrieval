package cs646.stats;

import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.List;

public interface ScoringFunction {

    /**
     * @param weights Weight of the query terms, e.g., P(t|q) or c(t,q).
     * @param tfs     The frequencies of the query terms in documents.
     * @param tfcs    The frequencies of the query terms in the corpus.
     * @param dl      The length of the document.
     * @param index   The index of all files
     * @return
     */
    double score(List<Double> weights, List<Double> tfs, List<Double> tfcs, double dl, int docid, IndexReader index) throws IOException;
}
