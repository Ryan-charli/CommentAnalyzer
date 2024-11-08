package analysis;

import parser.CodeParser;
import java.io.*;
import java.util.*;

public class CommentAnalyzer {
    private CodeParser parser;
    private List<String> comments;

    // Initialising
    public CommentAnalyzer(String language) {
        this.parser = new CodeParser(language);
        this.comments = new ArrayList<>();
    }

    // Analysing comments in a single file
    public void analyzeFile(File file) {
        try {
            List<String> fileComments = parser.extractComments(file);
            comments.addAll(fileComments);
            System.out.println("File: " + file.getName() + " has " + fileComments.size() + " comments.");
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
        }
    }

    // Analysing comments across the catalogue
    public void analyzeDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isFile()) {
                    analyzeFile(file);
                }
            }
        }
    }

    // return
    public int getTotalCommentCount() {
        return comments.size();
    }

    // Returns annotation details
    public void displayComments() {
        System.out.println("Total Comments: " + getTotalCommentCount());
        for (String comment : comments) {
            System.out.println(comment);
        }
    }

    // Annotation quality analysis (length-based)
    public double analyzeCommentQuality() {
        double totalLength = 0;
        for (String comment : comments) {
            totalLength += comment.length();
        }
        return totalLength / (comments.size() + 1); // Preventing division by zero
    }
}
