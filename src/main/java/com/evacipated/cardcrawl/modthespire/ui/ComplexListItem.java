package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.StringJoiner;

public class ComplexListItem extends JPanel {
    // Standard icons
    public static final ImageIcon ENABLED_ICON = new ImageIcon(Loader.class.getResource("/assets/check-icon.png"));
    public static final ImageIcon DISABLED_ICON = new ImageIcon(Loader.class.getResource("/assets/close-icon.png"));
    public static final ImageIcon WORKSHOP_ICON = new ImageIcon(Loader.class.getResource("/assets/workshop.png"));

    // Error icons
    public static final ImageIcon ERROR_ICON = new ImageIcon(Loader.class.getResource("/assets/error-icon.png"));
    public static final ImageIcon WARNING_ICON = new ImageIcon(Loader.class.getResource("/assets/warning-icon.png"));

    private JLabel name;
    private JLabel sourceIcon;
    private StatusButton statusButton;
    private StatusButton stateButton;

    private boolean isEnabled;
    private ModInfo modInfo;
    private String[] missingDependencies;

    /**
     * Constructs a new ComplexListItem.
     *
     * @param modInfo The ModInfo instance created by the loader.
     */
    public ComplexListItem(ModInfo modInfo) {
        this.modInfo = modInfo;
        missingDependencies = getMissingDependencies();
        isEnabled = false;

        setupUI();
    }

    /**
     * Sets up the mod item's ui.
     * <p>
     * You shouldn't need to call this yourself.
     */
    private void setupUI() {
        // Ui element creation
        name = new JLabel();
        sourceIcon = new JLabel();
        stateButton = new StatusButton(null);
        statusButton = new StatusButton(WARNING_ICON);

        // Element population
        name.setText(modInfo.Name != null ? modInfo.Name : modInfo.ID);
        setEnabled(false);

        if (!modInfo.isWorkshop) sourceIcon.setVisible(false);
        if (missingDependencies.length <= 0) statusButton.setVisible(false);

        // Listeners
        stateButton.addActionListener(e -> setEnabled(!isEnabled));
        statusButton.addActionListener(e -> showMissingDependencies());

        setLayout(new GridBagLayout());
        setBorder(new EmptyBorder(0, 0, 0, 0));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.weighty = 1;
        constraints.weightx = 1;

        add(stateButton);
        add(name, constraints);

        constraints.anchor = GridBagConstraints.EAST;
        add(statusButton, constraints);
        add(sourceIcon, constraints);
    }

    /**
     * Updates the enabled state of the item.
     *
     * @param state Whether or not the item should be considered enabled.
     */
    public void setEnabled(boolean state) {
        isEnabled = state;

        stateButton.setIcon(isEnabled ? DISABLED_ICON : ENABLED_ICON);
        stateButton.setToolTipText((isEnabled ? "Disables " : "Enables ") + name.getText());
    }

    /**
     * Displays a dialog to the end-user containing a list of mods currently missing.
     */
    public void showMissingDependencies() {
        StringJoiner message = new StringJoiner(", ");

        for (String dependency : missingDependencies) {
            message.add(dependency);
        }

        JOptionPane.showMessageDialog(Launcher.getInstance(), "Missing dependencies: " + message.toString(), name.getText() + " - Missing Dependencies", JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Returns a string array of the item's missing dependencies.
     *
     * @return A complete list of missing dependencies.
     */
    public String[] getMissingDependencies() {
        ArrayList<String> dependencies = new ArrayList<>();

        for (String dependency : modInfo.Dependencies) {
            if (!Launcher.getInstance().wasModDiscovered(dependency)) {
                dependencies.add(dependency);
            }
        }

        return (String[]) dependencies.toArray();
    }
}
