package es2.virtualThreads;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class WordCounterImpl implements WordCounter{

    public void getWordOccurrences(String webAddress, String word, int depth){
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var res = executor.submit(new WebCrawler(executor, webAddress, 1, depth, word, new ArrayList<>()));
            try {
                int totalOccurrences = res.get().values().stream().mapToInt(Integer::intValue).sum();
                res.get().forEach((x, y) -> {
                    System.out.println("[In " + x + " local occurrences: " + y + "]");
                });
                System.out.println("Total occurrences: " + totalOccurrences);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } // close() called implicitly.
    }
}
