package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

// Extracting comments from a file
public class CommentExtractor {
    // multi-threaded processing
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    // Detects the programming language from the file extension, returns a language identifier string.
    private String detectLanguage(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".java")) return "java";
        if (fileName.endsWith(".c") || fileName.endsWith(".h")) return "c";
        if (fileName.endsWith(".py")) return "python";
        return "unknown"; // Unsupported file types
    }

    // Extract from file, return list
    public List<String> extractComments(File file) throws IOException {
        String language = detectLanguage(file);  // Auto-detect language
        if ("unknown".equals(language)) {
            System.out.println("Unsupported file types：" + file.getName());
            return new ArrayList<>();
        }

        // Get annotation symbols by language
        String[] symbols = LanguageConfig.getCommentSymbols(language);
        List<String> comments = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inMultiLineComment = false;

            // Loop through each line to check for comments
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (inMultiLineComment) {
                    comments.add(line);
                    if (line.contains(symbols[2])) inMultiLineComment = false;
                } else if (line.startsWith(symbols[0])) {
                    comments.add(line);
                } else if (line.contains(symbols[1])) {
                    comments.add(line);
                    inMultiLineComment = true;
                }
            }
        }
        return comments;
    }

    // Recursively extract all file comments from a directory
    public List<String> extractCommentsFromDirectory(File dir) throws IOException, InterruptedException {
        List<String> comments = new ArrayList<>();
        List<Future<List<String>>> futures = new ArrayList<>();

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                comments.addAll(extractCommentsFromDirectory(file));  // Handling subdirectories
            } else {
                Future<List<String>> future = executor.submit(() -> extractComments(file));
                futures.add(future);  // Asynchronous processing of documents
            }
        }

        for (Future<List<String>> future : futures) {
            try {
                comments.addAll(future.get());
            } catch (ExecutionException e) {
                System.err.println("Error while processing file: " + e.getMessage());
            }
        }

        executor.shutdown();
        return comments;
    }
}
