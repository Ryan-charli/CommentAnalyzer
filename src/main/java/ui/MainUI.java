package ui;

import analysis.CommentAnalyzer;
import analysis.CommentLocation;
import parser.CommentExtractor;
import analysis.CodeQualityAnalyzer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MainUI {
    private final JFrame frame;
    private final CommentAnalyzer analyzer;
    private final JTextArea outputArea;
    private final JProgressBar progressBar;
    private final JProgressBar reportProgressBar;
    private final JLabel statusLabel;
    private File currentDirectory;
    private final ExecutorService executorService;
    private final Map<String, List<CommentLocation>> allResults;

    private static final int BATCH_SIZE = 100;
    private static final int TEXT_BUFFER_LIMIT = 1000000;

    public MainUI() {
        analyzer = new CommentAnalyzer(); // Updated constructor call
        executorService = Executors.newFixedThreadPool(6);
        allResults = new ConcurrentHashMap<>();
        
        frame = new JFrame("Multi-Language Comment Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        
        JPanel buttonPanel = new JPanel();
        JButton selectButton = new JButton("Select Directory");
        JButton exportButton = new JButton("Export Report");
        
        selectButton.addActionListener(event -> selectDirectory());
        exportButton.addActionListener(event -> exportReport());
        
        buttonPanel.add(selectButton);
        buttonPanel.add(exportButton);
        
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel progressPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        progressBar = new JProgressBar(0, 100);
        progressBar.setString("Analysis Progress");
        progressBar.setStringPainted(true);
        
        reportProgressBar = new JProgressBar(0, 100);
        reportProgressBar.setString("Report Generation");
        reportProgressBar.setStringPainted(true);
        
        progressPanel.add(progressBar);
        progressPanel.add(reportProgressBar);
        
        statusLabel = new JLabel("Ready");
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressPanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        
        frame.getContentPane().add(BorderLayout.NORTH, buttonPanel);
        frame.getContentPane().add(BorderLayout.CENTER, new JScrollPane(outputArea));
        frame.getContentPane().add(BorderLayout.SOUTH, bottomPanel);
        
        frame.setVisible(true);
    }

    // Method for selecting directory to analyze
    private void selectDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            currentDirectory = fileChooser.getSelectedFile();
            startAnalysis(currentDirectory);
        }
    }

    // Method to start the analysis process
    private void startAnalysis(File directory) {
        allResults.clear();
        outputArea.setText("Analysis in progress...\n");
        reportProgressBar.setValue(0);
        progressBar.setValue(0);
        
        CompletableFuture.runAsync(() -> {
            try {
                // Create an extractor that auto-detects language
                CommentExtractor extractor = new CommentExtractor("");
                extractor.setProgressListener((progress, status) -> {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        statusLabel.setText(status);
                    });
                });
                
                extractor.setResultCallback((filePath, comments) -> {
                    if (!comments.isEmpty()) {
                        allResults.put(filePath, comments);
                    }
                });
                
                extractor.extractCommentsFromDirectory(directory);
                generateAndDisplayReport();
                
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(frame, "Error: " + e.getMessage());
                    statusLabel.setText("Analysis failed");
                    progressBar.setValue(0);
                    reportProgressBar.setValue(0);
                });
            }
        }, executorService);
    }

    private void generateAndDisplayReport() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Generating basic report...");
            
            // First generate basic report
            StringBuilder report = new StringBuilder();
            report.append("Basic Analysis Report\n");
            report.append("====================\n\n");
            
            int totalComments = 0;
            double totalQuality = 0;
            
            for (List<CommentLocation> comments : allResults.values()) {
                for (CommentLocation comment : comments) {
                    totalComments++;
                    totalQuality += analyzer.getCommentQuality(
                        comment.getContent(), "", false).getScore();
                }
            }
            
            report.append(String.format("""
                Summary:
                Total files with comments: %d
                Total comments: %d
                Average quality score: %.2f
                
                Base Directory: %s
                
                """, allResults.size(), totalComments,
                totalComments > 0 ? totalQuality / totalComments : 0,
                currentDirectory.getAbsolutePath()));
            
            outputArea.setText(report.toString());
            
            // Then start AI analysis
            statusLabel.setText("Starting AI analysis...");
            analyzer.startAIAnalysis(aiReport -> {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("\nAI Analysis Results\n");
                    outputArea.append("==================\n\n");
                    outputArea.append("Language: " + aiReport.get("language") + "\n");
                    outputArea.append(aiReport.get("aiAnalysis").toString() + "\n\n");
                });
            });
        });
    }

    // Inner class for storing report statistics
    private static class ReportStats {
        final int totalComments;
        final double totalQuality;
        final int fileCount;

        ReportStats(int totalComments, double totalQuality, int fileCount) {
            this.totalComments = totalComments;
            this.totalQuality = totalQuality;
            this.fileCount = fileCount;
        }
    }

    // Method for exporting the report to a file
    private void exportReport() {
        if (allResults.isEmpty() || outputArea.getText().equals("Analysis in progress...\n")) {
            JOptionPane.showMessageDialog(frame, "No report to export");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("comment_analysis_report.txt"));
        
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            statusLabel.setText("Exporting report...");
            CompletableFuture.runAsync(() -> {
                try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                    // Write header and summary
                    writer.print(outputArea.getText().split("\\.\\.\\.")[0]);
                    
                    List<String> sortedPaths = new ArrayList<>(allResults.keySet());
                    Collections.sort(sortedPaths);
                    
                    for (String filePath : sortedPaths) {
                        List<CommentLocation> comments = allResults.get(filePath);
                        if (comments == null || comments.isEmpty()) continue;

                        writer.println("File: " + filePath);
                        writer.println("Number of comments: " + comments.size() + "\n");

                        for (CommentLocation comment : comments) {
                            double quality = analyzer.getCommentQuality(
                                comment.getContent(), "", false).getScore();
                            writer.printf("Line %d: (Quality Score: %.2f)%n",
                                comment.getLineNumber(),
                                quality);
                            writer.println(comment.getContent() + "\n");
                        }
                        writer.println("-------------------\n");
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Report exported successfully");
                        JOptionPane.showMessageDialog(frame, "Report exported successfully");
                    });
                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Export failed");
                        JOptionPane.showMessageDialog(frame, "Error exporting report: " + e.getMessage());
                    });
                }
            }, executorService);
        }
    }

    // Method for shutting down the application
    public void shutdown() {
        try {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (!executorService.isTerminated()) {
                executorService.shutdownNow();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainUI ui = new MainUI();
            Runtime.getRuntime().addShutdownHook(new Thread(ui::shutdown));
        });
    }
}