import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FileTokenizer implements Runnable
{
    public File path;
    public Path results;
    private Properties props;
    private StanfordCoreNLP pipeline;

    public FileTokenizer(File p,String res)
    {
        this.path = p;
        this.results = new File(res).toPath();
        props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma");
        pipeline = new StanfordCoreNLP(props);
    }

    public void save_to_file(Hashtable<String,ArrayList<String>> docs)
    {
        try {
            FileWriter fileWriter = new FileWriter(this.results.toString());
            PrintWriter printWriter = new PrintWriter(fileWriter);
            for(String key:docs.keySet())
            {
                printWriter.println(key+"****"+String.join("***",docs.get(key)));
            }
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Hashtable<String,ArrayList<String>> read_from_file()
    {
        Hashtable<String,ArrayList<String>> docs = new Hashtable<>();

        try {
            File f = new File(this.results.toString());
            List<String> lines = Files.readAllLines(f.toPath());

            for(String l:lines)
            {
                String[] file_name_tokens = l.split("[\\*]{4}");
                String doc_n = file_name_tokens[0];

                ArrayList<String> tokens = new ArrayList<>(Arrays.asList(file_name_tokens[1].split("[\\*]{3}")));
                docs.put(doc_n,tokens);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return docs;
    }

    private ArrayList<String> get_lemmatized_tokens(String text)
    {
        CoreDocument document = pipeline.processToCoreDocument(text);
        ArrayList<String> results = new ArrayList<>();
        Stemmer s = new Stemmer();

        for (CoreLabel tok : document.tokens()) {
            if(tok.lemma()==null)
            {
                results.add(tok.word());
            }
            else
            {
                results.add(tok.lemma());
            }

//            if(tok.lemma()==null)
//            {
//                results.add(s.stem(tok.word()));
//            }
//            else
//            {
//                results.add(s.stem(tok.lemma()));
//            }
        }

//        ArrayList<String> results = new ArrayList();
//        results.addAll(Arrays.asList(text.split(" ")));

        return results;
    }

    public void run()
    {
        Hashtable<String ,ArrayList<String>> docs = new Hashtable<>();

        try
        {
            List<String> lines = Files.readAllLines(this.path.toPath());

            String doc_name = "";
            String doc_content = "";

            String prev_line = "";
            for(String l: lines)
            {
                if (l.matches("^(\\[\\[).*(\\]\\])$") && prev_line.isEmpty())
                {
                    if (!doc_name.isEmpty())
                    {
                        ArrayList<String> doc_tokens = this.get_lemmatized_tokens(doc_content);
                        docs.put(doc_name,doc_tokens);
//                        System.out.println(doc_name+doc_tokens.size());
                    }
                    doc_name = l.replace("[","").replace("]","");
                    doc_content = "";
                }
                else
                {
                    doc_content+=l+" ";
                }
                prev_line = l;
            }

        }
        catch (Exception e)
        {
            // Throwing an exception
            System.out.println ("Exception is caught"+e.getMessage());
        }

        System.out.println("tokenization for "+Thread.currentThread().getId()+" is finished.");
        this.save_to_file(docs);
        System.out.println("File containing "+docs.size()+" documents successfully tokenized and saved.");
    }
}
