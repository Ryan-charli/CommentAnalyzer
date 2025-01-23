package analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import java.io.File;
import java.util.*;

public class CodeAnalyzer {
    private final Map<String, MethodMetrics> methodMetrics = new HashMap<>();

    static class MethodMetrics {
        String methodName;
        int complexity;
        int lineCount;
        List<String> comments;
        List<String> codeSmells;

        public MethodMetrics(String name) {
            this.methodName = name;
            this.comments = new ArrayList<>();
            this.codeSmells = new ArrayList<>();
        }
    }

    public Map<String, MethodMetrics> analyzeFile(File file) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            
            // Analyze methods
            cu.findAll(MethodDeclaration.class).forEach(method -> {
                String methodName = method.getNameAsString();
                MethodMetrics metrics = new MethodMetrics(methodName);
                
                // Calculate complexity
                metrics.complexity = calculateComplexity(method);
                metrics.lineCount = method.getEnd().get().line - method.getBegin().get().line;
                
                // Get associated comments
                method.getAllContainedComments().forEach(comment -> 
                    metrics.comments.add(comment.getContent()));
                
                // Detect code smells
                detectCodeSmells(method, metrics);
                analyzeCodeCommentRelation(method, metrics);

                methodMetrics.put(methodName, metrics);
            });
            
        } catch (Exception e) {
            System.err.println("Error analyzing file: " + file.getName());
        }
        return methodMetrics;
    }

    private int calculateComplexity(MethodDeclaration method) {
        int complexity = 1;
        // Count decision points (if, while, for, case, catch)
        complexity += method.findAll(com.github.javaparser.ast.stmt.IfStmt.class).size();
        complexity += method.findAll(com.github.javaparser.ast.stmt.WhileStmt.class).size();
        complexity += method.findAll(com.github.javaparser.ast.stmt.ForStmt.class).size();
        complexity += method.findAll(com.github.javaparser.ast.stmt.SwitchEntry.class).size();
        complexity += method.findAll(com.github.javaparser.ast.stmt.CatchClause.class).size();
        return complexity;
    }

    private void detectCodeSmells(MethodDeclaration method, MethodMetrics metrics) {
        // Long method smell
        if (metrics.lineCount > 30) {
            metrics.codeSmells.add("Long Method");
        }
        
        // High complexity smell
        if (metrics.complexity > 10) {
            metrics.codeSmells.add("High Cyclomatic Complexity");
        }
        
        // Comment ratio smell
        double commentRatio = metrics.comments.size() / (double) metrics.lineCount;
        if (commentRatio < 0.1) {
            metrics.codeSmells.add("Low Comment Ratio");
        }
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder();
        report.append("Code Analysis Report\n===================\n\n");
        
        report.append("Method Analysis\n--------------\n");
        methodMetrics.forEach((name, metrics) -> {
            report.append("Method: ").append(name).append("\n");
            report.append(String.format("Lines: %d, Complexity: %d, Comments: %d\n", 
                metrics.lineCount, metrics.complexity, metrics.comments.size()));
            if (!metrics.codeSmells.isEmpty()) {
                report.append("Issues Found:\n");
                metrics.codeSmells.forEach(smell -> 
                    report.append("  - ").append(smell).append("\n"));
            }
            report.append("\n");
        });
        
        report.append(generateRelationReport());
        return report.toString();
    }
    private void analyzeCodeCommentRelation(MethodDeclaration method, MethodMetrics metrics) {
        // Analyze comment density vs complexity
        double commentDensity = metrics.comments.size() / (double) metrics.lineCount;
        if (metrics.complexity > 10 && commentDensity < 0.2) {
            metrics.codeSmells.add("Complex method with insufficient comments");
        }
    
        // Analyze comment quality vs method length
        if (metrics.lineCount > 30) {
            boolean hasMethodComment = method.getJavadoc().isPresent();
            if (!hasMethodComment) {
                metrics.codeSmells.add("Long method missing documentation");
            }
        }
    
        // Check parameter documentation
        if (!method.getParameters().isEmpty()) {
            boolean hasParamDocs = method.getJavadoc()
                .map(doc -> doc.getBlockTags().stream()
                    .anyMatch(tag -> tag.getTagName().equals("param")))
                .orElse(false);
            if (!hasParamDocs) {
                metrics.codeSmells.add("Missing parameter documentation");
            }
        }
    }
    
    private String generateRelationReport() {
        StringBuilder report = new StringBuilder();
        report.append("Code-Comment Relation Analysis\n");
        report.append("===========================\n");
        
        int highComplexityMethods = 0;
        int poorlyDocumentedMethods = 0;
        
        for (MethodMetrics metrics : methodMetrics.values()) {
            double commentRatio = metrics.comments.size() / (double) metrics.lineCount;
            if (metrics.complexity > 10 && commentRatio < 0.2) {
                highComplexityMethods++;
            }
            if (metrics.lineCount > 30 && commentRatio < 0.15) {
                poorlyDocumentedMethods++;
            }
        }
        
        report.append(String.format("\nSummary:\n- Complex methods with low comment ratio: %d\n", highComplexityMethods));
        report.append(String.format("- Long methods with poor documentation: %d\n\n", poorlyDocumentedMethods));
        
        return report.toString();
    }
}