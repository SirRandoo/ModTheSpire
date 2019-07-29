package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CheckableListItem extends JPanel {
    // Icons
    public static final ImageIcon ENABLED_ICON = new ImageIcon(Loader.class.getResource("/assets/check-icon.png"));
    public static final ImageIcon DISABLED_ICON = new ImageIcon(Loader.class.getResource("/assets/close-icon.png"));

    // Ui elements
    protected JLabel name;
    protected JButton stateBtn;

    // Internal
    private boolean isEnabled;

    /**
     * Constructs a new CheckableListItem.
     */
    public CheckableListItem(String text) {
        setupUI();

        name.setText(text);
    }

    /**
     * Sets up the mod item's ui.
     * <p>
     * You shouldn't need to call this yourself.
     */
    protected void setupUI() {
        // Ui element creation
        name = new JLabel();
        stateBtn = new StatusButton(null);

        // Element population
        setCheckState(false);

        // Listeners
        stateBtn.addActionListener(e -> setCheckState(!isEnabled));

        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.weighty = 1;
        constraints.weightx = 1;

        add(stateBtn);
        add(name, constraints);
    }

    /**
     * Returns the check state of the item.
     */
    public boolean getCheckState() {
        return isEnabled;
    }

    /**
     * Updates the check state state of the item.
     *
     * @param state Whether or not the item should be considered enabled.
     */
    public void setCheckState(boolean state) {
        isEnabled = state;

        stateBtn.setIcon(isEnabled ? DISABLED_ICON : ENABLED_ICON);
    }

    /**
     * Returns the current text being display on the item.
     */
    public String getText() {
        return name.getText();
    }

    /**
     * Returns the check button's enabled state.
     */
    public boolean getEnabledState() {
        return stateBtn.isEnabled();
    }

    /**
     * Updates the check button's enabled state.
     *
     * @param state Whether or not the check button should be enabled.
     */
    public void setEnabledState(boolean state) {
        stateBtn.setEnabled(state);
    }
}
