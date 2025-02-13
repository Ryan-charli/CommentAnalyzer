package parser;

import java.util.*;

public class LanguageConfig {
    private static final Map<String, String[]> commentSymbols = new HashMap<>();
    private static final Map<String, Set<String>> fileExtensions = new HashMap<>();
    private static final Map<String, String> shebangPatterns = new HashMap<>();

    static {
        commentSymbols.put("java", new String[]{"//", "/*", "*/"});
        commentSymbols.put("c", new String[]{"//", "/*", "*/"});
        commentSymbols.put("cpp", new String[]{"//", "/*", "*/"});
        commentSymbols.put("python", new String[]{"#", "\"\"\"", "\"\"\""});
        commentSymbols.put("javascript", new String[]{"//", "/*", "*/"});
        commentSymbols.put("ruby", new String[]{"#", "=begin", "=end"});
        commentSymbols.put("php", new String[]{"//", "/*", "*/"});
        
        addExtensions("java", ".java");
        addExtensions("c", ".c", ".h");
        addExtensions("cpp", ".cpp", ".hpp", ".cc", ".hh");
        addExtensions("python", ".py", ".pyw");
        addExtensions("javascript", ".js", ".jsx");
        addExtensions("ruby", ".rb");
        addExtensions("php", ".php");

        shebangPatterns.put("python", "^#!.*python");
        shebangPatterns.put("ruby", "^#!.*ruby");
        shebangPatterns.put("php", "^#!.*php");
    }

    private static void addExtensions(String language, String... exts) {
        fileExtensions.put(language, new HashSet<>(Arrays.asList(exts)));
    }

    public static boolean isSupportedExtension(String extension) {
        if (extension == null) return false;
        String ext = extension.toLowerCase();
        return fileExtensions.values().stream()
            .anyMatch(extSet -> extSet.contains(ext));
    }

    public static String detectLanguage(String fileName, String firstLine) {
        String ext = fileName.toLowerCase();
        int dotIndex = ext.lastIndexOf('.');
        if (dotIndex > 0) {
            ext = ext.substring(dotIndex);
            for (Map.Entry<String, Set<String>> entry : fileExtensions.entrySet()) {
                if (entry.getValue().contains(ext)) {
                    return entry.getKey();
                }
            }
        }

        if (firstLine != null && !firstLine.isEmpty()) {
            for (Map.Entry<String, String> entry : shebangPatterns.entrySet()) {
                if (firstLine.matches(entry.getValue())) {
                    return entry.getKey();
                }
            }
        }

        return null;
    }

    public static String[] getCommentSymbols(String language) {
        return commentSymbols.getOrDefault(language, new String[]{"//", "/*", "*/"});
    }

    public static Set<String> getSupportedExtensions(String language) {
        return fileExtensions.getOrDefault(language, new HashSet<>());
    }

    public static boolean isLanguageSupported(String language) {
        return commentSymbols.containsKey(language.toLowerCase());
    }
}