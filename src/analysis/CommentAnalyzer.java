package analysis;

import parser.CodeParser;
import java.io.*;
import java.util.*;

public class CommentAnalyzer {
    private CodeParser parser;
    private List<String> comments;

    // 构造函数，初始化解析器
    public CommentAnalyzer(String language) {
        this.parser = new CodeParser(language);
        this.comments = new ArrayList<>();
    }

    // 分析单个文件中的注释
    public void analyzeFile(File file) {
        try {
            List<String> fileComments = parser.extractComments(file);
            comments.addAll(fileComments);
            System.out.println("File: " + file.getName() + " has " + fileComments.size() + " comments.");
        } catch (IOException e) {
            System.err.println("Error reading file: " + file.getName());
        }
    }

    // 分析整个目录中的注释
    public void analyzeDirectory(File directory) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isFile()) {
                    analyzeFile(file);
                }
            }
        }
    }

    // 返回注释数量
    public int getTotalCommentCount() {
        return comments.size();
    }

    // 返回注释的详细信息
    public void displayComments() {
        System.out.println("Total Comments: " + getTotalCommentCount());
        for (String comment : comments) {
            System.out.println(comment);
        }
    }

    // 简单的注释质量分析（基于长度）
    public double analyzeCommentQuality() {
        double totalLength = 0;
        for (String comment : comments) {
            totalLength += comment.length();
        }
        return totalLength / (comments.size() + 1); // 防止除零
    }
}
