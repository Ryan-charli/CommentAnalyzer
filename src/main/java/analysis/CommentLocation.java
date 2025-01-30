package analysis;

public class CommentLocation {
    private final String fileName;
    private final int lineNumber;
    private final String content;

    public CommentLocation(String fileName, int lineNumber, String content) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.content = content;
    }

    public String getFileName() { return fileName; }
    public int getLineNumber() { return lineNumber; }
    public String getContent() { return content; }

    @Override
    public String toString() {
        return String.format("[%s:line %d] %s", fileName, lineNumber, content);
    }
}