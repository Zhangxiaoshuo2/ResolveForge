package dev.resolveforge.rag;

import java.io.IOException;
import java.util.*;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;

public final class Bm25Index implements AutoCloseable {
    public record Hit(PolicyChunk chunk, double score) {}
    private final SmartChineseAnalyzer analyzer = new SmartChineseAnalyzer();
    private final ByteBuffersDirectory directory = new ByteBuffersDirectory();
    private final DirectoryReader reader;
    private final IndexSearcher searcher;
    private final Map<String,PolicyChunk> chunks = new HashMap<>();
    private final Map<String,Set<String>> terms = new HashMap<>();
    public Bm25Index(List<PolicyChunk> input) {
        try {
            var config = new IndexWriterConfig(analyzer).setSimilarity(new BM25Similarity());
            try(var writer = new IndexWriter(directory,config)) {
                for(var chunk : input) {
                    var doc = new Document();
                    doc.add(new StringField("id",chunk.chunkId(), Field.Store.YES));
                    doc.add(new TextField("title",chunk.title(),Field.Store.NO));
                    doc.add(new TextField("content",chunk.content(),Field.Store.NO));
                    writer.addDocument(doc); chunks.put(chunk.chunkId(),chunk); terms.put(chunk.chunkId(),tokens(chunk.embeddingText()));
                }
            }
            reader=DirectoryReader.open(directory); searcher=new IndexSearcher(reader); searcher.setSimilarity(new BM25Similarity());
        } catch(IOException ex) { throw new IllegalStateException("bm25_index_failed",ex); }
    }
    public Set<String> tokens(String text) {
        Set<String> result = new LinkedHashSet<>();
        try(var stream = analyzer.tokenStream("content",text)) {
            var term=stream.addAttribute(CharTermAttribute.class); stream.reset();
            while(stream.incrementToken()) result.add(term.toString()); stream.end(); return result;
        } catch(IOException ex) { throw new IllegalStateException(ex); }
    }
    public List<Hit> search(String text, int recallK, double minScore, double minCoverage) {
        Set<String> queryTerms=tokens(text);
        if(queryTerms.isEmpty()) return List.of();
        var query = new BooleanQuery.Builder();
        for(String term:queryTerms) {
            var termQuery = new BooleanQuery.Builder().add(new BoostQuery(new TermQuery(new Term("title",term)),1.7f),BooleanClause.Occur.SHOULD)
                    .add(new TermQuery(new Term("content",term)),BooleanClause.Occur.SHOULD).build();
            query.add(termQuery,BooleanClause.Occur.SHOULD);
        }
        try {
            List<Hit> result=new ArrayList<>();
            for(var hit:searcher.search(query.build(),recallK).scoreDocs) {
                String id=searcher.storedFields().document(hit.doc).get("id");
                long overlap=queryTerms.stream().filter(terms.get(id)::contains).count();
                if(hit.score>=minScore && (double)overlap/queryTerms.size()>=minCoverage)
                    result.add(new Hit(chunks.get(id),hit.score));
            }
            return result.stream().sorted(Comparator.comparingDouble(Hit::score).reversed().thenComparing(h->h.chunk().chunkId())).toList();
        } catch(IOException ex) { throw new IllegalStateException("bm25_search_failed",ex); }
    }
    public void close() { try {reader.close();directory.close();analyzer.close();} catch(IOException ex) {throw new IllegalStateException(ex);} }
}
