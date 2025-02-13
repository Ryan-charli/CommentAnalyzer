package analysis;

public class CommentDetail {
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