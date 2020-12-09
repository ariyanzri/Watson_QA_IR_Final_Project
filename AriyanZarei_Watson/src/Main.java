import jdk.nashorn.internal.runtime.arrays.ArrayLikeIterator;

import java.util.ArrayList;
import java.util.Hashtable;

public class Main {
    public static void main(String[] args) {
        Indexer ind = new Indexer(args[0], args[1], args[2]);
        ind.build_index_from_wikipedia();
//        Hashtable<String,Float> res = ind.search_query("iran demographics");
//
//        for(String d:res.keySet())
//        {
//            System.out.println(d+": "+res.get(d));
//
//        }
//        Question_Parser p = new Question_Parser(args[3],ind);
//        p.search_all_questions();
//        p.search_all_questions_top_k(100);
    }
}
