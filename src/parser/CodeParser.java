package parser;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class CodeParser {
    private String language;
    private String singleLineCommentSymbol;
    private String multiLineCommentStartSymbol;
    private String multiLineCommentEndSymbol;

    // initialise
    public CodeParser(String language) {
        this.language = language.toLowerCase();
        setCommentSymbols();
    }

    // Annotation tags according to language
    private void setCommentSymbols() {
        switch (language) {
            case "java":
            case "c":
            case "cpp":
                singleLineCommentSymbol = "//";
                multiLineCommentStartSymbol = "/*";
                multiLineCommentEndSymbol = "*/";
                break;
            case "python":
                singleLineCommentSymbol = "#";
                multiLineCommentStartSymbol = null;
                multiLineCommentEndSymbol = null;
                break;
            default:
                singleLineCommentSymbol = "//"; // Default single-line comment symbols
                multiLineCommentStartSymbol = "/*";
                multiLineCommentEndSymbol = "*/";
                break;
        }
    }

    // Extracting comments from files
    public List<String> extractComments(File file) throws IOException {
        List<String> comments = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        boolean inMultiLineComment = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Multi-line comment handling
            if (inMultiLineComment) {
                comments.add(line);
                if (line.contains(multiLineCommentEndSymbol)) {
                    inMultiLineComment = false;
                }
            }
            // Check for multi-line comments
            else if (multiLineCommentStartSymbol != null && line.contains(multiLineCommentStartSymbol)) {
                inMultiLineComment = true;
                comments.add(line);
            }
            // Checking for single line comments
            else if (line.startsWith(singleLineCommentSymbol)) {
                comments.add(line);
            }
        }

        reader.close();
        return comments;
    }
}
