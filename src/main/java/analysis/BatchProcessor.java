package analysis;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BatchProcessor {
    private final BlockingQueue<CommentBatch> batchQueue;
    private final int BATCH_SIZE;
    private final ExecutorService aiExecutor;
    private final Map<String, AIAnalysisResult> aiCache;
    private final TwoPhaseAnalyzer analyzer;
    private volatile boolean isRunning = true;

    public BatchProcessor(int batchSize, int queueCapacity, int aiThreads) {
        this.BATCH_SIZE = batchSize;
        this.batchQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.aiExecutor = Executors.newFixedThreadPool(aiThreads);
        this.aiCache = new ConcurrentHashMap<>();
        this.analyzer = new TwoPhaseAnalyzer();
        startProcessing();
    }

    private void startProcessing() {
        Thread processingThread = new Thread(() -> {
            while (isRunning || !batchQueue.isEmpty()) {
                try {
                    CommentBatch batch = batchQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (batch != null) {
                        processBatch(batch);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        processingThread.setDaemon(true);
        processingThread.start();
    }

    private void processBatch(CommentBatch batch) {
        Map<String, Object> basicReport = analyzer.analyzeBasic(batch.comments);
        batch.callback.accept(basicReport);

        // Filter high priority comments for AI analysis
        List<CommentLocation> highPriorityComments = batch.comments.stream()
            .filter(this::isHighPriorityComment)
            .collect(Collectors.toList());

        if (!highPriorityComments.isEmpty()) {
            submitToAIAnalysis(highPriorityComments, batch.callback);
        }
    }

    private boolean isHighPriorityComment(CommentLocation comment) {
        CommentTypeAnalyzer.CommentType type = analyzer.typeAnalyzer.analyzeCommentType(
            comment.getContent(), "", "", true, true, false, false).getType();
        return type == CommentTypeAnalyzer.CommentType.FILE_COMMENT || 
               type == CommentTypeAnalyzer.CommentType.CLASS_COMMENT;
    }

    private void submitToAIAnalysis(List<CommentLocation> comments, Consumer<Map<String, Object>> callback) {
        CompletableFuture.supplyAsync(() -> {
            Map<String, Object> aiResults = new HashMap<>();
            for (CommentLocation comment : comments) {
                String cacheKey = comment.getContent().trim();
                AIAnalysisResult result = aiCache.computeIfAbsent(cacheKey,
                    k -> analyzer.aiAnalyzer.analyzeComment(comment));
                aiResults.put(comment.getFileName() + ":" + comment.getLineNumber(), result);
            }
            return aiResults;
        }, aiExecutor).thenAccept(results -> {
            Map<String, Object> aiReport = new HashMap<>();
            aiReport.put("aiAnalysis", results);
            callback.accept(aiReport);
        });
    }

    public void submitBatch(List<CommentLocation> comments, Consumer<Map<String, Object>> callback) {
        List<List<CommentLocation>> batches = new ArrayList<>();
        for (int i = 0; i < comments.size(); i += BATCH_SIZE) {
            batches.add(comments.subList(i, Math.min(i + BATCH_SIZE, comments.size())));
        }

        for (List<CommentLocation> batch : batches) {
            try {
                batchQueue.put(new CommentBatch(batch, callback));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void shutdown() {
        isRunning = false;
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

    private static class CommentBatch {
        final List<CommentLocation> comments;
        final Consumer<Map<String, Object>> callback;

        CommentBatch(List<CommentLocation> comments, Consumer<Map<String, Object>> callback) {
            this.comments = comments;
            this.callback = callback;
        }
    }
}

class TwoPhaseAnalyzer {
    final CommentTypeAnalyzer typeAnalyzer;
    final AIAnalyzer aiAnalyzer;

    TwoPhaseAnalyzer() {
        this.typeAnalyzer = new CommentTypeAnalyzer();
        this.aiAnalyzer = new AIAnalyzer();
    }

    Map<String, Object> analyzeBasic(List<CommentLocation> comments) {
        Map<String, Object> report = new HashMap<>();
        Map<CommentTypeAnalyzer.CommentType, List<CommentDetail>> categorizedComments = new EnumMap<>(CommentTypeAnalyzer.CommentType.class);

        for (CommentLocation comment : comments) {
            CommentTypeAnalyzer.CommentAnalysisResult analysis = typeAnalyzer.analyzeCommentType(
                comment.getContent(), "", "", true, true, false, false);
            
            CommentDetail detail = new CommentDetail(
                comment.getFileName(),
                comment.getLineNumber(),
                comment.getContent(),
                calculateBasicScore(comment.getContent(), analysis.getType())
            );

            categorizedComments.computeIfAbsent(analysis.getType(), k -> new ArrayList<>())
                              .add(detail);
        }

        report.put("categorizedComments", categorizedComments);
        report.put("totalComments", comments.size());
        return report;
    }

    private double calculateBasicScore(String content, CommentTypeAnalyzer.CommentType type) {
        double score = 0.5; // Base score
        
        // Add points based on comment type
        switch (type) {
            case FILE_COMMENT -> score += 0.2;
            case CLASS_COMMENT -> score += 0.15;
            case METHOD_COMMENT -> score += 0.1;
            default -> score += 0.05;
        }

        // Add points for length and formatting
        if (content.length() > 10) score += 0.1;
        if (content.length() > 50) score += 0.1;
        if (content.matches("^\\s*[A-Z].*")) score += 0.05; // Starts with capital letter
        if (!content.contains("TODO") && !content.contains("FIXME")) score += 0.05;

        return Math.min(score, 1.0);
    }

    private static class CommentDetail {
        final String fileName;
        final int lineNumber;
        final String content;
        final double basicScore;

        CommentDetail(String fileName, int lineNumber, String content, double basicScore) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.content = content;
            this.basicScore = basicScore;
        }
    }
}

class AIAnalyzer {
    private final OllamaClient ollamaClient;

    AIAnalyzer() {
        this.ollamaClient = new OllamaClient("http://localhost:11434");
    }

    AIAnalysisResult analyzeComment(CommentLocation comment) {
        try {
            String aiResponse = ollamaClient.generateAnalysis(comment.getContent());
            double relevanceScore = calculateRelevanceScore(aiResponse);
            return new AIAnalysisResult(aiResponse, relevanceScore);
        } catch (Exception e) {
            return new AIAnalysisResult("AI analysis failed: " + e.getMessage(), 0.0);
        }
    }

    private double calculateRelevanceScore(String aiResponse) {
        // Simple scoring based on response length and content
        double score = 0.5;
        if (aiResponse.length() > 50) score += 0.2;
        if (aiResponse.contains("purpose") || aiResponse.contains("functionality")) score += 0.1;
        if (aiResponse.contains("improvement") || aiResponse.contains("suggestion")) score += 0.1;
        if (!aiResponse.contains("error") && !aiResponse.contains("failed")) score += 0.1;
        return Math.min(score, 1.0);
    }
}

class AIAnalysisResult {
    final String analysis;
    final double relevanceScore;

    AIAnalysisResult(String analysis, double relevanceScore) {
        this.analysis = analysis;
        this.relevanceScore = relevanceScore;
    }
}