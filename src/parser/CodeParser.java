package parser;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class CodeParser {
    private String language;
    private String singleLineCommentSymbol;
    private String multiLineCommentStartSymbol;
    private String multiLineCommentEndSymbol;

    // 构造函数：初始化 CodeParser 并根据语言设置注释标记
    public CodeParser(String language) {
        this.language = language.toLowerCase();
        setCommentSymbols();
    }

    // 根据语言设置注释标记
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
                singleLineCommentSymbol = "//"; // 默认单行注释符号
                multiLineCommentStartSymbol = "/*";
                multiLineCommentEndSymbol = "*/";
                break;
        }
    }

    // 从文件中提取注释
    public List<String> extractComments(File file) throws IOException {
        List<String> comments = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        boolean inMultiLineComment = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // 多行注释处理
            if (inMultiLineComment) {
                comments.add(line);
                if (line.contains(multiLineCommentEndSymbol)) {
                    inMultiLineComment = false;
                }
            }
            // 检查是否为多行注释的开始
            else if (multiLineCommentStartSymbol != null && line.contains(multiLineCommentStartSymbol)) {
                inMultiLineComment = true;
                comments.add(line);
            }
            // 检查是否为单行注释
            else if (line.startsWith(singleLineCommentSymbol)) {
                comments.add(line);
            }
        }

        reader.close();
        return comments;
    }
}
