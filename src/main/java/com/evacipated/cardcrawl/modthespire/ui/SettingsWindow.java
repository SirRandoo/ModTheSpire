package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;


/**
 * A settings dialog for ModTheSpire.
 */
public class SettingsWindow extends JDialog {
    // Ui elements
    private JCheckBox mtsDebug;
    private JCheckBox mtsBeta;

    public SettingsWindow(JFrame parent) {
        super(parent, parent.getTitle() + " - Settings", ModalityType.APPLICATION_MODAL);

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setResizable(true);

        setupUI();
    }

    private void setupUI() {
        mtsDebug = new JCheckBox("Debug mode");
        mtsBeta = new JCheckBox("Slay the Spire beta");

        mtsDebug.setToolTipText("Enable debug mode for ModTheSpire.");
        mtsDebug.addItemListener(e -> {
            Loader.DEBUG = e.getStateChange() == ItemEvent.SELECTED;

            Loader.MTS_CONFIG.setBool("debug", Loader.DEBUG);
        });

        // TODO: Consider the viability of the beta checkbox
        mtsBeta.setToolTipText("Whether or not ModTheSpire will run on the beta version of Slay the Spire.");
        mtsBeta.addItemListener(e -> Loader.allowBeta = true);

        JPanel panel = new JPanel();

        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        panel.add(mtsDebug);
        panel.add(mtsBeta);

        add(panel);
        setSize(new Dimension(400, 300));
        setMinimumSize(getSize());
    }
}
