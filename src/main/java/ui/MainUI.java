package ui;

import analysis.CommentAnalyzer;
import analysis.CommentLocation;
import parser.CommentExtractor;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
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

    // Constants for batch processing
    private static final int BATCH_SIZE = 100;
    private static final int TEXT_BUFFER_LIMIT = 1000000;

    public MainUI() {
        analyzer = new CommentAnalyzer("java");
        executorService = Executors.newFixedThreadPool(6); // Increased thread pool size
        allResults = new ConcurrentHashMap<>();
        
        frame = new JFrame("Comment Analyzer");
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

    private void selectDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            currentDirectory = fileChooser.getSelectedFile();
            startAnalysis(currentDirectory);
        }
    }

    private void startAnalysis(File directory) {
        allResults.clear();
        outputArea.setText("Analysis in progress...\n");
        reportProgressBar.setValue(0);
        progressBar.setValue(0);
        
        CompletableFuture.runAsync(() -> {
            try {
                CommentExtractor extractor = new CommentExtractor("java");
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
            statusLabel.setText("Generating report...");
            
            CompletableFuture.supplyAsync(() -> {
                int totalComments = 0;
                double totalQuality = 0;
                
                for (List<CommentLocation> comments : allResults.values()) {
                    for (CommentLocation comment : comments) {
                        totalComments++;
                        totalQuality += analyzer.analyzeCommentQuality(comment.getContent());
                    }
                }
                
                return new ReportStats(totalComments, totalQuality, allResults.size());
            }, executorService).thenAcceptAsync(stats -> {
                StringBuilder report = new StringBuilder();
                report.append("Comment Analysis Report\n");
                report.append("=====================\n\n");
                report.append(String.format("""
                    Summary:
                    Total files with comments: %d
                    Total comments: %d
                    Average quality score: %.2f
                    
                    Base Directory: %s
                    
                    """, stats.fileCount, stats.totalComments,
                    stats.totalComments > 0 ? stats.totalQuality / stats.totalComments : 0,
                    currentDirectory.getAbsolutePath()));

                outputArea.setText(report.toString());
                
                List<String> sortedPaths = new ArrayList<>(allResults.keySet());
                Collections.sort(sortedPaths);
                
                int totalBatches = (sortedPaths.size() + BATCH_SIZE - 1) / BATCH_SIZE;
                AtomicInteger processedBatches = new AtomicInteger(0);
                
                for (int i = 0; i < sortedPaths.size(); i += BATCH_SIZE) {
                    int end = Math.min(i + BATCH_SIZE, sortedPaths.size());
                    List<String> batch = sortedPaths.subList(i, end);
                    
                    StringBuilder batchReport = new StringBuilder();
                    for (String filePath : batch) {
                        List<CommentLocation> comments = allResults.get(filePath);
                        if (comments == null || comments.isEmpty()) continue;

                        batchReport.append("File: ").append(filePath).append("\n");
                        batchReport.append("Number of comments: ").append(comments.size()).append("\n\n");

                        for (CommentLocation comment : comments) {
                            double quality = analyzer.analyzeCommentQuality(comment.getContent());
                            batchReport.append(String.format("Line %d: (Quality Score: %.2f)\n",
                                comment.getLineNumber(),
                                quality));
                            batchReport.append(comment.getContent()).append("\n\n");
                        }
                        batchReport.append("-------------------\n\n");
                    }

                    int currentBatch = processedBatches.incrementAndGet();
                    int progress = (currentBatch * 100) / totalBatches;
                    
                    SwingUtilities.invokeLater(() -> {
                        reportProgressBar.setValue(progress);
                        if (outputArea.getText().length() < TEXT_BUFFER_LIMIT) {
                            outputArea.append(batchReport.toString());
                        }
                    });
                }

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Analysis complete - Found " + stats.totalComments + " comments");
                    reportProgressBar.setValue(100);
                    
                    if (outputArea.getText().length() >= TEXT_BUFFER_LIMIT) {
                        outputArea.append("\n... Report truncated. Please export to see full results ...\n");
                    }
                });
            }, executorService);
        });
    }

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
                            double quality = analyzer.analyzeCommentQuality(comment.getContent());
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