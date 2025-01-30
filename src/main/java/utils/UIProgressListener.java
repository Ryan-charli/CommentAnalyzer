package utils;

import javax.swing.*;

import ui.ProgressListener;

public class UIProgressListener implements ProgressListener {
    private JProgressBar bar;
    private JLabel label;
    
    public UIProgressListener(JProgressBar bar, JLabel label) {
        this.bar = bar;
        this.label = label;
    }
    
    @Override
    public void updateProgress(int progress, String status) {
        SwingUtilities.invokeLater(() -> {
            bar.setValue(progress);
            label.setText(status);
        });
    }
}