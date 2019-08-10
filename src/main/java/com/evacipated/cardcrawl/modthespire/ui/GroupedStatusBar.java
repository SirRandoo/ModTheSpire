package com.evacipated.cardcrawl.modthespire.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;

/**
 * A simple status bar.
 * <p>
 * This implementation currently contains a JLabel
 * to serve as a status message, a progress bar for
 * informing the end-user on current tasks, and button
 * groups.
 * <p>
 * Button groups serve as a way to recycle the status bar
 * for other panels.
 */
public class GroupedStatusBar extends JPanel {
    // Left-right ui elements
    private JLabel message;
    private JProgressBar progressBar;
    private CardLayout groups;

    // Internal variables
    private Timer timer;
    private HashMap<String, JPanel> groupMap = new HashMap<>();

    public GroupedStatusBar() {
        createUI();
    }

    /**
     * Creates the ui elements for this implementation of
     * a status bar.
     * <p>
     * This shouldn't be called directly.
     */
    private void createUI() {
        message = new JLabel();
        message.setBorder(null);

        progressBar = new JProgressBar();
        progressBar.setMaximumSize(new Dimension(250, 20));
        progressBar.setVisible(false);

        groups = new CardLayout();

        timer = new Timer(0, (ActionEvent event) -> clearMessage());
        timer.setRepeats(false);

        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(message);
        add(Box.createHorizontalGlue());
        add(progressBar);
        add(new JPanel(groups));
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
     * Returns the status bar's progress bar's current value.
     */
    public int getProgressValue() {
        return progressBar.getValue();
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
     * @param message The message to display.
     */
    public void showMessage(String message) {
        showMessage(message, 5);
    }

    /**
     * Sets a message on the status bar.
     * <p>
     * This method does not remove the message
     *
     * @param message The message to display.
     */
    public void setMessage(String message) {
        this.message.setText(message);
    }

    /**
     * Clears a message on the status bar.
     */
    public void clearMessage() {
        this.message.setText("");
    }

    /**
     * Creates a new button group.
     *
     * @param id      The ID to register the group of buttons under.
     * @param buttons The buttons to group onto the status bar.
     * @throws IllegalArgumentException The ID passed is already registered.
     */
    public void addButtonGroup(String id, StatusButton... buttons) throws IllegalArgumentException {
        if (buttons.length == 0) return;
        if (groupMap.containsKey(id)) throw new IllegalArgumentException();

        JPanel container = new JPanel();
        BoxLayout layout = new BoxLayout(container, BoxLayout.X_AXIS);

        container.setLayout(layout);

        for (StatusButton button : buttons) container.add(button);

        groupMap.put(id, container);
        groups.addLayoutComponent(container, id);
    }

    /**
     * Removes a button group.
     *
     * @param id The button group to remove.
     */
    public void removeButtonGroup(String id) {
        JPanel i = groupMap.get(id);

        if (i != null) groups.removeLayoutComponent(groupMap.get(id));
    }
}
