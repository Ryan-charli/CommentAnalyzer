package parser;

import analysis.CommentLocation;
import java.io.*;
import java.util.*;

public class CodeParser {
    private String language;
    private String singleLineCommentSymbol;
    private String multiLineCommentStartSymbol;
    private String multiLineCommentEndSymbol;

    public CodeParser() {
    }

    public CodeParser(String language) {
        setLanguage(language);
    }

    public void setLanguage(String language) {
        this.language = language.toLowerCase();
        String[] symbols = LanguageConfig.getCommentSymbols(language);
        this.singleLineCommentSymbol = symbols[0];
        this.multiLineCommentStartSymbol = symbols[1];
        this.multiLineCommentEndSymbol = symbols[2];
    }

    public List<CommentLocation> extractCommentsWithLocations(File file) throws IOException {
        List<CommentLocation> comments = new ArrayList<>();
        
        try (BufferedReader detector = new BufferedReader(new FileReader(file))) {
            String firstLine = detector.readLine();
            if (language == null) {
                String detectedLang = LanguageConfig.detectLanguage(file.getName(), firstLine);
                if (detectedLang == null) {
                    throw new IllegalArgumentException("Unsupported file type: " + file.getName());
                }
                setLanguage(detectedLang);
            }
        }

        StringBuilder multiLineComment = new StringBuilder();
        boolean inMultiLineComment = false;
        boolean inPythonDocString = false;
        int startLine = 0;
        int currentLine = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                currentLine++;
                String trimmedLine = line.trim();

                if (language.equals("python")) {
                    // Handle Python docstrings
                    if (!inMultiLineComment && trimmedLine.startsWith("\"\"\"")) {
                        inPythonDocString = !inPythonDocString;
                        if (inPythonDocString) {
                            startLine = currentLine;
                            multiLineComment = new StringBuilder(trimmedLine);
                        } else {
                            multiLineComment.append("\n").append(trimmedLine);
                            comments.add(new CommentLocation(
                                file.getName(),
                                startLine,
                                multiLineComment.toString().trim()
                            ));
                            multiLineComment = new StringBuilder();
                        }
                        continue;
                    }

                    if (inPythonDocString) {
                        multiLineComment.append("\n").append(line);
                        if (trimmedLine.endsWith("\"\"\"")) {
                            inPythonDocString = false;
                            comments.add(new CommentLocation(
                                file.getName(),
                                startLine,
                                multiLineComment.toString().trim()
                            ));
                            multiLineComment = new StringBuilder();
                        }
                        continue;
                    }
                }

                if (inMultiLineComment) {
                    multiLineComment.append("\n").append(line);
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

                if (trimmedLine.contains(multiLineCommentStartSymbol) && 
                    !isInString(line, line.indexOf(multiLineCommentStartSymbol))) {
                    inMultiLineComment = true;
                    startLine = currentLine;
                    multiLineComment.append(line);
                    
                    // Check if multi-line comment ends on the same line
                    if (trimmedLine.contains(multiLineCommentEndSymbol)) {
                        inMultiLineComment = false;
                        comments.add(new CommentLocation(
                            file.getName(),
                            startLine,
                            multiLineComment.toString().trim()
                        ));
                        multiLineComment = new StringBuilder();
                    }
                }
                // Handle single-line comments
                else if (trimmedLine.startsWith(singleLineCommentSymbol)) {
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

    private boolean isInString(String line, int index) {
        boolean inString = false;
        boolean escaped = false;
        
        for (int i = 0; i < index; i++) {
            char c = line.charAt(i);
            if (c == '\\') {
                escaped = !escaped;
            } else if ((c == '"' || c == '\'') && !escaped) {
                inString = !inString;
                escaped = false;
            } else {
                escaped = false;
            }
        }
        
        return inString;
    }
}