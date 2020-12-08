import jdk.nashorn.internal.runtime.arrays.ArrayLikeIterator;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Indexer ind = new Indexer(args[0], args[1], args[2]);
        ind.build_index_from_wikipedia();
//        ArrayList<String> res = ind.search_query("missile blue");
//
//        for(String d:res)
//        {
//            System.out.println(d);
//        }
    }
}
