package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

/**
 * A simple status bar.
 * <p>
 * This implementation currently contains a JLabel
 * to serve as a status message, a progress bar for
 * displaying a single in-app task, or task sequence,
 * a play button for launching Slay the Spire without
 * navigating to the File menu, a mods folder button
 * for opening the mods folder for ModTheSpire,
 * and a dump jar button for setting ModTheSpire
 * to "dump" mode.
 */
public class AugmentedStatusBar extends JPanel {
    private JButton modFolder;
    private JProgressBar progressBar;
    private JButton playSpire;
    private JButton dumpJar;

    private JLabel message;
    private Timer timer;

    public AugmentedStatusBar() {
        super();

        createUI();
    }

    /**
     * Creates the ui elements for this implementation of
     * a status bar.
     * <p>
     * This shouldn't be called directly.
     */
    private void createUI() {
        // Set the mods folder button's icon to an actual folder icon,
        // while creating a helpful tooltip.
        modFolder = new JButton(UIManager.getIcon("FileView.directoryIcon"));
        modFolder.setToolTipText("Opens the mods folder for ModTheSpire.");
        modFolder.setHideActionText(true);
        modFolder.setContentAreaFilled(false);
        modFolder.setMargin(new Insets(0, 6, 0, 6));
        modFolder.setMaximumSize(new Dimension(modFolder.getIcon().getIconWidth() + 4, modFolder.getIcon().getIconWidth() + 4));
        modFolder.setSize(modFolder.getMaximumSize());

        modFolder.addActionListener((ActionEvent event) -> {
            File file = new File(Loader.MOD_DIR);

            try {
                if (!file.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.mkdirs();
                }

                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } catch (IOException e) {
                showMessage(e.getMessage(), 2);

                e.printStackTrace();
            }
        });

        playSpire = new JButton(new ImageIcon(this.getClass().getResource("/assets/play-icon.png")));
        playSpire.setToolTipText("Launch Slay the Spire with mods.");
        playSpire.setSelected(true);
        playSpire.setDefaultCapable(true);
        playSpire.setHideActionText(true);
        playSpire.setContentAreaFilled(false);
        playSpire.setMargin(new Insets(0, 6, 0, 6));
        playSpire.setMaximumSize(new Dimension(playSpire.getIcon().getIconWidth() + 4, playSpire.getIcon().getIconHeight() + 4));
        playSpire.setSize(playSpire.getMaximumSize());

        dumpJar = new JButton(UIManager.getIcon("FileView.fileIcon"));
        dumpJar.setToolTipText("Jump an output jar with mod patches.");
        dumpJar.setHideActionText(true);
        dumpJar.setContentAreaFilled(false);
        dumpJar.setMargin(new Insets(0, 6, 0, 6));
        dumpJar.setMaximumSize(new Dimension(dumpJar.getIcon().getIconWidth() + 4, dumpJar.getIcon().getIconHeight() + 4));
        dumpJar.setSize(dumpJar.getMaximumSize());

        message = new JLabel();
        message.setBorder(null);

        progressBar = new JProgressBar();
        progressBar.setMaximumSize(new Dimension(250, 20));
        progressBar.setVisible(false);

        timer = new Timer(0, (ActionEvent event) -> message.setText(""));
        timer.setRepeats(false);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(message);
        add(Box.createHorizontalGlue());
        add(progressBar);
        add(playSpire);
        add(modFolder);
        add(dumpJar);
    }

    /**
     * Adds an action listener to the play button on the
     * status bar.
     *
     * @param l The action listener to install.
     */
    public void addPlayActionListener(ActionListener l) {
        playSpire.addActionListener(l);
    }

    /**
     * Removes an action listener from the play button
     * on the status bar.
     *
     * @param l The action listener to remove.
     */
    public void removePlayActionListener(ActionListener l) {
        playSpire.removeActionListener(l);
    }

    /**
     * Adds an action listener to the dump jar button on the
     * status bar.
     *
     * @param l The action listener to install.
     */
    public void addDumpActionListener(ActionListener l) {
        dumpJar.addActionListener(l);
    }

    /**
     * Removes an action listener from the dump jar button
     * on the status bar.
     *
     * @param l The action listener to remove.
     */
    public void removeDumpActionListener(ActionListener l) {
        dumpJar.removeActionListener(l);
    }

    /**
     * Sets the status bar's progress bar's visibility.
     *
     * @param visible Whether or not the progress bar
     *                should be visible.
     */
    public void setProgressBarVisible(boolean visible) {
        progressBar.setVisible(visible);
    }

    /**
     * Sets the status bar's progress bar's display type
     * to indeterminate.
     *
     * @param indeterminate Whether or not the progress
     *                      bar should be indeterminate.
     */
    public void setProgressIndeterminate(boolean indeterminate) {
        progressBar.setIndeterminate(indeterminate);
    }

    /**
     * Sets the status bar's progress bar's maximum.
     *
     * @param maximum The maximum value the progress
     *                bar can be.
     */
    public void setProgressMaximum(int maximum) {
        progressBar.setMaximum(maximum);
    }

    /**
     * Sets the status bar's progress bar's current
     * value.
     *
     * @param value The new value to display on the
     *              progress bar.
     */
    public void setProgressValue(int value) {
        progressBar.setValue(value);
    }

    /**
     * Shows a message on the status bar.
     *
     * @param message The message to display.
     * @param seconds The number of seconds to display
     *                the message for.
     */
    public void showMessage(String message, float seconds) {
        if (timer.isRunning()) timer.stop();

        // Mild padding to ensure it isn't against the window frame.
        this.message.setText(" " + message);
        timer.setInitialDelay((int) (seconds * 1000));

        timer.start();
    }

    /**
     * Shows a message on the status bar.
     * <p>
     * This implementation defaults to 5 seconds.
     *
     * @param message The number of seconds to display
     *                the message for.
     */
    public void showMessage(String message) {
        showMessage(message, 5);
    }
}
