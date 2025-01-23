package analysis;

import parser.CodeParser;
import java.io.*;
import java.util.*;

public class CommentAnalyzer {
    private CodeParser parser;
    private List<String> comments;
    private CodeQualityAnalyzer qualityAnalyzer;

    public CommentAnalyzer(String language) {
        this.parser = new CodeParser(language);
        this.comments = new ArrayList<>();
        this.qualityAnalyzer = new CodeQualityAnalyzer();
    }

    public void analyzeFile(File file) {
        try {
            List<String> fileComments = parser.extractComments(file);
            comments.addAll(fileComments);
            System.out.println("File: " + file.getName() + " has " + fileComments.size() + " comments.");
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

    public Map<String, Double> analyzeCommentQuality() {
        Map<String, Double> qualityReport = new HashMap<>();
        double totalScore = 0;
        
        for (String comment : comments) {
            double score = qualityAnalyzer.analyzeCommentQuality(comment);
            qualityReport.put(comment, score);
            totalScore += score;
        }
        
        qualityReport.put("Average Score", comments.isEmpty() ? 0 : totalScore / comments.size());
        return qualityReport;
    }

    public void displayComments() {
        System.out.println("Total Comments: " + getTotalCommentCount());
        Map<String, Double> qualityReport = analyzeCommentQuality();
        
        for (Map.Entry<String, Double> entry : qualityReport.entrySet()) {
            if (!entry.getKey().equals("Average Score")) {
                System.out.printf("Comment: %s%nQuality Score: %.2f%n%n", 
                    entry.getKey(), entry.getValue());
            }
        }
        
        System.out.printf("Average Quality Score: %.2f%n", qualityReport.get("Average Score"));
    }
    public void addComments(List<String> newComments) {
        comments.addAll(newComments);
    }
}