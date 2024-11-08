package parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

// CommentExtractor: 负责从文件中提取注释，支持目录递归和多语言
public class CommentExtractor {
    // 定义一个线程池，使用4个线程同时处理多个文件
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    // 根据文件扩展名检测编程语言，返回语言标识符字符串
    private String detectLanguage(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".java")) return "java";
        if (fileName.endsWith(".c") || fileName.endsWith(".h")) return "c";
        if (fileName.endsWith(".py")) return "python";
        return "unknown"; // 不支持的文件类型
    }

    // 从文件中提取注释，返回注释列表
    public List<String> extractComments(File file) throws IOException {
        String language = detectLanguage(file);  // 自动检测语言
        if ("unknown".equals(language)) {
            System.out.println("不支持的文件类型：" + file.getName());
            return new ArrayList<>();
        }

        // 根据语言获取注释符号
        String[] symbols = LanguageConfig.getCommentSymbols(language);
        List<String> comments = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inMultiLineComment = false;

            // 循环读取每一行，检查是否为注释
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (inMultiLineComment) {
                    comments.add(line);
                    if (line.contains(symbols[2])) inMultiLineComment = false;
                } else if (line.startsWith(symbols[0])) {
                    comments.add(line);
                } else if (line.contains(symbols[1])) {
                    comments.add(line);
                    inMultiLineComment = true;
                }
            }
        }
        return comments;
    }

    // 递归地从目录中提取所有文件的注释
    public List<String> extractCommentsFromDirectory(File dir) throws IOException, InterruptedException {
        List<String> comments = new ArrayList<>();
        List<Future<List<String>>> futures = new ArrayList<>();

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                comments.addAll(extractCommentsFromDirectory(file));  // 递归处理子目录
            } else {
                Future<List<String>> future = executor.submit(() -> extractComments(file));
                futures.add(future);  // 异步处理文件
            }
        }

        for (Future<List<String>> future : futures) {
            try {
                comments.addAll(future.get());
            } catch (ExecutionException e) {
                System.err.println("处理文件时出错: " + e.getMessage());
            }
        }

        executor.shutdown();
        return comments;
    }
}
