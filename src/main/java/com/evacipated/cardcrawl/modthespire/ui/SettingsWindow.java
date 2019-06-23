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
    private JCheckBox launcherBypass;

    /**
     * Constructs the settings dialog.
     *
     * @param parent Typically the launcher instance, but can be
     *               an other JFrame instance.
     */
    public SettingsWindow(JFrame parent) {
        super(parent, parent.getTitle() + " - Settings", ModalityType.APPLICATION_MODAL);

        this.setResizable(true);

        setupUI();
        applySettings();
    }

    /**
     * Sets up the ui for the settings dialog.
     */
    private void setupUI() {
        // Object creation
        JPanel panel = new JPanel();

        mtsDebug = new JCheckBox("Enable debug mode");
        mtsBeta = new JCheckBox("Allow mods on the beta version of Slay the Spire");  // TODO: Consider the viability of the beta checkbox
        launcherBypass = new JCheckBox("Skip the launcher if a preset was previously loaded");

        // Tooltips
        mtsDebug.setToolTipText("If debug mode is enabled, debugging information will be outputted to your log.");
        mtsBeta.setToolTipText("Whether or not ModTheSpire will launch on the beta version of Slay the Spire.\n\n*Not all mods may work!");
        launcherBypass.setToolTipText("If launcher bypass is enabled, the launcher will automatically start Slay the Spire the next time ModTheSpire is launched.\n\n*Your last launched preset will be used!");

        // Item listeners
        mtsDebug.addItemListener(e -> {
            Loader.DEBUG = e.getStateChange() == ItemEvent.SELECTED;

            Loader.MTS_CONFIG.setBool("debug", Loader.DEBUG);
        });

        mtsBeta.addItemListener(e -> {
            Loader.allowBeta = true;

            Loader.MTS_CONFIG.setBool("isBeta", e.getStateChange() == ItemEvent.SELECTED);
        });
        launcherBypass.addItemListener(e -> Loader.MTS_CONFIG.setBool("launcher.presets.bypass", e.getStateChange() == ItemEvent.SELECTED));

        // Panel setup
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Object insertion
        panel.add(mtsDebug);
        panel.add(mtsBeta);
        panel.add(launcherBypass);

        // Dialog setup
        add(panel);
        setSize(new Dimension(400, 300));
        setMinimumSize(getSize());
    }

    /**
     * Applies the user's custom settings.
     * <p>
     * This method is responsible for applying the user's
     * custom settings between sessions.  Without this method,
     * settings would not persist through individual sessions.
     */
    public void applySettings() {
        Loader.allowBeta = Loader.MTS_CONFIG.getBool("isBeta");

        mtsDebug.setSelected(Loader.DEBUG);
        mtsBeta.setSelected(Loader.allowBeta);
        launcherBypass.setSelected(Loader.MTS_CONFIG.getBool("launcher.presets.bypass"));
    }
}
