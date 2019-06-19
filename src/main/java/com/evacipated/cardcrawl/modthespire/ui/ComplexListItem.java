package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ComplexListItem extends JPanel {
    // Icons
    public static final ImageIcon LOADING_ICON = new ImageIcon(Loader.class.getResource("/assets/ajax-loader.gif"));
    public static final ImageIcon DOWNLOAD_ICON = new ImageIcon(Loader.class.getResource("/assets/download.gif"));
    public static final ImageIcon ERROR_ICON = new ImageIcon(Loader.class.getResource("/assets/error.gif"));
    public static final ImageIcon CHECKMARK_ICON = new ImageIcon(Loader.class.getResource("/assets/good.gif"));
    public static final ImageIcon UPDATE_ICON = new ImageIcon(Loader.class.getResource("/assets/update.gif"));
    public static final ImageIcon WARNING_ICON = new ImageIcon(Loader.class.getResource("/assets/warning.gif"));
    public static final ImageIcon WORKSHOP_ICON = new ImageIcon(Loader.class.getResource("/assets/workshop.gif"));

    private JCheckBox label;
    private JLabel sourceIcon;
    private JButton statusButton;
    private States state;
    private Sources source;


    /**
     * Creates a new ComplexListItem.
     */
    public ComplexListItem() {
        setupUI();
    }

    /**
     * Creates a new ComplexListItem.
     *
     * @param name  The name to label the item
     * @param state The current state of the item.  If the
     *              state is null, the status button will
     *              not be visible.
     */
    public ComplexListItem(String name, States state) {
        this();

        label.setText(name);
        updateModState(state);
    }

    /**
     * Sets up the mod item's ui.
     * <p>
     * You shouldn't need to call this yourself.
     */
    private void setupUI() {
        label = new JCheckBox();
        statusButton = new JButton();
        sourceIcon = new JLabel();

        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.weighty = 1;
        constraints.weightx = 1;

        add(label, constraints);

        constraints.anchor = GridBagConstraints.EAST;
        add(statusButton, constraints);
        add(sourceIcon, constraints);
    }

    /**
     * Returns the current state for the mod item.
     */
    public States getModState() {
        return state;
    }

    /**
     * Updates the state of a mod item.
     *
     * @param state The new state of the mod item.
     */
    public void updateModState(States state) {
        this.state = state;

        // If the state isn't null, update the status button's icon to
        // reflect its current state.
        if (this.state != null) {
            switch (this.state) {
                case DEPENDENCY_MISSING:
                case UPDATE_FAILED:
                    statusButton.setIcon(ERROR_ICON);
                    break;

                case UPDATE_PENDING:
                    statusButton.setIcon(UPDATE_ICON);
                    break;

                case UPDATING:
                    statusButton.setIcon(LOADING_ICON);
                    break;
            }
        }

        // If the state isn't null, allow the user to interact with it.
        if (this.state != null) {
            statusButton.setVisible(true);
            statusButton.setEnabled(true);

            // If the state is null, disallow the user from interacting with it.
        } else {
            statusButton.setEnabled(false);
            statusButton.setVisible(false);
        }
    }

    /**
     * Gets the check state for the JCheckBox label.
     */
    public boolean getCheckState() {
        return label.isSelected();
    }

    /**
     * Sets the check state for the JCheckBox label.
     *
     * @param checkState Whether or not the box should be checked.
     */
    public void setCheckState(boolean checkState) {
        label.setSelected(checkState);
    }

    /**
     * Gets the text on the item's check box.
     */
    public String getText() {
        return label.getText();
    }

    /**
     * Gets the JCheckBox for the item.
     */
    public JCheckBox getCheckBox() {
        return label;
    }

    /**
     * Sets the list item's display text.
     */
    public void setText(String text) {
        label.setText(text);
    }

    /**
     * Returns the mod's source.
     */
    public Sources getSource() {
        return source;
    }

    /**
     * Sets the source for list item.
     *
     * @param source A source indicated where the mod came from.
     */
    public void setSource(Sources source) {
        if (source == Sources.WORKSHOP) {
            this.sourceIcon.setIcon(WORKSHOP_ICON);
        }

        this.source = source;
    }

    /**
     * The different states a mod item can be in.
     *
     * <h3>DEPENDENCY_MISSING</h3>
     * A mod item is currently missing dependencies.
     * Users can click the status button while it's
     * in this state to view the missing dependencies.
     *
     * <h3>UPDATE_MISSING</h3>
     * A mod item couldn't be updated for some arcane
     * reason.  Users can click the status button while
     * it's in this state to view the reason an update
     * failed.
     *
     * <h3>UPDATE_PENDING</h3>
     * A mod item has a new update, but has yet to be
     * installed.  Users can click the status button
     * while it's in this state to update a specific
     * mod.
     *
     * <h3>UPDATING</h3>
     * A mod item is currently updating.  Users can
     * click the status button while it's in this
     * state to view the updater's current status.
     */
    enum States {DEPENDENCY_MISSING, UPDATE_FAILED, UPDATE_PENDING, UPDATING}

    /**
     * The different sources a mod item can originate from.
     *
     * <h3>OTHER</h3>
     * A mod item originated from a non-workshop repository.
     *
     * <h3>WORKSHOP</h3>
     * A mod item originated from the Steam workshop.
     */
    enum Sources {OTHER, WORKSHOP}
}
