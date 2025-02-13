package analysis;

class AIAnalyzer {
    private final OllamaClient ollamaClient;

    AIAnalyzer() {
        this.ollamaClient = new OllamaClient("http://localhost:11434");
    }

    AIAnalysisResult analyzeComment(CommentLocation comment) {
        try {
            String aiResponse = ollamaClient.generateAnalysis(comment.getContent());
            double relevanceScore = calculateRelevanceScore(aiResponse);
            return new AIAnalysisResult(aiResponse, relevanceScore);
        } catch (Exception e) {
            return new AIAnalysisResult("AI analysis failed: " + e.getMessage(), 0.0);
        }
    }

    private double calculateRelevanceScore(String aiResponse) {
        // Simple scoring based on response length and content
        double score = 0.5;
        if (aiResponse.length() > 50) score += 0.2;
        if (aiResponse.contains("purpose") || aiResponse.contains("functionality")) score += 0.1;
        if (aiResponse.contains("improvement") || aiResponse.contains("suggestion")) score += 0.1;
        if (!aiResponse.contains("error") && !aiResponse.contains("failed")) score += 0.1;
        return Math.min(score, 1.0);
    }
}