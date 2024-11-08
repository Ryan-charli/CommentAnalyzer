package parser;

import java.util.HashMap;
import java.util.Map;

// Programming Language Comment Symbols
public class LanguageConfig {
    private static final Map<String, String[]> commentSymbols = new HashMap<>();

    static {
        commentSymbols.put("java", new String[]{"//", "/*", "*/"});
        commentSymbols.put("c", new String[]{"//", "/*", "*/"});
        commentSymbols.put("python", new String[]{"#", "\"\"\"", "\"\"\""});
    }

    // Get comment symbols according to programming language
    public static String[] getCommentSymbols(String language) {
        return commentSymbols.getOrDefault(language, new String[0]);
    }
}
