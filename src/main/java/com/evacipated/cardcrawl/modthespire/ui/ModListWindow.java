/*
  Created by SirRandoo
 */

package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.ConfigUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * The main launcher window.
 * <p>
 * This window is responsible for handling user related logic,
 * like loading the user's desired preset.
 * <p>
 * The window is divided into 3 sections: the menu bar, the
 * content panel, and the status bar.
 * <p>
 * The menu bar houses top-level ui tasks, such as loading presets.
 * The content panel houses the discovered mod list, and the
 * information panel about a selected mod.  The status bar houses
 * immediate information display and quality of life shortcuts,
 * such as a play button.
 */
public class ModListWindow extends JFrame implements WindowListener {
    private static Rectangle geometry = new Rectangle(0, 0, 800, 500);
    private ModInfo[] modInfos;

    // Top-down UI elements.
    private JMenuBar menuBar;
    private ModListComponent modList;
    private ModView modView;
    private JTextArea presetLabel;
    private AugmentedStatusBar statusBar;

    // Dialogs
    private SettingsWindow settingsWindow;

    // Persistence
    private boolean isMaximized;
    private boolean isCentered;
    private File preset = null;
    private PresetTask task = null;


    public ModListWindow(ModInfo[] modInfos) {
        try {
            // Attempt to tailor the window's UI to the user's operating system's.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        this.modInfos = modInfos;
        initializeUI();
        validateUserPreferences();

        addWindowListener(this);

        Enumeration<Object> ui = UIManager.getDefaults().keys();

        while (ui.hasMoreElements()) {
            System.out.println(ui.nextElement().toString());
        }
    }

    public static Properties getDefaults() {
        Properties defaults = new Properties();

        defaults.setProperty("launcher.position.x", "center");
        defaults.setProperty("launcher.position.y", "center");
        defaults.setProperty("launcher.geometry.width", Integer.toString(geometry.width));
        defaults.setProperty("launcher.geometry.height", Integer.toString(geometry.height));
        defaults.setProperty("launcher.maximized", Boolean.toString(false));
        defaults.setProperty("launcher.presets.last", "default");

        return defaults;
    }

    public static File getDefaultPreset() {
        File defaultPreset = new File(ConfigUtils.CONFIG_DIR + File.separator + "default.mts");

        if (!defaultPreset.exists()) {
            new Thread(() -> {
                try {
                    FileWriter fileWriter = new FileWriter(defaultPreset);
                    Properties properties = new Properties();

                    for (ModInfo modInfo : Loader.MODINFOS) {
                        if (Objects.isNull(modInfo.ID)) continue;

                        properties.setProperty(modInfo.ID + ".enabled", Boolean.toString(false));
                        properties.setProperty(modInfo.ID + ".position", Integer.toString(properties.size() / 2));
                    }

                    properties.store(fileWriter, "The default ModTheSpire preset.");

                } catch (IOException e) {
                    System.out.println("Could not create default.mts file!  (" + e.toString() + ")");
                }
            }).start();
        }

        return defaultPreset;
    }

    private void initializeUI() {
        setTitle("ModTheSpire (v" + Loader.MTS_VERSION + ")");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setResizable(true);

        initializeMenuBar();
        initializeStatusBar();

        statusBar.setOpaque(true);

        getContentPane().add(menuBar, BorderLayout.NORTH);
        getContentPane().add(initializeModView(), BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        pack();
        setMinimumSize(new Dimension(geometry.width, geometry.height));

        if (isCentered) {
            setLocationRelativeTo(null);

        } else {
            setLocation(geometry.getLocation());
        }
    }

    private void initializeMenuBar() {
        menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem playAction = new JMenuItem("Play");
        playAction.addActionListener((ActionEvent event) -> startStS());

        JMenuItem loadPreset = new JMenuItem("Load Preset...");
        loadPreset.addActionListener((ActionEvent event) -> loadPreset());

        JMenuItem savePreset = new JMenuItem("Save Preset");
        savePreset.addActionListener((ActionEvent event) -> savePreset(true));

        JMenuItem saveAsPreset = new JMenuItem("Save Preset as...");
        saveAsPreset.addActionListener((ActionEvent event) -> savePreset());

        JMenuItem settingsAction = new JMenuItem("Settings..");
        settingsAction.addActionListener((ActionEvent event) -> openSettings());

        JMenuItem exitAction = new JMenuItem("Exit");
        exitAction.addActionListener((ActionEvent event) -> this.dispose());

        fileMenu.add(playAction);
        fileMenu.addSeparator();

        fileMenu.add(loadPreset);
        fileMenu.add(savePreset);
        fileMenu.add(saveAsPreset);
        fileMenu.addSeparator();

        fileMenu.add(settingsAction);
        fileMenu.addSeparator();

        fileMenu.add(exitAction);

        // Help menu
        JMenu helpMenu = new JMenu("Help");

        JMenuItem helpAction = new JMenuItem("Help");
        helpAction.addActionListener((ActionEvent event) -> showHelp());

        JMenuItem aboutAction = new JMenuItem("About");
        aboutAction.addActionListener((ActionEvent event) -> showAbout());

        helpMenu.add(helpAction);
        helpMenu.add(aboutAction);

        // Insert the menus to the menu bar
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
    }

    private JPanel initializeModView() {
        JPanel panel = new JPanel();
        JPanel listPanel = new JPanel();

        // Create the mod view elements
        modList = new ModListComponent();
        modView = new ModView();
        presetLabel = new JTextArea();

        presetLabel.setText("Preset: default");
        presetLabel.setWrapStyleWord(true);
        presetLabel.setLineWrap(true);
        presetLabel.setEditable(false);
        presetLabel.setAutoscrolls(false);
        presetLabel.setOpaque(false);
        presetLabel.setFont(modList.getFont());

        modView.setBorder(new EmptyBorder(0, 10, 0, 0));
        modView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        modView.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        if (modInfos.length > 0) {
            ModInfo first = modInfos[0];

            modView.setModName(first.ID, first.Name);
            modView.setDependencies(first.Dependencies);
            modView.setStsVersion(first.STS_Version);
            modView.setMtsVersion(first.MTS_Version != null ? first.MTS_Version.toString() : null);
            modView.setCredits(first.Credits);
            modView.setModVersion(first.ModVersion != null ? first.ModVersion.toString() : null);
        }

        modList.setDragEnabled(true);
        modList.setDropMode(DropMode.INSERT);
        modList.setOpaque(true);
        modList.setVisible(true);

//        for (ModInfo mod : modInfos) modList.add(new ComplexListItem(mod.Name, null));

        listPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.add(presetLabel);
        listPanel.add(modList);

        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(listPanel);
        panel.add(modView);

        return panel;
    }

    private void initializeStatusBar() {
        statusBar = new AugmentedStatusBar();

        statusBar.addPlayActionListener((ActionEvent event) -> startStS());
    }

    /**
     * Validates the end-user's window preferences.  If the user's preferences
     * cannot be honored, they'll be reset to their default values to ensure
     * the best possible user experience.
     * <p>
     * Window preferences are automatically saved when the user resizes the
     * window, or moves the window.
     */
    private void validateUserPreferences() {
        String posX;
        String posY;
        int width;
        int height;

        // Migrate old preferences
        /// Migrate the X property into launcher.position.x
        /// If it doesn't exist in the user's property file, we'll load
        /// the new format instead.
        if (Loader.MTS_CONFIG.has("x")) {
            System.out.println("Migrating x -> launcher.position.x ...");

            posX = Loader.MTS_CONFIG.getString("x");

            Loader.MTS_CONFIG.setString("launcher.position.x", posX);
            Loader.MTS_CONFIG.remove("x");

        } else {
            posX = Loader.MTS_CONFIG.getString("launcher.position.x");
        }

        // Migrate the Y property into launcher.position.y
        // If it doesn't exist in the user's property file, we'll load the
        // new format instead.
        if (Loader.MTS_CONFIG.has("y")) {
            System.out.println("Migrating y -> launcher.position.y ...");

            posY = Loader.MTS_CONFIG.getString("y");

            Loader.MTS_CONFIG.setString("launcher.position.y", posY);
            Loader.MTS_CONFIG.remove("y");

        } else {
            posY = Loader.MTS_CONFIG.getString("launcher.position.y");
        }

        // Migrate the width property into launcher.geometry.width
        // If it doesn't exist in the user's property file, we'll load the
        // new format instead.
        if (Loader.MTS_CONFIG.has("width")) {
            System.out.println("Migrating width -> launcher.geometry.width ...");

            width = Loader.MTS_CONFIG.getInt("width");

            Loader.MTS_CONFIG.setInt("launcher.geometry.width", width);
            Loader.MTS_CONFIG.remove("width");

        } else {
            width = Loader.MTS_CONFIG.getInt("launcher.geometry.width");
        }

        // Migrate the height property into launcher.geometry.height
        // If it doesn't exist in the user's property file, we'll load the
        // new format instead.
        if (Loader.MTS_CONFIG.has("height")) {
            System.out.println("Migrating height -> launcher.geometry.height ...");

            height = Loader.MTS_CONFIG.getInt("height");

            Loader.MTS_CONFIG.setInt("launcher.geometry.height", height);
            Loader.MTS_CONFIG.remove("height");

        } else {
            height = Loader.MTS_CONFIG.getInt("launcher.geometry.height");
        }

        // Migrate the maximized property into launcher.maximized
        // If it doesn't exist in the user's property file, we'll load the
        // new format instead.
        if (Loader.MTS_CONFIG.has("maximized")) {
            System.out.println("Migrating maximized -> launcher.maximized ...");

            isMaximized = Loader.MTS_CONFIG.getBool("maximized");

            Loader.MTS_CONFIG.setBool("maximized", isMaximized);
            Loader.MTS_CONFIG.remove("maximized");

        } else {
            isMaximized = Loader.MTS_CONFIG.getBool("launcher.maximized");
        }

        // Ensure the user's width & height don't fall below the minimum height.
        if (height < getMinimumSize().height)
            Loader.MTS_CONFIG.setInt("launcher.geometry.height", getMinimumSize().height);
        if (width < getMinimumSize().width) Loader.MTS_CONFIG.setInt("launcher.geometry.width", getMinimumSize().width);

        // If either X or Y property is set to "center", we'll position the launcher
        // in the center of the screen later.
        //
        // If it isn't, we'll verify that the position is within the user's screen.
        // If it isn't, we'll reset them to their default values (center).
        if (!posX.equalsIgnoreCase("center") && !posY.equalsIgnoreCase("center")) {
            geometry.x = Integer.parseInt(posX);
            geometry.y = Integer.parseInt(posY);

            if (!isValidLocation(geometry)) {
                geometry.x = 0;
                geometry.y = 0;

                Loader.MTS_CONFIG.setString("launcher.position.x", "center");
                Loader.MTS_CONFIG.setString("launcher.position.y", "center");

                isCentered = true;

            } else {
                isCentered = false;
            }
        } else {
            isCentered = true;
        }

        // Save any changes we may have made to the config.
        try {
            Loader.MTS_CONFIG.save();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startStS() {
        // If the user launches Slay the Spire, but they don't allow ModTheSpire
        // to run on the beta version, we'll display a prompt asking the user if
        // they would like to continue.  If they don't want to, the dialog will
        // close, and the launch sequence will be aborted.
        if (Loader.STS_BETA && !Loader.allowBeta) {
            int result = JOptionPane.showConfirmDialog(
                this,
                "You're running the beta version of Slay the Spire, but your settings don't allow ModTheSpire to run on the beta version.  <b>Some mods may not work correctly.<b><br/><br/>Do you want ModTheSpire to run anyway?",
                this.getTitle(),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.YES_OPTION) return;
        }

        // Launch Slay the Spire.
        Thread t = new Thread(() -> {
            // Only load the user's selected mods.
            File[] selectedMods = {};

            Loader.runMods(selectedMods);
        });

        t.start();
    }

    private void loadPreset() {
    }

    private void savePreset(File preset) {
    }

    private void savePreset(boolean sameFile) {
    }

    private void savePreset() {
    }

    private void openSettings() {
        if (settingsWindow == null) settingsWindow = new SettingsWindow(this);

        settingsWindow.setVisible(true);
    }

    /**
     * Opens the end-user's browser to ModTheSpire's wiki.
     * <p>
     * TODO: Add a brief overview of the new launcher.
     */
    private void showHelp() {
        System.out.println("User requested help.  Currently, there are no help files in-app, so we'll just open their browser.");

        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/kiooeht/ModTheSpire/wiki"));
            } catch (URISyntaxException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAbout() {
        StringBuilder contents = new StringBuilder();
        ImageIcon icon = null;

        contents.append("<html>");
        contents.append("<p>");
        contents.append("<div align=\"center\" style=\"font-weight: bold;\">");
        contents.append("ModTheSpire is a tool to load external mods for Slay the Spire without modifying the base game files.");
        contents.append("</div>");
        contents.append("<hr width=\"100%\"/><br/>");
        contents.append("</p>");
        contents.append("<p>");
        contents.append("<center>This software is licensed under the MIT license.");
        contents.append("You're free to modify and/or redistribute it under the conditions of the license(s).").append("</center>");
        contents.append("<br/><br/>");
        contents.append("ModTheSpire Version: ").append(Loader.MTS_VERSION != null ? Loader.MTS_VERSION.toString() : "UNKNOWN").append("<br/>");
        contents.append("Slay the Spire Version: ").append(Loader.STS_VERSION != null ? Loader.STS_VERSION : "UNKNOWN").append("<br/>");
        contents.append("Slay the Spire Channel: <b>").append(Loader.STS_BETA ? "Beta" : "Release").append("</b>");
        contents.append("</p>");
        contents.append("<html>");

        if (this.getIconImage() != null) icon = new ImageIcon(this.getIconImage());

        try {
            AboutWindow about = new AboutWindow(this);
            about.setContents(contents.toString());

            if (icon != null) about.setIcon(icon);
            if (!about.isVisible()) about.setVisible(true);

        } catch (HeadlessException e) {
            System.out.println("Cannot display about dialog in a headless environment!");
            e.printStackTrace();
        }
    }

    /**
     * Returns whether or not a location is valid on the end-user's display.
     *
     * @param location The location to validate
     */
    private boolean isValidLocation(Rectangle location) {
        for (GraphicsDevice graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle bounds = graphicsDevice.getDefaultConfiguration().getBounds();

            // Expand screen bounds slightly
            bounds.x -= 10;
            bounds.width += 20;
            bounds.y -= 10;
            bounds.height += 20;

            if (bounds.contains(location)) {
                return true;
            }
        }

        return false;
    }

    private boolean isValidLocation(Point location, Rectangle size) {
        size.setLocation(location);

        return isValidLocation(size);
    }

    public void setModInfo(ModInfo info) {
        if (info == null) {
            modView.setViewVisible(false);
        } else {
            modView.setViewVisible(true);
            modView.setModVersion(info.ModVersion != null ? info.ModVersion.toString() : null);
            modView.setModName(info.ID, info.Name);
            modView.setStsVersion(info.STS_Version);
            modView.setMtsVersion(info.MTS_Version != null ? info.MTS_Version.toString() : null);
            modView.setDependencies(info.Dependencies);
            modView.setCredits(info.Credits);
            modView.setDescription(info.Description);
            modView.setAuthors(info.Authors);
        }
    }

    /**
     * Invoked the first time the launcher window is made visible.
     * <p>
     * This override is responsible for loading the user's last known preset.
     */
    @Override
    public void windowOpened(WindowEvent e) {
        String lastPreset = Loader.MTS_CONFIG.getString("launcher.presets.last");

        if (Objects.isNull(lastPreset)) lastPreset = getDefaultPreset().toString();

        preset = new File(lastPreset);
        task = new PresetTask(preset);
        task.execute();
    }

    /**
     * Invoked when the launcher window is about to close.
     * <p>
     * This method is responsible for saving user preferences.
     */
    @Override
    public void windowClosing(WindowEvent event) {
        System.out.println("Performing closing operations...");
        Window window = event.getWindow();

        if (Loader.DEBUG) System.out.println("Saving window geometry...");
        Loader.MTS_CONFIG.setInt("launcher.geometry.width", window.getSize().width);
        Loader.MTS_CONFIG.setInt("launcher.geometry.height", window.getSize().height);
        Loader.MTS_CONFIG.setBool("launcher.maximized", isMaximized);

        if (Loader.DEBUG) System.out.println("Saving window position...");
        if (!isCentered) {
            Loader.MTS_CONFIG.setInt("launcher.position.x", window.getX());
            Loader.MTS_CONFIG.setInt("launcher.position.y", window.getY());
        } else {
            Loader.MTS_CONFIG.setString("launcher.position.x", "center");
            Loader.MTS_CONFIG.setString("launcher.position.y", "center");
        }

        if (Loader.DEBUG) System.out.println("Saving preset information...");
        Loader.MTS_CONFIG.setString("launcher.presets.last", Objects.isNull(preset) ? getDefaultPreset().toString() : preset.toString());

        if (Loader.DEBUG) System.out.println("Saving user preferences...");
        try {
            Loader.MTS_CONFIG.save();
        } catch (IOException e) {
            System.out.println("Could not save user preferences!");
            e.printStackTrace();
        }

        // Clean up the launcher's ui elements
        this.dispose();
    }

    /**
     * Invoked when the launcher window is closed.
     */
    @Override
    public void windowClosed(WindowEvent e) {
        // Ignored
    }

    /**
     * Invoked when the launcher window is iconified.
     */
    @Override
    public void windowIconified(WindowEvent e) {
        // Ignored
    }

    /**
     * Invoked when a window is changed from a minimized
     * to a normal state.
     */
    @Override
    public void windowDeiconified(WindowEvent e) {
        // Ignored
    }

    /**
     * Invoked when the Window is set to be the active Window.  Only
     * a Frame or a Dialog can be the active Window.  The native windowing
     * system may denote the active Window or its children with special
     * decorations, such as a highlighted title bar.  The active Window is
     * always either the focused Window, or the first Frame or Dialog that
     * is an owner of the focused Window.
     */
    @Override
    public void windowActivated(WindowEvent e) {
        // Ignored
    }

    /**
     * Invoked when a Window is no longer the active Window. Only a Frame or a
     * Dialog can be the active Window. The native windowing system may denote
     * the active Window or its children with special decorations, such as a
     * highlighted title bar. The active Window is always either the focused
     * Window, or the first Frame or Dialog that is an owner of the focused
     * Window.
     */
    @Override
    public void windowDeactivated(WindowEvent e) {
        // Ignored
    }

    /**
     * A background task for updating the display to reflect the
     * end-user's selected preset.
     */
    private class PresetTask extends SwingWorker<Void, ModListWindow> {
        private File target;

        public PresetTask(File target) {
            this.target = target;
        }

        /**
         * Opens the end-user's requested preset, and updates the
         * mod list to reflect the preset's values.
         */
        @Override
        protected Void doInBackground() throws Exception {
            // If the target is null for some reason, throw an IOException.
            if (Objects.isNull(target)) throw new IOException("Preset file cannot be null!");

            // Some declarations
            FileReader fileReader = new FileReader(target);
            Properties preset = new Properties();
            ArrayList<PresetItem> newModList = new ArrayList<>();

            // Load the preset properties.
            preset.load(fileReader);

            // Sort the mod list.
            for (ModInfo modInfo : Loader.MODINFOS) {
                // If a mod doesn't have an ID, the mod will be omitted.
                // Omitted mods won't be displayed.
                if (Objects.isNull(modInfo.ID)) {
                    System.out.println("Mod @ " + modInfo.jarURL.toString() + " has a null ID!");
                    continue;
                }

                String modPositionRaw = preset.getProperty(modInfo.ID + ".position");
                String modEnabledRaw = preset.getProperty(modInfo.ID + ".enabled");

                int modPosition = -1;
                boolean modEnabled = false;

                // Cast the raw values
                if (modPositionRaw != null) modPosition = Integer.parseInt(modPositionRaw);
                if (modEnabledRaw != null) modEnabled = Boolean.parseBoolean(modEnabledRaw);

                ComplexListItem item = new ComplexListItem();

                item.setText(Objects.isNull(modInfo.Name) ? modInfo.ID : modInfo.Name);
                item.setCheckState(modEnabled);

                PresetItem presetItem = new PresetItem(item, modPosition == -1 ? 500 + newModList.size() : modPosition);
                newModList.add(presetItem);

                Collections.sort(newModList);
            }

            modList.removeAll();

            for (PresetItem presetItem : newModList) {
                modList.add(presetItem.item);
            }

            return null;
        }
    }

    private class PresetItem implements Comparable {
        private ComplexListItem item;
        private int position;

        public PresetItem(ComplexListItem item, int position) {
            this.item = item;
            this.position = position;
        }

        @Override
        public int compareTo(Object o) {
            return Integer.compare(this.position, ((PresetItem) o).position);
        }
    }
}
