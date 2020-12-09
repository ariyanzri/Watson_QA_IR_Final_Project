import edu.stanford.nlp.util.Index;
import jdk.nashorn.internal.runtime.arrays.ArrayLikeIterator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Question_Parser
{
    public ArrayList<Question> questions = new ArrayList<>();
    public Indexer indexer;

    public Question_Parser(String question_file, Indexer ind)
    {
        indexer = ind;
        try
        {
            File question_file_path = new File(question_file);
            List<String> lines = Files.readAllLines(question_file_path.toPath());

            ArrayList<String> tmp = new ArrayList<>();

            for (String l : lines)
            {
                if (l.isEmpty())
                {
                    Question q = new Question();
                    q.category = tmp.get(0);
                    q.clue = tmp.get(1);
                    if (tmp.get(2).contains("|"))
                    {
                        q.answers.addAll(Arrays.asList(tmp.get(2).split("\\|")));
                    } else
                    {
                        q.answers.add(tmp.get(2));
                    }
                    questions.add(q);

                    tmp = new ArrayList<>();
                } else
                {
                    tmp.add(l);
                }
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void search_all_questions()
    {
        float acc = 0;
        for (Question q : questions)
        {
            ArrayList<String> tokens = indexer.get_lemmatized_tokens(q.clue+" "+q.category,true);
            String doc_id = indexer.find_most_relevant(String.join(" ", tokens));
            String answers = String.join(",", q.answers);
            String r;
            if (answers.contains(doc_id))
            {
                r = "CORRECT ";
                acc+=1;
            } else
            {
                r = "INCORRECT ";
            }

            System.out.println(r + "Ground Truth: " + answers + " - Watson result: " + doc_id);
        }
        System.out.println("Accuracy: "+acc/100.0);
    }

    public void search_all_questions_top_k(int k)
    {
        float acc = 0;
        for (Question q : questions)
        {
            ArrayList<String> tokens = indexer.get_lemmatized_tokens(q.clue+" "+q.category,true);
            ArrayList<String> top_k_doc_id = indexer.find_top_k_relevant(String.join(" ", tokens),k);
            String answers = String.join(",", q.answers);
            String r;

            int i =0;
            for(String doc_id:top_k_doc_id)
            {
                i+=1;
                if (answers.contains(doc_id))
                {
                    r = "@ position "+i+" ";
                    System.out.println(r + "Ground Truth: " + answers + " - Watson result: " + doc_id);
                    acc+=1;
                    break;
                }
            }
            if(i==top_k_doc_id.size())
            {
                r = "INCORRECT ";
                System.out.println(r + "Ground Truth: " + answers + " - Watson result: " + "NONE");
            }

        }
        System.out.println("Accuracy: "+acc/100.0);
    }
}

class Question
{
    public String clue;
    public ArrayList<String> answers = new ArrayList<>();
    public String category;
}
