package analysis;

import parser.CodeParser;
import java.io.*;
import java.util.*;

public class CommentAnalyzer {
    private CodeParser parser;
    private List<String> comments;
    private final CodeQualityAnalyzer qualityAnalyzer;
    private final AIComparison aiComparison;
    private final OllamaClient ollamaClient;

    public CommentAnalyzer(String language) {
        this.parser = new CodeParser(language);
        this.comments = new ArrayList<>();
        this.qualityAnalyzer = new CodeQualityAnalyzer(true);
        this.aiComparison = new AIComparison();
        this.ollamaClient = new OllamaClient("http://localhost:11434");
    }

    public double analyzeCommentQuality(String comment) {
        return qualityAnalyzer.analyzeCommentQuality(comment, "", false).getScore();
    }

    public AnalysisResult analyzeCommentWithDetails(String comment, String nextCodeLine, boolean isFirst) {
        CodeQualityAnalyzer.QualityAnalysisResult quality = 
            qualityAnalyzer.analyzeCommentQuality(comment, nextCodeLine, isFirst);
        
        String aiGeneratedComment = ollamaClient.generateComment(nextCodeLine);
        String comparisonResult = aiComparison.compareComments(
            Arrays.asList(comment), aiGeneratedComment);

        return new AnalysisResult(quality.getScore(), quality.getDetails(), comparisonResult);
    }

    public void analyzeFile(File file) {
        try {
            List<CommentLocation> commentLocations = parser.extractCommentsWithLocations(file);
            for (CommentLocation loc : commentLocations) {
                comments.add(loc.getContent());
            }
            System.out.println("File: " + file.getName() + " has " + commentLocations.size() + " comments.");
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
        }
    }

    public void analyzeDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isFile()) {
                    analyzeFile(file);
                }
            }
        }
    }

    public int getTotalCommentCount() {
        return comments.size();
    }

    public void displayComments() {
        System.out.println("Total Comments: " + getTotalCommentCount());
        double totalScore = 0;
        
        for (String comment : comments) {
            double score = analyzeCommentQuality(comment);
            totalScore += score;
            System.out.printf("Comment: %s%nQuality Score: %.2f%n%n", 
                comment, score);
        }
        
        double averageScore = comments.isEmpty() ? 0 : totalScore / comments.size();
        System.out.printf("Average Quality Score: %.2f%n", averageScore);
    }

    public void addComments(List<String> newComments) {
        comments.addAll(newComments);
    }

    public Map<String, Object> getQualityReport() {
        Map<String, Object> report = new HashMap<>();
        Map<String, CommentDetails> commentScores = new HashMap<>();
        double totalScore = 0;
        
        for (String comment : comments) {
            AnalysisResult result = analyzeCommentWithDetails(comment, "", false);
            commentScores.put(comment, new CommentDetails(
                result.getScore(),
                result.getDetails(),
                result.getAiComparison()
            ));
            totalScore += result.getScore();
        }
        
        report.put("comments", commentScores);
        report.put("averageScore", comments.isEmpty() ? 0 : totalScore / comments.size());
        return report;
    }

    public static class AnalysisResult {
        private final double score;
        private final String details;
        private final String aiComparison;

        public AnalysisResult(double score, String details, String aiComparison) {
            this.score = score;
            this.details = details;
            this.aiComparison = aiComparison;
        }

        public double getScore() { return score; }
        public String getDetails() { return details; }
        public String getAiComparison() { return aiComparison; }
    }

    private static class CommentDetails {
        private final double score;
        private final String details;
        private final String aiComparison;

        CommentDetails(double score, String details, String aiComparison) {
            this.score = score;
            this.details = details;
            this.aiComparison = aiComparison;
        }
    }
}