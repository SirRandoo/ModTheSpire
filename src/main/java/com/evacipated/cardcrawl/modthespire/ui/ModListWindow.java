/*
  Created by SirRandoo
 */

package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.ConfigUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
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
    private static final String defaultPreset = ConfigUtils.CONFIG_DIR + File.separator + "default.mts";
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

    // Internals
    private File preset = null;
    private PresetTask presetTask = null;
    private boolean outJarRequested = false;


    public ModListWindow(ModInfo[] modInfos) {
        this.modInfos = modInfos;
        addWindowListener(this);

        SwingUtilities.invokeLater(() -> {
            initializeUI();
            validateUserPreferences();
        });

//
//        Enumeration<Object> ui = UIManager.getDefaults().keys();
//
//        while (ui.hasMoreElements()) {
//            System.out.println(ui.nextElement().toString());
//        }
    }

    /**
     * Returns the default properties the launcher uses
     * to provide basic functionality.
     */
    public static Properties getDefaults() {
        Properties defaults = new Properties();

        defaults.setProperty("launcher.position.x", "center");
        defaults.setProperty("launcher.position.y", "center");
        defaults.setProperty("launcher.geometry.width", Integer.toString(geometry.width));
        defaults.setProperty("launcher.geometry.height", Integer.toString(geometry.height));
        defaults.setProperty("launcher.maximized", Boolean.toString(false));
        defaults.setProperty("launcher.presets.last", defaultPreset);

        return defaults;
    }

    /**
     * Generates a default preset using the list the
     * loader generated.  This method does not attempt
     * to resolve load order.
     */
    private File getDefaultPreset() {
        File defaultPreset = new File(ModListWindow.defaultPreset);

        if (!defaultPreset.exists() && !Objects.isNull(Loader.MODINFOS)) {
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
        } else {
            if (!Objects.isNull(Loader.MODINFOS)) {
                System.out.println("Mod infos: " + Arrays.toString(Loader.MODINFOS));
            } else {
                System.out.println("Mod info is null!");
            }
        }

        return defaultPreset;
    }

    /**
     * Generates a preset properties object from the
     * current list of mods.  This method stores the
     * mod's current state and position to a Properties
     * object to use for saving.
     */
    private Properties generatePresetProperties() {
        Properties properties = new Properties();

        for (int index = 0; index < modList.getModel().getSize(); index++) {
            ComplexListItem item = modList.getModel().getElementAt(index);

            for (ModInfo modInfo : Loader.MODINFOS) {
                if (Objects.isNull(modInfo.ID)) continue;

                String displayName = modInfo.Name;

                if (Objects.isNull(displayName)) displayName = modInfo.ID;

                if (displayName.equalsIgnoreCase(item.getText())) {
                    properties.setProperty(modInfo.ID.toLowerCase() + ".enabled", Boolean.toString(item.getCheckState()));
                    properties.setProperty(modInfo.ID.toLowerCase() + ".position", Integer.toString(index));
                    break;
                }
            }
        }

        return properties;
    }

    /**
     * Initializes the launcher's ui.
     */
    private void initializeUI() {
        // Set the look and feel of the ui.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        // Instead of using EXIT_ON_CLOSE, we'll use HIDE_ON_CLOSE
        // to ensure our window listener events fire.
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setTitle("ModTheSpire");
        setResizable(true);

        initializeMenuBar();
        initializeStatusBar();

        // Add all the components to the launcher.
        getContentPane().add(menuBar, BorderLayout.NORTH);
        getContentPane().add(initializeModView(), BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        pack();

        // To ensure the launcher doesn't get shrunk too far below
        // the recommended size.
        setMinimumSize(new Dimension(geometry.width, geometry.height));

        // Move the launcher window to the position we have saved.
        if (isCentered) {
            setLocationRelativeTo(null);

        } else {
            setLocation(geometry.getLocation());
        }
    }

    /**
     * Initializes the menu bar.
     * <p>
     * The menu bar is the top-most panel of the launcher.
     * It houses the bulk of the launcher's actions, such
     * as opening the settings dialog.
     */
    private void initializeMenuBar() {
        menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");

        JMenuItem playAction = new JMenuItem("Play");
        playAction.addActionListener((ActionEvent event) -> startStS());

        JMenuItem loadPreset = new JMenuItem("Load Preset...");
        loadPreset.addActionListener((ActionEvent event) -> loadPreset());

        JMenuItem savePreset = new JMenuItem("Save Preset");
        savePreset.addActionListener((ActionEvent event) -> savePreset());

        JMenuItem saveAsPreset = new JMenuItem("Save Preset as...");
        saveAsPreset.addActionListener((ActionEvent event) -> {
            JFileChooser fileChooser = new JFileChooser(ConfigUtils.CONFIG_DIR);
            fileChooser.setFileFilter(new FileNameExtensionFilter("ModTheSpire Preset", "mts"));
            fileChooser.setSelectedFile(new File("customPreset.mts"));

            int result = fileChooser.showSaveDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                preset = fileChooser.getSelectedFile();
            }

            savePreset();
        });

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

    /**
     * Initializes the mod view.
     * <p>
     * The mod view is the central panel of the launcher.
     * Its used to display the current list of loaded mods,
     * and provide detailed information about them, should
     * the end-user request it.
     */
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
        modView.setPreferredSize(new Dimension(70, 300));

        if (modInfos.length > 0) {
            ModInfo first = modInfos[0];

            modView.setModName(first.ID, first.Name);
            modView.setDependencies(first.Dependencies);
            modView.setStsVersion(first.STS_Version);
            modView.setMtsVersion(first.MTS_Version != null ? first.MTS_Version.toString() : null);
            modView.setCredits(first.Credits);
            modView.setModVersion(first.ModVersion != null ? first.ModVersion.toString() : null);
        }

        modList.setVisible(true);

//        for (ModInfo mod : modInfos) modList.add(new ComplexListItem(mod.Name, null));

        listPanel.setLayout(new GridBagLayout());
        listPanel.setPreferredSize(new Dimension(30, 300));

        GridBagConstraints listConstraints = new GridBagConstraints();
        listConstraints.weightx = 1.0;
        listConstraints.weighty = 0.1;
        listConstraints.gridx = 0;
        listConstraints.gridy = 0;
        listConstraints.insets = new Insets(0, 0, 0, 0);
        listConstraints.fill = GridBagConstraints.HORIZONTAL;

        listPanel.add(presetLabel, listConstraints);

        listConstraints.gridy = 1;
        listConstraints.fill = GridBagConstraints.BOTH;
        listConstraints.weighty = 1.0;
        listPanel.add(modList, listConstraints);

        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        panel.setLayout(new GridBagLayout());

        GridBagConstraints layout = new GridBagConstraints();
        layout.weighty = 1.0;
        layout.weightx = 0.3;
        layout.gridx = 0;
        layout.insets = new Insets(0, 0, 0, 0);
        layout.fill = GridBagConstraints.BOTH;

        panel.add(listPanel, layout);

        layout.weightx = 0.7;
        layout.gridx = 1;
        panel.add(modView, layout);

        return panel;
    }

    /**
     * Initializes the status bar.
     * <p>
     * The status bar is the bottom-most panel of the launcher.
     * Its used to provide shortcut buttons to the end-user,
     * display in-app messages, and to provide real-time feedback
     * on certain actions via its progress bar.
     */
    private void initializeStatusBar() {
        statusBar = new AugmentedStatusBar();

        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, getBackground().darker()));
        statusBar.addPlayActionListener((ActionEvent event) -> startStS());
        statusBar.addDumpActionListener((ActionEvent event) -> {
            outJarRequested = true;
            Loader.OUT_JAR = true;
            startStS();

            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(new File(System.getProperty("user.dir")));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
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

    /**
     * Starts Slay the Spire with the selected preset.
     * <p>
     * If the end-user is using the beta version of Slay
     * the Spire, the launcher will display a prompt
     * notifying the user that mods may not work with the
     * beta version.
     */
    private void startStS() {
        // Ensure the loader isn't in dump mode
        if (Loader.OUT_JAR && !outJarRequested) {
            Loader.OUT_JAR = false;
        } else if (outJarRequested) {
            outJarRequested = false;
        }

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

    /**
     * Invoked when the end-user clicks the "Load Preset..."
     * action in the file menu.
     * <p>
     * This method is responsible for providing the user with
     * a file prompt to load a preset.
     */
    private void loadPreset() {
        JFileChooser fileChooser = new JFileChooser(ConfigUtils.CONFIG_DIR);
        fileChooser.setFileFilter(new FileNameExtensionFilter("ModTheSpire Preset", "mts"));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File preset = fileChooser.getSelectedFile();

            setPreset(preset);
            presetTask = new PresetTask(preset);
            presetTask.execute();
        }
    }

    /**
     * Invoked when the end-user clicks the "Save preset"
     * action in the file menu.
     * <p>
     * This method is responsible for providing the user with
     * a save prompt.  If the user selects a file, the launcher
     * will save the preset data to it.
     */
    private void savePreset() {
        new SaveFileTask(preset, generatePresetProperties()).execute();

        statusBar.showMessage("Preset saved!");  // TODO: Determine if this is accurate
    }

    /**
     * Updates the current preset the launcher is using.
     *
     * @param preset The new preset.
     */
    private void setPreset(File preset) {
        presetLabel.setText("Preset: " + preset.getName().substring(0, preset.getName().lastIndexOf('.')));
        this.preset = preset;
    }

    /**
     * Invoked when the end-user clicks the "Settings..."
     * action in the file menu.
     * <p>
     * This method is responsible for displaying the settings
     * dialog to the end-user.
     */
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

    /**
     * Invoked when the end-user clicks the "About"
     * action in the help menu.
     * <p>
     * This method is responsible for displaying a brief
     * overview of ModTheSpire, among other metadata.
     */
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
        AboutWindow about = new AboutWindow(this);

        try {
            about.setContents(contents.toString());

            if (icon != null) about.setIcon(icon);
            if (!about.isVisible()) about.setVisible(true);

        } catch (HeadlessException e) {
            System.out.println("Cannot display about dialog in a headless environment!");
            e.printStackTrace();
        } finally {
            about.dispose();
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

        if (preset.exists()) {
            presetTask = new PresetTask(preset);
            presetTask.execute();
        } else {
            presetTask = new PresetTask(getDefaultPreset());
            presetTask.execute();
        }
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
     * A background presetTask for updating the display to reflect the
     * end-user's selected preset.
     */
    private class PresetTask extends SwingWorker<Void, Void> {
        private File target;

        PresetTask(File target) {
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

                //noinspection unchecked
                Collections.sort(newModList);
            }

            SwingUtilities.invokeLater(() -> {
                modList.removeAll();
                newModList.forEach(item -> modList.add(item.item));
                modList.revalidate();
            });

            return null;
        }

        @Override
        protected void done() {
            setPreset(target);
        }
    }

    /**
     * A utility class for housing comparable mod items.
     */
    private class PresetItem implements Comparable {
        private ComplexListItem item;
        private int position;

        PresetItem(ComplexListItem item, int position) {
            this.item = item;
            this.position = position;
        }

        @Override
        public int compareTo(Object o) {
            return Integer.compare(this.position, ((PresetItem) o).position);
        }
    }

    /**
     * Saves a file (hopefully) away from the Swing threads.
     */
    private class SaveFileTask extends SwingWorker<Void, Void> {
        private File target;
        private Object data;

        SaveFileTask(File target, Object data) {
            this.target = target;
            this.data = data;
        }

        /**
         * Saves the data to the requested file location.
         *
         * @throws IOException The file could not be saved for
         *                     some reason, such as a permission
         *                     error.
         */
        @Override
        protected Void doInBackground() throws Exception {
            if (!Objects.isNull(data)) {
                FileWriter fileWriter = new FileWriter(target);

                if (!Objects.isNull(data) && data instanceof Properties) {
                    ((Properties) data).store(fileWriter, "");
                }

                fileWriter.flush();
                fileWriter.close();
            }

            return null;
        }
    }
}
