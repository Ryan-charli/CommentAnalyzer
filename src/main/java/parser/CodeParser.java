package parser;

import java.io.*;
import java.util.*;

public class CodeParser {
    private String language;
    private String singleLineCommentSymbol;
    private String multiLineCommentStartSymbol;
    private String multiLineCommentEndSymbol;

    public CodeParser(String language) {
        this.language = language.toLowerCase();
        setCommentSymbols();
    }

    private void setCommentSymbols() {
        switch (language) {
            case "java":
                singleLineCommentSymbol = "//";
                multiLineCommentStartSymbol = "/*";
                multiLineCommentEndSymbol = "*/";
                break;
            default:
                singleLineCommentSymbol = "//";
                multiLineCommentStartSymbol = "/*";
                multiLineCommentEndSymbol = "*/";
        }
    }

    public List<String> extractComments(File file) throws IOException {
        List<String> comments = new ArrayList<>();
        StringBuilder multiLineComment = new StringBuilder();
        boolean inMultiLineComment = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                
                if (inMultiLineComment) {
                    if (trimmedLine.contains(multiLineCommentEndSymbol)) {
                        multiLineComment.append(trimmedLine.substring(0, 
                            trimmedLine.indexOf(multiLineCommentEndSymbol) + 2));
                        comments.add(multiLineComment.toString());
                        multiLineComment = new StringBuilder();
                        inMultiLineComment = false;
                    } else {
                        multiLineComment.append(trimmedLine).append("\n");
                    }
                    continue;
                }

                if (trimmedLine.startsWith(multiLineCommentStartSymbol)) {
                    inMultiLineComment = true;
                    multiLineComment.append(trimmedLine).append("\n");
                } else if (trimmedLine.startsWith(singleLineCommentSymbol)) {
                    comments.add(trimmedLine);
                }
            }
        }
        return comments;
    }
}