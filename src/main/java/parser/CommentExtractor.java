package parser;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class CommentExtractor {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final CodeParser parser;

    public CommentExtractor(String language) {
        this.parser = new CodeParser(language);
    }

    private boolean isSourceFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".java") || 
               name.endsWith(".py") || 
               name.endsWith(".c") || 
               name.endsWith(".cpp") ||
               name.endsWith(".h");
    }

    public List<String> extractComments(File file) throws IOException {
        if (!isSourceFile(file)) {
            return new ArrayList<>();
        }
        return parser.extractComments(file);
    }

    public List<String> extractCommentsFromDirectory(File dir) throws IOException {
        List<String> allComments = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        collectFiles(dir, files);
        
        for (File file : files) {
            allComments.addAll(extractComments(file));
        }
        
        return allComments;
    }
    
    private void collectFiles(File dir, Queue<File> files) {
        File[] filesList = dir.listFiles();
        if (filesList != null) {
            for (File file : filesList) {
                if (file.isDirectory()) {
                    collectFiles(file, files);
                } else if (isSourceFile(file)) {
                    files.offer(file);
                }
            }
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}