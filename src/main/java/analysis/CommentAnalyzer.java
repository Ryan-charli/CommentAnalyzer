package analysis;

import parser.CodeParser;
import parser.LanguageConfig;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CommentAnalyzer {
    private final CodeParser parser;
    private final Map<String, Map<CommentTypeAnalyzer.CommentType, List<CommentLocation>>> categorizedCommentsByLanguage;
    private final BatchProcessor batchProcessor;
    private final ExecutorService aiExecutor;
    private final CommentTypeAnalyzer typeAnalyzer;
    private final CodeQualityAnalyzer qualityAnalyzer;

    public CommentAnalyzer() {
        this.parser = new CodeParser();
        this.categorizedCommentsByLanguage = new ConcurrentHashMap<>();
        this.batchProcessor = new BatchProcessor(20, 50, 2);
        this.aiExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
        this.typeAnalyzer = new CommentTypeAnalyzer();
        this.qualityAnalyzer = new CodeQualityAnalyzer(true);
    }

    public CodeQualityAnalyzer.QualityAnalysisResult getCommentQuality(
            String comment, String nextCodeLine, boolean isFirstComment) {
        return qualityAnalyzer.analyzeCommentQuality(comment, nextCodeLine, isFirstComment);
    }
    
    public void analyzeFile(File file) {
        try {
            String detectedLanguage = LanguageConfig.detectLanguage(file.getName(), null);
            if (detectedLanguage == null) {
                System.err.println("Unsupported file type: " + file.getName());
                return;
            }

            parser.setLanguage(detectedLanguage);
            List<CommentLocation> commentLocations = parser.extractCommentsWithLocations(file);
            
            synchronized (categorizedCommentsByLanguage) {
                categorizeComments(commentLocations, detectedLanguage);
            }
            
            System.out.printf("File: %s (%s) has %d comments.%n", 
                file.getName(), detectedLanguage, commentLocations.size());
                    
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
        }
    }

    private void categorizeComments(List<CommentLocation> comments, String language) {
        Map<CommentTypeAnalyzer.CommentType, List<CommentLocation>> languageComments = 
            categorizedCommentsByLanguage.computeIfAbsent(language, _ -> new EnumMap<>(CommentTypeAnalyzer.CommentType.class));

        for (CommentLocation comment : comments) {
            CommentTypeAnalyzer.CommentAnalysisResult analysis = typeAnalyzer.analyzeCommentType(
                comment.getContent(), "", "", comment.getLineNumber() == 1,
                true, false, false
            );
            languageComments.computeIfAbsent(analysis.getType(), _ -> new ArrayList<>())
                           .add(comment);
        }
    }

    public void analyzeDirectory(File directory) {
        if (directory.isDirectory()) {
            Arrays.stream(directory.listFiles())
                  .parallel()
                  .forEach(file -> {
                      if (file.isFile()) {
                          String ext = getFileExtension(file.getName());
                          if (LanguageConfig.isSupportedExtension(ext)) {
                              analyzeFile(file);
                          }
                      } else if (file.isDirectory()) {
                          analyzeDirectory(file);
                      }
                  });
        }
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot) : "";
    }

    public Map<String, Object> getBasicReport() {
        Map<String, Object> report = new HashMap<>();
        Map<String, Map<String, List<CommentAnalysis>>> commentsByLanguageAndType = processBasicAnalysis();
        report.put("commentsByLanguageAndType", commentsByLanguageAndType);
        report.put("totalCommentsByLanguage", getTotalCommentCounts());
        report.put("analysisTime", new Date());
        return report;
    }

    private Map<String, Map<String, List<CommentAnalysis>>> processBasicAnalysis() {
        Map<String, Map<String, List<CommentAnalysis>>> result = new HashMap<>();
        
        for (String language : categorizedCommentsByLanguage.keySet()) {
            Map<String, List<CommentAnalysis>> commentsByType = new HashMap<>();
            Map<CommentTypeAnalyzer.CommentType, List<CommentLocation>> languageComments = 
                categorizedCommentsByLanguage.get(language);

            for (Map.Entry<CommentTypeAnalyzer.CommentType, List<CommentLocation>> entry : languageComments.entrySet()) {
                List<CommentAnalysis> analyses = entry.getValue().stream()
                    .map(comment -> {
                        double basicScore = calculateBasicScore(comment);
                        return new CommentAnalysis(comment, basicScore, 
                            "Basic analysis score: " + String.format("%.2f", basicScore), null);
                    })
                    .collect(Collectors.toList());
                commentsByType.put(entry.getKey().getDescription(), analyses);
            }

            result.put(language, commentsByType);
        }

        return result;
    }

    private double calculateBasicScore(CommentLocation comment) {
        double score = 0.5;
        String content = comment.getContent();
        
        if (content.length() > 10) score += 0.1;
        if (content.length() > 50) score += 0.1;
        if (content.matches("^\\s*[A-Z].*")) score += 0.1;
        if (!content.contains("TODO") && !content.contains("FIXME")) score += 0.1;
        if (content.contains("@param") || content.contains("@return")) score += 0.1;

        return Math.min(score, 1.0) * 5.0; // Convert to 5-point scale
    }

    public void startAIAnalysis(Consumer<Map<String, Object>> callback) {
        CompletableFuture.runAsync(() -> {
            for (String language : categorizedCommentsByLanguage.keySet()) {
                Map<CommentTypeAnalyzer.CommentType, List<CommentLocation>> languageComments = 
                    categorizedCommentsByLanguage.get(language);
                
                for (List<CommentLocation> comments : languageComments.values()) {
                    List<CommentLocation> highPriorityComments = comments.stream()
                        .filter(this::isHighPriorityComment)
                        .collect(Collectors.toList());

                    if (!highPriorityComments.isEmpty()) {
                        batchProcessor.submitBatch(highPriorityComments, result -> {
                            callback.accept(createAIAnalysisReport(language, result));
                        });
                    }
                }
            }
        }, aiExecutor);
    }

    private boolean isHighPriorityComment(CommentLocation comment) {
        return comment.getContent().length() > 100 || // 只分析长注释
               comment.getFileName().endsWith("Test.java"); // 或测试文件注释
    }

    private Map<String, Object> createAIAnalysisReport(String language, Map<String, Object> aiResult) {
        Map<String, Object> report = new HashMap<>();
        report.put("language", language);
        report.put("aiAnalysis", aiResult);
        report.put("analysisTime", new Date());
        return report;
    }

    private Map<String, Integer> getTotalCommentCounts() {
        return categorizedCommentsByLanguage.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().values().stream()
                    .mapToInt(List::size)
                    .sum()
            ));
    }

    public void shutdown() {
        batchProcessor.shutdown();
        aiExecutor.shutdown();
        try {
            if (!aiExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                aiExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            aiExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static class CommentAnalysis {
        private final CommentLocation comment;
        private final double score;
        private final String details;
        private final String aiComparison;

        public CommentAnalysis(CommentLocation comment, double score, 
                             String details, String aiComparison) {
            this.comment = comment;
            this.score = score;
            this.details = details;
            this.aiComparison = aiComparison;
        }

        public CommentLocation getComment() { return comment; }
        public double getScore() { return score; }
        public String getDetails() { return details; }
        public String getAiComparison() { return aiComparison; }
        
    }
}