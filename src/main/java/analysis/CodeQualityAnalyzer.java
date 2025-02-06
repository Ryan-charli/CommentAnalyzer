package analysis;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

public class CodeQualityAnalyzer {
    private final HttpClient client;
    private final String ollamaEndpoint;
    private final CommentTypeAnalyzer typeAnalyzer;
    private final boolean useAI;
    private String prevCodeLine = "";
    private boolean isBeforeClass = true;
    private boolean isAfterClassBeforeFields = false;
    private boolean isAfterFieldsBeforeMethods = false;

    public CodeQualityAnalyzer(boolean useAI) {
        this.useAI = useAI;
        this.client = HttpClient.newHttpClient();
        this.ollamaEndpoint = "http://localhost:11434/api/generate";
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

    private double getAIScore(String comment, String codeContext, CommentTypeAnalyzer.CommentType type) throws Exception {
        String typeSpecificPrompt = switch (type) {
            case METHOD_COMMENT -> """
                Analyze this method comment (score 1-5):
                1. Does it describe the method's purpose?
                2. Are parameters documented?
                3. Is return value explained?
                4. Are exceptions documented?
                5. Is the explanation clear and accurate?
                """;
            case CLASS_COMMENT -> """
                Analyze this class comment (score 1-5):
                1. Does it explain class purpose?
                2. Are class responsibilities clear?
                3. Is inheritance/implementation explained?
                4. Are important class features documented?
                5. Is it well-structured and complete?
                """;
            case FILE_COMMENT -> """
                Analyze this file-level comment (score 1-5):
                1. Does it describe file purpose?
                2. Is copyright/license info included?
                3. Are package details explained?
                4. Is version/author information present?
                5. Is it properly formatted?
                """;
            default -> """
                Analyze this code comment (score 1-5):
                1. Is it clear and understandable?
                2. Does it add valuable information?
                3. Is it accurate and up-to-date?
                4. Does it follow good practices?
                5. Is it properly formatted?
                """;
        };

        String prompt = String.format("""
            %s
            
            Comment:
            %s
            
            Related Code:
            %s
            
            Respond with only a number 1-5.
            """, typeSpecificPrompt, comment, codeContext);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ollamaEndpoint))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(new JSONObject()
                .put("model", "deepseek-r1:7b")
                .put("prompt", prompt)
                .put("temperature", 0.1)
                .put("stream", false)
                .toString()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JSONObject jsonResponse = new JSONObject(response.body());
        String responseText = jsonResponse.getString("response").trim();
        
        try {
            return Double.parseDouble(responseText) / 5.0;
        } catch (NumberFormatException e) {
            return 0.5;
        }
    }

    // [Previous evaluation methods remain the same]

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
}