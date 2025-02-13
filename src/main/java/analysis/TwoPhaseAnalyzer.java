package analysis;

import java.util.*;

public class TwoPhaseAnalyzer {
    public final CommentTypeAnalyzer typeAnalyzer;
    public final AIAnalyzer aiAnalyzer;

    public TwoPhaseAnalyzer() {
        this.typeAnalyzer = new CommentTypeAnalyzer();
        this.aiAnalyzer = new AIAnalyzer();
    }

    public Map<String, Object> analyzeBasic(List<CommentLocation> comments) {
        Map<String, Object> report = new HashMap<>();
        Map<CommentTypeAnalyzer.CommentType, List<CommentDetail>> categorizedComments = 
            new EnumMap<>(CommentTypeAnalyzer.CommentType.class);

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

    public static class CommentDetail {
        public final String fileName;
        public final int lineNumber;
        public final String content;
        public final double basicScore;

        public CommentDetail(String fileName, int lineNumber, String content, double basicScore) {
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.content = content;
            this.basicScore = basicScore;
        }
    }

    private double calculateBasicScore(String content, CommentTypeAnalyzer.CommentType type) {
        double score = 0.5;
        
        switch (type) {
            case FILE_COMMENT -> score += 0.2;
            case CLASS_COMMENT -> score += 0.15;
            case METHOD_COMMENT -> score += 0.1;
            default -> score += 0.05;
        }

        if (content.length() > 10) score += 0.1;
        if (content.length() > 50) score += 0.1;
        if (content.matches("^\\s*[A-Z].*")) score += 0.05;
        if (!content.contains("TODO") && !content.contains("FIXME")) score += 0.05;

        return Math.min(score, 1.0);
    }
}