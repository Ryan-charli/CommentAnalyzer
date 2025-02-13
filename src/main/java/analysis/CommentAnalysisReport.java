package analysis;

import java.util.*;

public class CommentAnalysisReport {
    private final Map<CommentTypeAnalyzer.CommentType, List<CommentDetail>> commentsByType;
    private final Map<String, Double> aiScores;
    
    public CommentAnalysisReport() {
        this.commentsByType = new EnumMap<>(CommentTypeAnalyzer.CommentType.class);
        this.aiScores = new HashMap<>();
    }
    
    public void addComment(CommentTypeAnalyzer.CommentType type, CommentDetail detail) {
        commentsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(detail);
    }
    
    public void addAIScore(String commentId, double score) {
        aiScores.put(commentId, score);
    }
    
    public Map<String, Object> generateReport() {
        Map<String, Object> report = new HashMap<>();
        Map<String, List<Map<String, Object>>> commentsByCategory = new HashMap<>();
        
        for (var entry : commentsByType.entrySet()) {
            List<Map<String, Object>> comments = new ArrayList<>();
            for (CommentDetail detail : entry.getValue()) {
                Map<String, Object> commentInfo = new HashMap<>();
                commentInfo.put("content", detail.content());
                commentInfo.put("location", detail.location());
                commentInfo.put("baseScore", detail.baseScore());
                commentInfo.put("aiScore", aiScores.getOrDefault(detail.id(), 0.0));
                comments.add(commentInfo);
            }
            commentsByCategory.put(entry.getKey().getDescription(), comments);
        }
        
        report.put("categorizedComments", commentsByCategory);
        return report;
    }
    
    public record CommentDetail(String id, String content, String location, double baseScore) {}
}