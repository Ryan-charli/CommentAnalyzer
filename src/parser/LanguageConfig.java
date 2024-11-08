package parser;

import java.util.HashMap;
import java.util.Map;

// LanguageConfig: 存储不同编程语言的注释符号
public class LanguageConfig {
    private static final Map<String, String[]> commentSymbols = new HashMap<>();

    static {
        commentSymbols.put("java", new String[]{"//", "/*", "*/"});
        commentSymbols.put("c", new String[]{"//", "/*", "*/"});
        commentSymbols.put("python", new String[]{"#", "\"\"\"", "\"\"\""});
    }

    // 根据语言名称获取注释符号
    public static String[] getCommentSymbols(String language) {
        return commentSymbols.getOrDefault(language, new String[0]);
    }
}
