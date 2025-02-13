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
    private ProgressListener progressListener;
    private BiConsumer<String, List<CommentLocation>> resultCallback;
    private volatile boolean isCancelled = false;
    private final CodeParser parser;
    
    public CommentExtractor(String language) {
        this.parser = new CodeParser();
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
        
        AtomicInteger totalFiles = countSupportedFiles(directory);

        if (totalFiles.get() == 0) {
            updateProgress(100, "No supported files found to analyze");
            return;
        }

        processFiles(directory, totalFiles);
    }

    private AtomicInteger countSupportedFiles(File directory) throws IOException {
        AtomicInteger totalFiles = new AtomicInteger(0);
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.toString().toLowerCase();
                if (isSupportedFile(fileName)) {
                    totalFiles.incrementAndGet();
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return totalFiles;
    }

    private void processFiles(File directory, AtomicInteger totalFiles) throws IOException {
        AtomicInteger processedFiles = new AtomicInteger(0);
        
        Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isCancelled) {
                    return FileVisitResult.TERMINATE;
                }
                
                String fileName = file.toString();
                if (isSupportedFile(fileName)) {
                    try {
                        processFile(file, directory.toPath(), processedFiles, totalFiles);
                    } catch (IOException e) {
                        System.err.println("Error processing file: " + file + ": " + e.getMessage());
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void processFile(Path file, Path basePath, AtomicInteger processedFiles, AtomicInteger totalFiles) throws IOException {
        String relativePath = getRelativePath(basePath, file);
        updateProgress(
            (processedFiles.get() * 100) / totalFiles.get(),
            String.format("Analyzing file (%d/%d): %s", 
                processedFiles.get() + 1, 
                totalFiles.get(),
                relativePath)
        );

        // Read first line for language detection
        String firstLine = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            firstLine = reader.readLine();
        }

        // Auto-detect language and set it in parser
        String detectedLanguage = LanguageConfig.detectLanguage(file.toString(), firstLine);
        if (detectedLanguage != null) {
            parser.setLanguage(detectedLanguage);
            List<CommentLocation> fileComments = parser.extractCommentsWithLocations(file.toFile());
            if (resultCallback != null && !fileComments.isEmpty()) {
                resultCallback.accept(relativePath, fileComments);
            }
        }
        
        processedFiles.incrementAndGet();
    }

    private boolean isSupportedFile(String fileName) {
        String ext = getFileExtension(fileName);
        return LanguageConfig.isSupportedExtension(ext);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex).toLowerCase() : "";
    }

    private String getRelativePath(Path base, Path file) {
        try {
            return base.relativize(file).toString();
        } catch (IllegalArgumentException e) {
            return file.getFileName().toString();
        }
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