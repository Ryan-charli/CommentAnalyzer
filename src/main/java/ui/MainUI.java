package ui;

import analysis.CodeAnalyzer;
import analysis.CommentAnalyzer;
import parser.CommentExtractor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.List;
import java.io.PrintWriter;

public class MainUI {
    private JFrame frame;
    private CommentAnalyzer analyzer;
    private JTextArea outputArea;

    public MainUI() {
        frame = new JFrame("Comment Analyzer");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(600, 400);
    
    JPanel buttonPanel = new JPanel();
    JButton selectButton = new JButton("Select Directory");
    JButton exportButton = new JButton("Export Report");
    
    selectButton.addActionListener(__ -> selectDirectory());
    exportButton.addActionListener(__ -> exportReport());
    
    buttonPanel.add(selectButton);
    buttonPanel.add(exportButton);
    
    outputArea = new JTextArea();
    outputArea.setEditable(false);
    outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

    frame.getContentPane().add(BorderLayout.NORTH, buttonPanel);
    frame.getContentPane().add(BorderLayout.CENTER, new JScrollPane(outputArea));
    frame.setVisible(true);
}
    private void exportReport() {
    if (outputArea.getText().isEmpty()) {
        JOptionPane.showMessageDialog(frame, "No report to export");
        return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setSelectedFile(new File("code_analysis_report.txt"));
    
    if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
        try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
            writer.print(outputArea.getText());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error exporting report: " + e.getMessage());
        }
    }
}
    private String analyzeAllJavaFiles(File directory, CodeAnalyzer analyzer) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.getName().endsWith(".java")) {
                    analyzer.analyzeFile(file);
                }
            }
        }
        return analyzer.generateReport();
    }
    
    private void selectDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File directory = fileChooser.getSelectedFile();
            try {
                analyzer = new CommentAnalyzer("java"); // 初始化 analyzer
                CommentExtractor extractor = new CommentExtractor("java");
                CodeAnalyzer codeAnalyzer = new CodeAnalyzer();
                
                List<String> comments = extractor.extractCommentsFromDirectory(directory);
                analyzer.addComments(comments); // 添加注释
                String codeReport = analyzeAllJavaFiles(directory, codeAnalyzer);
                
                displayResults(codeReport, comments);
                extractor.shutdown();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
            }
        }
    }

private void displayResults(String codeReport, List<String> comments) {
    StringBuilder sb = new StringBuilder();
    sb.append(codeReport).append("\n\n");
    
    Map<String, Double> qualityReport = analyzer.analyzeCommentQuality();
    sb.append("Comment Analysis\n================\n\n");
    sb.append("Total Comments: ").append(comments.size()).append("\n\n");
    
    for (Map.Entry<String, Double> entry : qualityReport.entrySet()) {
        if (!entry.getKey().equals("Average Score")) {
            sb.append("Comment: ").append(entry.getKey()).append("\n");
            sb.append("Quality Score: ").append(String.format("%.2f", entry.getValue())).append("\n\n");
        }
    }
    
    outputArea.setText(sb.toString());
}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainUI::new);
    }
}