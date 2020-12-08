import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.JSONOutputter;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.queryparser.classic.QueryParser;


import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Indexer {
    private String path_to_wiki_files;
    private String path_to_tokenized_files;
    private String path_to_index;
    private Properties props;
    private StanfordCoreNLP pipeline;

    public Indexer(String path, String token_path, String path_index) {
        path_to_wiki_files = path;
        path_to_tokenized_files = token_path;
        path_to_index = path_index;
        props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        pipeline = new StanfordCoreNLP(props);
    }

    private ArrayList<String> get_lemmatized_tokens(String text) {
        CoreDocument document = pipeline.processToCoreDocument(text);

        ArrayList<String> results = new ArrayList<>();

        for (CoreLabel tok : document.tokens()) {
            results.add(tok.lemma());
        }

//        ArrayList<String> results = new ArrayList();
//        results.addAll(Arrays.asList(text.split(" ")));

        return results;
    }

    public void build_index_from_docs(Hashtable<String, ArrayList<String>> docs) {
        try {
            Analyzer analyzer = new StandardAnalyzer();
            Directory directory = FSDirectory.open(Paths.get(path_to_index));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(directory, config);

            for (String doc_id : docs.keySet()) {
                String doc_content = String.join(" ", docs.get(doc_id));

                Document doc = new Document();
                doc.add(new StringField("doc_id", doc_id, Field.Store.YES));
                doc.add(new TextField("lematized_tokens", doc_content, Field.Store.YES));
//                System.out.println(doc_id);
                writer.addDocument(doc);
            }

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String> search_query(String query) {
        ArrayList<String> hit_doc_ids = new ArrayList<>();

        try {
            Analyzer analyzer = new StandardAnalyzer();
            Directory directory = FSDirectory.open(Paths.get(path_to_index));
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(directory, config);

            DirectoryReader reader = DirectoryReader.open(writer);

            IndexSearcher searcher = new IndexSearcher(reader);

            ArrayList<String> query_tokens = this.get_lemmatized_tokens(query);

            String querystr = String.join(" ", query_tokens);

            Query q = null;

            q = new QueryParser("lematized_tokens", analyzer).parse(querystr);

            int hitsPerPage = 10;

            TopDocs docs = searcher.search(q, hitsPerPage);
            ScoreDoc[] hits = docs.scoreDocs;

            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                hit_doc_ids.add(d.get("doc_id"));
            }

            reader.close();
            writer.close();
            directory.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return hit_doc_ids;
    }

    public void build_index_from_wikipedia() {
        File path_files = new File(path_to_wiki_files);
        Hashtable<String, ArrayList<String>> docs = new Hashtable<>();

        ArrayList<Runnable> tokenizers = new ArrayList<>();

        try {

            for (String s : path_files.list()) {
                File f = new File(path_files, s);

                File path_tokenized_files = new File(path_to_tokenized_files);
                File f_r = new File(path_tokenized_files, "tokenized_" + s);
                Runnable t = new FileTokenizer(f, f_r.toPath().toString());
                tokenizers.add(t);

            }
            System.out.println(tokenizers.size());
            ExecutorService pool = Executors.newFixedThreadPool(4);
            for (Runnable t : tokenizers) {
                pool.execute(t);
            }

            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MINUTES);


            for (Runnable t : tokenizers) {
                FileTokenizer ft = (FileTokenizer) t;
                Hashtable<String, ArrayList<String>> d = ft.read_from_file();
                for (String k : d.keySet()) {
                    docs.put(k, d.get(k));
                }
            }

            build_index_from_docs(docs);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
