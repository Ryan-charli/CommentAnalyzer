package analysis;

import java.util.regex.Pattern;

public class CodeQualityAnalyzer {
    private final OllamaClient ollamaClient;
    private final CommentTypeAnalyzer typeAnalyzer;
    private final boolean useAI;
    private String prevCodeLine = "";
    private boolean isBeforeClass = true;
    private boolean isAfterClassBeforeFields = false;
    private boolean isAfterFieldsBeforeMethods = false;

    public CodeQualityAnalyzer(boolean useAI) {
        this.useAI = useAI;
        this.ollamaClient = new OllamaClient("http://localhost:11434");
        this.typeAnalyzer = new CommentTypeAnalyzer();
    }

    public QualityAnalysisResult analyzeCommentQuality(String comment, String nextCodeLine, boolean isFirst) {
        if (comment == null || comment.trim().isEmpty()) {
            return new QualityAnalysisResult(0.0, "Empty comment");
        }

        updateContextState(nextCodeLine);
        
        CommentTypeAnalyzer.CommentAnalysisResult typeResult = typeAnalyzer.analyzeCommentType(
            comment, prevCodeLine, nextCodeLine, isFirst,
            isBeforeClass, isAfterClassBeforeFields, isAfterFieldsBeforeMethods
        );

        double baseScore = calculateBaseScore(comment, typeResult);
        
        if (useAI) {
            try {
                double aiScore = getAIScore(comment, nextCodeLine, typeResult.getType());
                return new QualityAnalysisResult(
                    (baseScore * 0.6 + aiScore * 0.4) * 5.0,
                    String.format("Type: %s, %s, Base: %.2f, AI: %.2f", 
                        typeResult.getType().getDescription(),
                        typeResult.getReason(),
                        baseScore * 5.0,
                        aiScore * 5.0)
                );
            } catch (Exception e) {
                return new QualityAnalysisResult(baseScore * 5.0,
                    String.format("Type: %s, %s, Score based on rules", 
                        typeResult.getType().getDescription(),
                        typeResult.getReason()));
            }
        }

        return new QualityAnalysisResult(baseScore * 5.0,
            String.format("Type: %s, %s", 
                typeResult.getType().getDescription(),
                typeResult.getReason()));
    }

    private void updateContextState(String currentLine) {
        if (currentLine.contains("class ") || currentLine.contains("interface ")) {
            isBeforeClass = false;
            isAfterClassBeforeFields = true;
            isAfterFieldsBeforeMethods = false;
        } else if (currentLine.matches(".*\\s+\\w+\\s*=.*|.*private.*|.*public.*|.*protected.*")) {
            isAfterClassBeforeFields = false;
        } else if (currentLine.contains("(") && currentLine.contains(")")) {
            isAfterFieldsBeforeMethods = true;
        }
        prevCodeLine = currentLine;
    }

    private double calculateBaseScore(String comment, CommentTypeAnalyzer.CommentAnalysisResult typeResult) {
        double typeBaseScore = typeResult.getBaseScore();
        double coherenceScore = evaluateCoherenceAndCompleteness(comment, typeResult.getType()) * 0.4;
        double consistencyScore = evaluateConsistency(comment) * 0.3;
        double languageScore = evaluateLanguageQuality(comment) * 0.3;

        return (typeBaseScore * 0.4 + coherenceScore + consistencyScore + languageScore) / 2.0;
    }

    private double getAIScore(String comment, String codeContext, CommentTypeAnalyzer.CommentType type) {
        try {
            String response = ollamaClient.generateAnalysis(comment);
            try {
                return Double.parseDouble(response.trim()) / 5.0;
            } catch (NumberFormatException e) {
                return 0.5;
            }
        } catch (Exception e) {
            return 0.5;
        }
    }

    private double evaluateCoherenceAndCompleteness(String comment, CommentTypeAnalyzer.CommentType type) {
        double score = 0.0;

        switch (type) {
            case METHOD_COMMENT -> {
                if (comment.contains("@param")) score += 0.3;
                if (comment.contains("@return")) score += 0.3;
                if (comment.contains("@throws")) score += 0.2;
                if (comment.length() > 10) score += 0.2;
            }
            case CLASS_COMMENT -> {
                if (comment.contains("@author")) score += 0.2;
                if (comment.contains("@version")) score += 0.2;
                if (comment.length() > 20) score += 0.3;
                if (comment.contains("class") || comment.contains("interface")) score += 0.3;
            }
            case FILE_COMMENT -> {
                if (comment.contains("Licensed")) score += 0.4;
                if (comment.contains("copyright")) score += 0.3;
                if (comment.length() > 30) score += 0.3;
            }
            default -> {
                if (comment.length() > 5) score += 0.5;
                if (containsTechnicalTerms(comment)) score += 0.5;
            }
        }
        return Math.min(score, 1.0);
    }

    private double evaluateConsistency(String comment) {
        double score = 0.0;
        if (comment.startsWith("/*") || comment.startsWith("/**")) score += 0.3;
        if (comment.endsWith("*/")) score += 0.3;
        if (!comment.contains("TODO") && !comment.contains("FIXME")) score += 0.4;
        return score;
    }

    private double evaluateLanguageQuality(String comment) {
        double score = 0.0;
        if (comment.matches("[A-Z].*")) score += 0.3;
        if (!comment.contains("!!!!") && !comment.contains("????")) score += 0.3;
        if (comment.length() < 200) score += 0.4;
        return score;
    }

    private boolean containsTechnicalTerms(String comment) {
        return comment.matches("(?i).*(method|class|function|return|parameter|variable|object|interface).*");
    }

    public static class QualityAnalysisResult {
        private final double score;
        private final String details;

        public QualityAnalysisResult(double score, String details) {
            this.score = score;
            this.details = details;
        }

        public double getScore() { return score; }
        public String getDetails() { return details; }
    }
}