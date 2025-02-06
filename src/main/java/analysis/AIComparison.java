package analysis;

import java.util.List;

public class AIComparison {
    public String compareComments(List<String> existingComments, String aiGeneratedComment) {
        StringBuilder differences = new StringBuilder();
        differences.append("AI Generated Comment:\n")
                  .append(aiGeneratedComment)
                  .append("\n\nAnalysis:\n");

        for (String comment : existingComments) {
            differences.append("Original: ").append(comment).append("\n");
            if (!aiGeneratedComment.contains(comment)) {
                differences.append("Missing Information: ").append(comment).append("\n");
            }
            
            double similarity = calculateSimilarity(comment, aiGeneratedComment);
            differences.append(String.format("Similarity Score: %.2f%%\n", similarity * 100));
        }
        
        return differences.toString();
    }

    private double calculateSimilarity(String str1, String str2) {
        String[] words1 = str1.toLowerCase().split("\\W+");
        String[] words2 = str2.toLowerCase().split("\\W+");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2)) {
                    commonWords++;
                    break;
                }
            }
        }
        
        return (double) commonWords / Math.max(words1.length, words2.length);
    }
}