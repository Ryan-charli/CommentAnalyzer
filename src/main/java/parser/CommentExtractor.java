package parser;

import analysis.CommentLocation;
import ui.ProgressListener;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class CommentExtractor {
    private final String fileExtension;
    private ProgressListener progressListener;
    private BiConsumer<String, List<CommentLocation>> resultCallback;
    private volatile boolean isCancelled = false;
    
    public CommentExtractor(String language) {
        this.fileExtension = "." + language.toLowerCase();
    }

    public void setProgressListener(ProgressListener listener) {
        this.progressListener = listener;
    }

    public void setResultCallback(BiConsumer<String, List<CommentLocation>> callback) {
        this.resultCallback = callback;
    }

    public void cancel() {
        isCancelled = true;
    }

    public void extractCommentsFromDirectory(File directory) throws IOException {
        isCancelled = false;
        updateProgress(0, "Starting directory scan...");
        
        // Count total files
        AtomicInteger totalFiles = new AtomicInteger(0);
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(fileExtension)) {
                    totalFiles.incrementAndGet();
                }
                return FileVisitResult.CONTINUE;
            }
        });

        if (totalFiles.get() == 0) {
            updateProgress(100, "No files found to analyze");
            return;
        }

        // Process files
        AtomicInteger processedFiles = new AtomicInteger(0);
        
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isCancelled) {
                    return FileVisitResult.TERMINATE;
                }
                
                if (file.toString().endsWith(fileExtension)) {
                    try {
                        String relativePath = getRelativePath(directory.toPath(), file);
                        updateProgress(
                            (processedFiles.get() * 100) / totalFiles.get(),
                            String.format("Analyzing file (%d/%d): %s", 
                                processedFiles.get() + 1, 
                                totalFiles.get(),
                                relativePath)
                        );
                        
                        // Process single file and send results immediately
                        List<CommentLocation> fileComments = extractCommentsFromFile(file.toFile());
                        if (resultCallback != null) {
                            resultCallback.accept(relativePath, fileComments);
                        }
                        
                        processedFiles.incrementAndGet();
                        
                    } catch (IOException e) {
                        System.err.println("Error processing file: " + file + ": " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private String getRelativePath(Path base, Path file) {
        try {
            return base.relativize(file).toString();
        } catch (IllegalArgumentException e) {
            return file.getFileName().toString();
        }
    }

    private List<CommentLocation> extractCommentsFromFile(File file) throws IOException {
        List<CommentLocation> comments = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            boolean inMultiLineComment = false;
            StringBuilder multiLineComment = new StringBuilder();
            int multiLineStartNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();

                if (inMultiLineComment) {
                    multiLineComment.append("\n").append(line);
                    if (trimmedLine.contains("*/")) {
                        inMultiLineComment = false;
                        String comment = multiLineComment.toString().trim();
                        comments.add(new CommentLocation(
                            file.getAbsolutePath(),
                            multiLineStartNumber,
                            comment
                        ));
                        multiLineComment = new StringBuilder();
                    }
                    continue;
                }

                // Handle single-line comments
                int singleLineIndex = line.indexOf("//");
                if (singleLineIndex >= 0 && !isInString(line, singleLineIndex)) {
                    String comment = line.substring(singleLineIndex).trim();
                    comments.add(new CommentLocation(
                        file.getAbsolutePath(),
                        lineNumber,
                        comment
                    ));
                }

                // Handle multi-line comments
                int multiLineIndex = line.indexOf("/*");
                if (multiLineIndex >= 0 && !isInString(line, multiLineIndex)) {
                    inMultiLineComment = true;
                    multiLineStartNumber = lineNumber;
                    multiLineComment.append(line);
                    
                    // Check if multi-line comment ends on the same line
                    if (trimmedLine.contains("*/")) {
                        inMultiLineComment = false;
                        String comment = multiLineComment.toString().trim();
                        comments.add(new CommentLocation(
                            file.getAbsolutePath(),
                            lineNumber,
                            comment
                        ));
                        multiLineComment = new StringBuilder();
                    }
                }
            }
        }
        return comments;
    }

    private boolean isInString(String line, int index) {
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < index; i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                escaped = !escaped;
            } else if (c == '"' && !escaped) {
                inString = !inString;
                escaped = false;
            } else {
                escaped = false;
            }
        }
        
        return inString;
    }

    private void updateProgress(int progress, String status) {
        if (progressListener != null) {
            progressListener.updateProgress(progress, status);
        }
    }

    public void shutdown() {
        cancel();
    }
}