package analysis;

class AIAnalysisResult {
    final String analysis;
    final double relevanceScore;

    AIAnalysisResult(String analysis, double relevanceScore) {
        this.analysis = analysis;
        this.relevanceScore = relevanceScore;
    }
}