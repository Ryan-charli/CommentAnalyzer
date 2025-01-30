package analysis;

import parser.CodeParser;
import java.io.*;
import java.util.*;

public class CommentAnalyzer {
    private CodeParser parser;
    private List<String> comments;
    private final CodeQualityAnalyzer qualityAnalyzer;

    public CommentAnalyzer(String language) {
        this.parser = new CodeParser(language);
        this.comments = new ArrayList<>();
        this.qualityAnalyzer = new CodeQualityAnalyzer();
    }

    public double analyzeCommentQuality(String comment) {
        return qualityAnalyzer.analyzeCommentQuality(comment);
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

    // Get a quality report of all annotations
    public Map<String, Double> getQualityReport() {
        Map<String, Double> report = new HashMap<>();
        double totalScore = 0;
        
        for (String comment : comments) {
            double score = analyzeCommentQuality(comment);
            report.put(comment, score);
            totalScore += score;
        }
        
        report.put("Average Score", comments.isEmpty() ? 0 : totalScore / comments.size());
        return report;
    }
}