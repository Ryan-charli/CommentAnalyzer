package analysis;

import java.util.regex.Pattern;

public class CommentTypeAnalyzer {
    public enum CommentType {
        FILE_COMMENT("File level comment"),
        CLASS_COMMENT("Class level comment"),
        METHOD_COMMENT("Method level comment"),
        FIELD_COMMENT("Field level comment"),
        INLINE_COMMENT("Inline comment");

        private final String description;
        CommentType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private static final Pattern LICENSE_PATTERN = Pattern.compile(
        ".*Apache.*License.*|.*Copyright.*|.*Licensed to.*", 
        Pattern.MULTILINE | Pattern.CASE_INSENSITIVE
    );

    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "\\s*(public|private|protected)?\\s*(static)?\\s*[\\w<>\\[\\]]+\\s+\\w+\\s*\\([^)]*\\)\\s*\\{?"
    );
    
    private static final Pattern CLASS_PATTERN = Pattern.compile(
        "\\s*(public|private|protected)?\\s*(static)?\\s*(class|interface|enum)\\s+\\w+"
    );
    
    private static final Pattern FIELD_PATTERN = Pattern.compile(
        "\\s*(public|private|protected)?\\s*(static)?\\s*(final)?\\s*[\\w<>\\[\\]]+\\s+\\w+\\s*=?"
    );

    public CommentAnalysisResult analyzeCommentType(
            String comment, 
            String prevCodeLine, 
            String nextCodeLine, 
            boolean isFirstComment,
            boolean isBeforeClass,
            boolean isAfterClassBeforeFields,
            boolean isAfterFieldsBeforeMethods) {

        CommentType type;
        double baseScore = 0.5;
        String reason = "";

        // Rule 1: If comment is before class declaration
        if (isBeforeClass || isFirstComment || isLicenseComment(comment)) {
            type = CommentType.FILE_COMMENT;
            baseScore = 0.8;
            reason = "File level comment (before class)";
        }
        // Rule 2: If comment is between class declaration and fields
        else if (isAfterClassBeforeFields) {
            type = CommentType.FILE_COMMENT;
            baseScore = 0.7;
            reason = "File level comment (after class declaration)";
        }
        // Rule 3: If comment describes a method
        else if (METHOD_PATTERN.matcher(nextCodeLine).matches() || 
                 containsMethodTags(comment)) {
            type = CommentType.METHOD_COMMENT;
            baseScore = 0.6;
            reason = "Method comment";
        }
        // Rule 4: If comment is between fields and methods
        else if (isAfterFieldsBeforeMethods) {
            type = CommentType.METHOD_COMMENT;
            baseScore = 0.5;
            reason = "Method comment (between fields and methods)";
        }
        // Additional cases
        else if (CLASS_PATTERN.matcher(nextCodeLine).matches()) {
            type = CommentType.CLASS_COMMENT;
            baseScore = 0.6;
            reason = "Class comment";
        }
        else if (FIELD_PATTERN.matcher(nextCodeLine).matches()) {
            type = CommentType.FIELD_COMMENT;
            baseScore = 0.5;
            reason = "Field comment";
        }
        else {
            type = CommentType.INLINE_COMMENT;
            baseScore = 0.4;
            reason = "Inline comment";
        }

        return new CommentAnalysisResult(type, baseScore, reason);
    }

    private boolean isLicenseComment(String comment) {
        return LICENSE_PATTERN.matcher(comment).find();
    }

    private boolean containsMethodTags(String comment) {
        return comment.contains("@param") ||
               comment.contains("@return") ||
               comment.contains("@throws") ||
               comment.contains("@exception");
    }

    public static class CommentAnalysisResult {
        private final CommentType type;
        private final double baseScore;
        private final String reason;

        public CommentAnalysisResult(CommentType type, double baseScore, String reason) {
            this.type = type;
            this.baseScore = baseScore;
            this.reason = reason;
        }

        public CommentType getType() { return type; }
        public double getBaseScore() { return baseScore; }
        public String getReason() { return reason; }
    }
}