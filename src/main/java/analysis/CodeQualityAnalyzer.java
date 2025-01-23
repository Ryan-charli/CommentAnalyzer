package analysis;

public class CodeQualityAnalyzer {
    private static final int MAX_COMMENT_LENGTH = 200;
    private static final int MIN_COMMENT_LENGTH = 5;

    public double analyzeCommentQuality(String comment) {
        // Skip if it's not actually a comment
        if (isCodeLine(comment) || comment.trim().isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        String cleanComment = cleanCommentSymbols(comment);
        
        // Content score (50%)
        if (hasSubstantiveContent(cleanComment)) score += 0.5;
        
        // Format score (30%)
        if (hasProperFormat(cleanComment)) score += 0.3;
        
        // Best practices (20%)
        if (!containsCodeSmells(cleanComment)) score += 0.2;
        
        return score;
    }

    private String cleanCommentSymbols(String comment) {
        return comment.replaceAll("^\\s*[/\\*]+\\s*", "")
                     .replaceAll("\\s*\\*+/$", "")
                     .trim();
    }

    private boolean isCodeLine(String line) {
        // Check for common code patterns
        return line.matches(".*(\\{|\\}|\\=|\\;|\\(.*\\)|catch|try|if|else|return).*");
    }

    private boolean hasSubstantiveContent(String comment) {
        int wordCount = comment.split("\\s+").length;
        return wordCount >= 3 && 
               comment.length() >= MIN_COMMENT_LENGTH &&
               comment.length() <= MAX_COMMENT_LENGTH;
    }

    private boolean hasProperFormat(String comment) {
        return comment.matches("[A-Z].*[.!?]\\s*$") || // Starts with capital, ends with punctuation
               comment.matches("@\\w+.*"); // Javadoc tag
    }

    private boolean containsCodeSmells(String comment) {
        String[] smells = {
            "TODO", "FIXME", "XXX",
            "obvious", "clearly", "simply",
            "\\b[A-Z]{4,}\\b"
        };
        
        for (String smell : smells) {
            if (comment.matches(".*" + smell + ".*")) return true;
        }
        return false;
    }
}