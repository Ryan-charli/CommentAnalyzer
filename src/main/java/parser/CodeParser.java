package parser;

import analysis.CommentLocation;
import java.io.*;
import java.util.*;

public class CodeParser {
    private final String language;
    private final String singleLineCommentSymbol;
    private final String multiLineCommentStartSymbol;
    private final String multiLineCommentEndSymbol;

    public CodeParser(String language) {
        this.language = language.toLowerCase();
        String[] symbols = LanguageConfig.getCommentSymbols(language);
        this.singleLineCommentSymbol = symbols[0];
        this.multiLineCommentStartSymbol = symbols[1];
        this.multiLineCommentEndSymbol = symbols[2];
    }

    public List<CommentLocation> extractCommentsWithLocations(File file) throws IOException {
        List<CommentLocation> comments = new ArrayList<>();
        StringBuilder multiLineComment = new StringBuilder();
        boolean inMultiLineComment = false;
        int startLine = 0;
        int currentLine = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                currentLine++;
                String trimmedLine = line.trim();
                
                if (inMultiLineComment) {
                    multiLineComment.append(trimmedLine).append("\n");
                    
                    if (trimmedLine.contains(multiLineCommentEndSymbol)) {
                        inMultiLineComment = false;
                        comments.add(new CommentLocation(
                            file.getName(),
                            startLine,
                            multiLineComment.toString().trim()
                        ));
                        multiLineComment = new StringBuilder();
                    }
                    continue;
                }

                if (trimmedLine.startsWith(multiLineCommentStartSymbol)) {
                    inMultiLineComment = true;
                    startLine = currentLine;
                    multiLineComment.append(trimmedLine).append("\n");
                } else if (trimmedLine.startsWith(singleLineCommentSymbol)) {
                    comments.add(new CommentLocation(
                        file.getName(),
                        currentLine,
                        trimmedLine
                    ));
                }
            }
        }
        return comments;
    }

    // Old method
    @Deprecated
    public List<String> extractComments(File file) throws IOException {
        List<String> comments = new ArrayList<>();
        List<CommentLocation> locatedComments = extractCommentsWithLocations(file);
        for (CommentLocation comment : locatedComments) {
            comments.add(comment.getContent());
        }
        return comments;
    }
}