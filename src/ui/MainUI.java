package ui;

import analysis.CommentAnalyzer;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import parser.CodeParser;
import parser.CommentExtractor;

public class MainUI {
    private JFrame frame;
    private CommentAnalyzer analyzer;

    public MainUI() {
        frame = new JFrame("Comment Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        
        JButton selectButton = new JButton("Select Directory");
        selectButton.addActionListener(e -> selectDirectory());
        
        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);

        frame.getContentPane().add(BorderLayout.NORTH, selectButton);
        frame.getContentPane().add(BorderLayout.CENTER, new JScrollPane(outputArea));
        frame.setVisible(true);
    }

    private void selectDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File directory = fileChooser.getSelectedFile();
            String language = "java"; // Set as target language
            analyzer = new CommentAnalyzer(language);
            analyzer.analyzeDirectory(directory);

            // Display of analysis results
            analyzer.displayComments();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainUI::new);
    }
}
