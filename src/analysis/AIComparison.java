package analysis;

import java.util.List;

public class AIComparison {
    public String compareComments(List<String> existingComments, String aiGeneratedComment) {
        StringBuilder differences = new StringBuilder();
        differences.append("AI Generated Comment:\n").append(aiGeneratedComment).append("\n\nDifferences:\n");

        for (String comment : existingComments) {
            if (!aiGeneratedComment.contains(comment)) {
                differences.append("Existing comment missing in AI generated: ").append(comment).append("\n");
            }
        }
        return differences.toString();
    }
}
