package com.evacipated.cardcrawl.modthespire.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CollapsiblePanel extends JPanel {
    private JButton button;
    private JPanel contents;

    /**
     * Creates a new CollapsiblePanel.
     */
    public CollapsiblePanel() {
        setupUI();
    }

    /**
     * Creates a new CollapsiblePanel.
     *
     * @param header The header text to display on the panel.
     */
    public CollapsiblePanel(String header) {
        this();

        setHeaderText(header);
    }

    /**
     * Sets up the panel's UI.
     * <p>
     * You shouldn't need to call this yourself.
     */
    public void setupUI() {
        button = new JButton();
        JSeparator headerLine = new JSeparator(JSeparator.HORIZONTAL);
        contents = new JPanel();

        button.setContentAreaFilled(false);
        button.setIcon(UIManager.getIcon("Tree.collapsedIcon"));
        button.addActionListener((ActionEvent e) -> {
            contents.setVisible(!contents.isVisible());

            button.setIcon(UIManager.getIcon(contents.isVisible() ? "Tree.expandedIcon" : "Tree.collapsedIcon"));
        });

        button.setFocusPainted(false);

        contents.setVisible(false);

        setLayout(new GridBagLayout());
        contents.setLayout(new BoxLayout(contents, BoxLayout.PAGE_AXIS));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;

        add(button, constraints);

        constraints.gridy = 1;
        add(headerLine, constraints);

        constraints.gridy = 2;
        add(contents, constraints);
    }

    /**
     * Returns the contents panel.
     */
    public JPanel getContents() {
        return contents;
    }

    /**
     * Updates the panel's header text.
     *
     * @param text The new header text for the panel.
     */
    public void setHeaderText(String text) {
        button.setText(text);
    }
}
