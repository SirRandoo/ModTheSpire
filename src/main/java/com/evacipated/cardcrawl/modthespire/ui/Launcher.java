package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.GameBetaFinder;
import com.evacipated.cardcrawl.modthespire.GameVersionFinder;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.ConfigUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.steam.SteamSearch;
import com.evacipated.cardcrawl.modthespire.steam.SteamWorkshop;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.objectweb.asm.ClassReader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

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
public class Launcher extends JFrame implements WindowListener {
    private static final Path defaultPreset = ConfigUtils.CONFIG_DIR.resolve("default.mts");
    private static Rectangle geometry = new Rectangle(0, 0, 800, 500);
    private static Launcher instance;
    private List<ModInfo> modInfos = new ArrayList<>();
    private HashMap<String, ModInfo> mappedInfos = new HashMap<>();

    // Top-down UI elements.
    private JMenuBar menuBar;
    private JTextArea presetLabel;
    private ModListComponent modList;
    private ModView modView;
    private JButton enableAll;
    private JButton disableAll;
    private AugmentedStatusBar statusBar;

    // Dialogs
    private SettingsWindow settingsWindow;

    // Persistence
    private boolean isMaximized;
    private boolean isCentered;

    // Internals
    private File preset = null;
    private DefaultListModel<ComplexListItem> listModel;
    private boolean outJarRequested = false;
    private boolean shouldBypass = true;


    private Launcher() {
        addWindowListener(this);

        SwingUtilities.invokeLater(() -> {
            initializeUI();
            validateUserPreferences();
        });
    }

    /**
     * Returns the default properties the launcher uses
     * to provide basic functionality.
     */
    public static Properties getDefaults() {
        Properties defaults = new Properties();

        defaults.setProperty("isBeta", Boolean.toString(true));
        defaults.setProperty("launcher.position.x", "center");
        defaults.setProperty("launcher.position.y", "center");
        defaults.setProperty("launcher.geometry.width", Integer.toString(geometry.width));
        defaults.setProperty("launcher.geometry.height", Integer.toString(geometry.height));
        defaults.setProperty("launcher.maximized", Boolean.toString(false));
        defaults.setProperty("launcher.presets.last", defaultPreset.toString());
        defaults.setProperty("launcher.presets.bypass", Boolean.toString(false));

        return defaults;
    }

    /**
     * Returns the current instance of the Launcher class.
     * If one hasn't been created, it will create it.
     */
    public static Launcher getInstance() {
        if (instance == null) instance = new Launcher();

        return instance;
    }

    /**
     * Generates a default preset using the list the
     * loader generated.  This method does not attempt
     * to resolve load order.
     */
    private File getDefaultPreset() {
        File defaultPreset = Launcher.defaultPreset.toFile();

        if (!defaultPreset.exists()) {
            new Thread(() -> {
                try {
                    FileWriter fileWriter = new FileWriter(defaultPreset);
                    Properties properties = new Properties();

                    for (ModInfo modInfo : modInfos) {
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

    /**
     * Generates a preset properties object from the
     * current list of mods.  This method stores the
     * mod's current state and position to a Properties
     * object to use for saving.
     */
    private Properties generatePresetProperties() {
        Properties properties = new Properties();

        for (int index = 0; index < listModel.getSize(); index++) {
            ComplexListItem item = listModel.getElementAt(index);

            for (ModInfo modInfo : modInfos) {
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
     * Returns the current instance of the Launcher class.
     * If one hasn't been created, it will create it.
     */
    public static Launcher getInstance() {
        if (instance == null) instance = new Launcher();

        return instance;
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
            JFileChooser fileChooser = new JFileChooser(ConfigUtils.CONFIG_DIR.toFile());
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

        JMenuItem updateAction = new JMenuItem("Check for updates...");
        updateAction.addActionListener((ActionEvent event) -> checkForUpdates());

        helpMenu.add(helpAction);
        helpMenu.addSeparator();
        helpMenu.add(updateAction);
        helpMenu.add(aboutAction);

        // Insert the menus to the menu bar
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
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
        getContentPane().add(initializeCentralUI(), BorderLayout.CENTER);
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
        listModel = new DefaultListModel<>();
        modList = new ModListComponent(listModel);
        modView = new ModView();
        presetLabel = new JTextArea();
        enableAll = new JButton("Enable All");
        disableAll = new JButton("Disable All");

        enableAll.addActionListener((ActionEvent event) -> {
            for (int index = 0; index < listModel.getSize(); index++) {
                listModel.elementAt(index).setCheckState(true);
            }

            modList.repaint();
        });

        disableAll.addActionListener((ActionEvent event) -> {
            for (int index = 0; index < listModel.getSize(); index++) {
                listModel.getElementAt(index).setCheckState(false);
            }

            modList.repaint();
        });

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

        if (modInfos.size() > 0) {
            ModInfo first = modInfos.get(0);

            modView.setModName(first.ID, first.Name);
            modView.setDependencies(first.Dependencies);
            modView.setStsVersion(first.STS_Version);
            modView.setMtsVersion(first.MTS_Version != null ? first.MTS_Version.toString() : null);
            modView.setCredits(first.Credits);
            modView.setModVersion(first.ModVersion != null ? first.ModVersion.toString() : null);
        }

        modList.setVisible(true);

        listPanel.setLayout(new GridBagLayout());
        listPanel.setPreferredSize(new Dimension(30, 300));

        GridBagConstraints listConstraints = new GridBagConstraints();
        listConstraints.weightx = 1.0;
        listConstraints.weighty = 0.1;
        listConstraints.gridx = 0;
        listConstraints.gridy = 0;
        listConstraints.insets = new Insets(0, 0, 0, 0);
        listConstraints.gridwidth = 2;
        listConstraints.fill = GridBagConstraints.HORIZONTAL;

        listPanel.add(presetLabel, listConstraints);

        listConstraints.gridy++;
        listConstraints.fill = GridBagConstraints.BOTH;
        listConstraints.weighty = 0.8;
        listPanel.add(modList, listConstraints);

        listConstraints.gridy++;
        listConstraints.gridwidth = 1;
        listConstraints.weighty = 0.1;
        listConstraints.fill = GridBagConstraints.HORIZONTAL;
        listPanel.add(enableAll, listConstraints);

        listConstraints.gridx++;
        listPanel.add(disableAll, listConstraints);

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

        statusModFolder = new StatusButton(new ImageIcon(this.getClass().getResource("/assets/folder-icon.png")), "Opens the mods folder for ModTheSpire.");
        statusModFolder.addActionListener((ActionEvent event) -> {
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
                showStatusMessage(e.getMessage(), 2);

                e.printStackTrace();
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
                this.getTitle() + " - Beta Confirmation",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.PLAIN_MESSAGE
            );

            if (result != JOptionPane.YES_OPTION) return;
        }

        // Launch Slay the Spire with the user's selected mods.
//        int size = (int) IntStream.range(0, listModel.getSize()).filter(i -> listModel.getElementAt(i).getCheckState()).count();
//        File[] selectedMods = new File[size];
//
//        for (int i = 0; i < listModel.getSize(); i++) {
//            ComplexListItem item = listModel.getElementAt(i);
//
//            for (ModInfo modInfo : modInfos) {
//                if (Objects.isNull(modInfo.ID)) continue;
//
//                if (item.getText().equalsIgnoreCase(Objects.isNull(modInfo.Name) ? modInfo.ID : modInfo.Name)) {
//                    try {
//                        selectedMods[i] = new File(modInfo.jarURL.toURI());
//                    } catch (URISyntaxException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }

        Thread t = new Thread(() -> {
            // Only load the user's selected mods.
            int size = 0;

            for (int i = 0; i < listModel.getSize(); i++) {
                if (listModel.getElementAt(i).getCheckState()) {
                    size++;
                }
            }

            File[] selectedMods = new File[size];

            for (int index = 0; index < listModel.getSize(); index++) {
                ComplexListItem item = listModel.getElementAt(index);

                for (ModInfo modInfo : modInfos) {
                    if (Objects.isNull(modInfo.ID)) continue;

                    if (item.getText().equalsIgnoreCase(Objects.isNull(modInfo.Name) ? modInfo.ID : modInfo.Name)) {
                        try {
                            selectedMods[index] = new File(modInfo.jarURL.toURI());
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

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
        JFileChooser fileChooser = new JFileChooser(ConfigUtils.CONFIG_DIR.toFile());
        fileChooser.setFileFilter(new FileNameExtensionFilter("ModTheSpire Preset", "mts"));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File preset = fileChooser.getSelectedFile();

            // Load the user's selected preset.
            try {
                (new PresetLoadTask(preset)).execute();
            } catch (IOException e) {
                System.out.println("Could not load preset @ " + preset.toString());
            }
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
        new PresetSaveTask(preset, generatePresetProperties()).execute();
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

    public void showStatusMessage(String message) {
        showStatusMessage(message, 5);
    }

    public void showStatusMessage(String message, int duration) {
        if (Loader.DEBUG) System.out.println("Status bar > " + message);
        statusBar.showMessage(message, duration);
    }

    /**
     * Returns whether or not a mod matching the specified id was discovered.
     *
     * @param modId The ID to check for.
     */
    public boolean wasModDiscovered(String modId) {
        return mappedInfos.containsKey(modId);
    }

    /**
     * Returns the current list of discovered mods.
     */
    public List<ModInfo> getDiscoveredMods() {
        return modInfos;
    }

    /**
     * Invoked the first time the launcher window is made visible.
     * <p>
     * This override is responsible for invoking the mod discovery process,
     * then loading the last known preset.
     */
    @Override
    public void windowOpened(WindowEvent e) {
        // Start the mod discovery
        ModDiscoveryTask discoveryTask = new ModDiscoveryTask();

        discoveryTask.addPropertyChangeListener(evt -> {
            if (evt.getPropertyName().equalsIgnoreCase("state") && evt.getNewValue() == SwingWorker.StateValue.DONE) {
                String lastPreset = Loader.MTS_CONFIG.getString("launcher.presets.last");

                if (Objects.isNull(lastPreset)) lastPreset = getDefaultPreset().toString();

                preset = new File(lastPreset);

                try {
                    (new PresetLoadTask(preset.exists() ? preset : getDefaultPreset())).execute();
                } catch (IOException e1) {
                    System.out.println("Could not load last known preset!  This should never happen.");
                }
            }
        });

        discoveryTask.execute();
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
     * A background task for loading the end-user's requested preset.
     * When the preset is loaded, the launcher's display will be updated
     * to reflect the changes.
     */
    private class PresetLoadTask extends SwingWorker<ArrayList<PresetItem>, Void> {
        private File target;

        PresetLoadTask(File target) throws IOException {
            if (!target.exists()) throw new IOException();

            this.target = target;
        }

        /**
         * Opens the end-user's requested preset, sorts the preset's
         * mods according to their saved order, and appends the any
         * new mods that may have been added.
         *
         * @throws PresetLoadException The preset could not be loaded.
         */
        @Override
        protected ArrayList<PresetItem> doInBackground() throws PresetLoadException {
            try (FileReader fileReader = new FileReader(target)) {
                // Some declarations
                Properties preset = new Properties();
                ArrayList<PresetItem> newModList = new ArrayList<>();

                // Load the preset properties.
                preset.load(fileReader);

                // Sort the mod list.
                for (ModInfo modInfo : modInfos) {
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

                    PresetItem presetItem = new PresetItem(
                        Objects.isNull(modInfo.Name) ? modInfo.ID : modInfo.Name,
                        modEnabled,
                        modPosition == -1 ? 500 + newModList.size() : modPosition
                    );

                    newModList.add(presetItem);
                }

                //noinspection unchecked
                Collections.sort(newModList);

                return newModList;

            } catch (IOException e) {
                e.printStackTrace();
            }

            throw new PresetLoadException();
        }

        /**
         * Updates the launcher's mod list to reflect the loaded preset.
         * <p>
         * If this is the first preset loaded and the user enabled bypass,
         * the launcher will automatically start Slay the Spire.
         */
        @Override
        protected void done() {
            // Update the preset label to reflect the loaded preset.
            setPreset(target);

            try {
                // Get the new mod list returned from doInBackground.
                ArrayList<PresetItem> newModList = get();

                // Remove any list items in the mod list
                listModel.removeAllElements();

                // Add the new items
                for (PresetItem item : newModList) {
                    ModListItem i = new ModListItem()

                    PresetItem component = new CheckableListItem(item.name, null);
                    component.setCheckState(item.enabled);

                    listModel.addElement(component);
                }

                // If the user has enabled the launcher bypass option,
                // we'll start Slay the Spire.  This will ONLY start
                // the game if this is the FIRST preset loaded.
                if (Loader.MTS_CONFIG.has("launcher.presets.bypass")) {
                    if (Loader.MTS_CONFIG.getBool("launcher.presets.bypass") && shouldBypass) {
                        shouldBypass = false;

                        System.out.println("Automatically starting Slay the Spire with preset \"" + preset.getName().substring(0, preset.getName().lastIndexOf('.')) + "\"");
                        startStS();
                    }
                }

            } catch (InterruptedException | ExecutionException e) {
                statusBar.showMessage("Could not load preset \"" + target.getName().substring(0, target.getName().lastIndexOf('.')) + "\"");
                e.printStackTrace();
            }
        }
    }

    /**
     * A background task for saving the end-user's current preset.
     * When the preset is saved, the launcher's status bar will be updated
     * to ensure the user knows its been saved.
     */
    private class PresetSaveTask extends SwingWorker<Void, Void> {
        private File target;
        private Properties data;

        PresetSaveTask(File target, Properties data) {
            this.target = target;
            this.data = data;
        }

        /**
         * Saves the end-user's current preset.
         *
         * @throws IOException The file could not be written to.
         */
        @Override
        protected Void doInBackground() throws IOException {
            try (FileWriter fileWriter = new FileWriter(target)) {
                data.store(fileWriter, "ModTheSpire preset");

                fileWriter.flush();
            }

            return null;
        }

        /**
         * When the preset is finished saving, this method
         * is responsible for informing the user that their
         * changes were recorded to disk.
         */
        @Override
        protected void done() {
            statusBar.showMessage("Saved preset \"" + target.getName().substring(0, target.getName().lastIndexOf('.')) + "\"");
        }
    }

    /**
     * A utility class for housing comparable mod items.
     */
    private class PresetItem implements Comparable {
        private String name;
        private boolean enabled;
        private int position;

        PresetItem(String name, boolean enabled, int position) {
            this.name = name;
            this.enabled = enabled;
            this.position = position;
        }

        @Override
        public int compareTo(Object o) {
            return Integer.compare(this.position, ((PresetItem) o).position);
        }
    }

    /**
     * A background task for starting, and logging output from Slay the Spire
     */
    private class SpireLaunchTask extends SwingWorker<Void, String> {
        private ProcessBuilder processBuilder;
        private boolean cullProcess = false;
        private Process process;

        SpireLaunchTask(String[] arguments) {
            this.processBuilder = new ProcessBuilder(arguments);

            process = null;
            processBuilder.redirectErrorStream(true);
        }

        @Override
        protected Void doInBackground() throws SecurityException, IOException {
            // Start Slay the Spire if it hasn't been started previously.
            // Once the start method returns, we'll "replace" the launcher
            // for the console.
            if (Objects.isNull(process)) {
                process = processBuilder.start();
                SwingUtilities.invokeLater(() -> setVisible(false));
                // TODO: Set console visible here
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                do {
                    String line = reader.readLine();

                    if (line != null) publish(line);
                } while (!cullProcess && process.isAlive());
            }

            return null;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String chunk : chunks) {
                // TODO: Append the output to the console window.
            }
        }

        @Override
        protected void done() {
            setVisible(true);
        }

        public void terminate() {
            cullProcess = true;
        }
    }

    /**
     * A background task for discovering mods for ModTheSpire.
     * <p>
     * This class migrates functionality from the loader to the
     * launcher to provide a more ui-centric foundation.
     */
    private class ModDiscoveryTask extends SwingWorker<List<ModInfo>, Void> {
        private List<SteamSearch.WorkshopInfo> workshopInfos = new ArrayList<>();

        /**
         * Finds and loads any mods for Slay the Spire, while providing
         * feedback to the end-user about what's happening.
         *
         * @return The discovered mod list.
         * @throws Exception Some arcane error happened.
         */
        @Override
        protected List<ModInfo> doInBackground() throws Exception {
            // Inform the end-user that the launcher is currently looking for
            // workshop mods.
            SwingUtilities.invokeLater(() -> {
                statusBar.setMessage("Searching for workshop mods...");
                statusBar.setProgressBarVisible(true);
                statusBar.setProgressIndeterminate(true);
            });

            // Look for workshop mods.
            workshopInfos = findWorkshopMods();

            // Inform the end-user that the launcher found X amount of mods.
            SwingUtilities.invokeLater(() -> statusBar.setMessage("Found " + workshopInfos.size() + " mods!"));

            // Save the workshop timestamps
            saveWorkshopTimestamps();

            // Inform the end-user that the launcher is trying to find the
            // current version of Slay the Spire.
            SwingUtilities.invokeLater(() -> statusBar.setMessage("Finding game version..."));
            findGameVersion();
            SwingUtilities.invokeLater(() -> statusBar.setMessage("Running version " + Loader.STS_VERSION));

            // Load the workshop mod's info.
            List<ModInfo> modInfos = new ArrayList<>();
            File[] modFolderFiles;
            modFolderFiles = new File(Loader.MOD_DIR).listFiles((d, name) -> name.endsWith(".jar"));

            // Ensure modFolderFiles isn't null.
            if (modFolderFiles == null) modFolderFiles = new File[0];

            // Finalize modFolderFiles in a new variable named finalModFolderFiles.
            File[] finalModFolderFiles = modFolderFiles;

            // Inform the end-user that we're loading mod info.
            SwingUtilities.invokeLater(() -> {
                statusBar.setMessage("Loading mod info...");

                statusBar.setProgressIndeterminate(false);
                statusBar.setProgressMaximum(workshopInfos.size() + finalModFolderFiles.length);
            });

            // Read the info file from the mod folder mods.
            for (File f : modFolderFiles) {
                // Read the mod info
                ModInfo info = ModInfo.ReadModInfo(f);

                // Ensure the loaded info isn't null.
                // If it isn't, we'll ensure the mod doesn't currently exist in our
                // mod info cache before adding it.
                if (info != null) {
                    if (modInfos.stream().noneMatch(i -> i.ID == null || i.ID.equalsIgnoreCase(info.ID)))
                        modInfos.add(info);
                }

                // Increment the progress bar
                SwingUtilities.invokeAndWait(() -> statusBar.setProgressValue(statusBar.getProgressValue() + 1));
            }

            // Read the info file from the Steam workshop mods.
            for (SteamSearch.WorkshopInfo workshopInfo : workshopInfos) {
                File[] files = workshopInfo.getInstallPath().toFile().listFiles((d, name) -> name.endsWith(".jar"));

                if (files != null) {
                    for (File f : files) {
                        ModInfo info = ModInfo.ReadModInfo(f);

                        if (info != null) {
                            // Disable the update json url for workshop content.
                            info.UpdateJSON = null;
                            info.isWorkshop = true;

                            // If the workshop item is a newer version, use it instead of the local mod.
                            boolean doAdd = true;
                            Iterator<ModInfo> it = modInfos.iterator();

                            while (it.hasNext()) {
                                ModInfo modInfo = it.next();

                                if (modInfo.ID != null && modInfo.ID.equalsIgnoreCase(info.ID)) {
                                    if (modInfo.ModVersion == null || info.ModVersion == null) {
                                        doAdd = false;
                                        break;
                                    }

                                    if (info.ModVersion.isGreaterThan(modInfo.ModVersion)) {
                                        it.remove();
                                    } else {
                                        doAdd = false;
                                        break;
                                    }
                                }
                            }

                            if (doAdd) modInfos.add(info);
                        }
                    }
                }

                // Increment the progress bar
                SwingUtilities.invokeAndWait(() -> statusBar.setProgressValue(statusBar.getProgressValue() + 1));
            }

            // Sort the mod infos alphabetically
            modInfos.sort(Comparator.comparing(m -> m.Name));

            // Clear the status bar's current message & hide the progress bar
            SwingUtilities.invokeAndWait(() -> {
                statusBar.clearMessage();
                statusBar.setVisible(false);
            });

            // Return the final mod info array
            return modInfos;
        }

        /**
         * When the task finishes discovering all the mods,
         * the mod list is then assigned to the launcher's
         * modInfos variable, then informs the user that
         * the launcher is finished discovering mods.
         */
        @Override
        protected void done() {
            try {
                modInfos = get();

                mappedInfos.clear();
                modInfos.forEach(modInfo -> mappedInfos.put(modInfo.ID, modInfo));

                statusBar.showMessage("Done!");

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        /**
         * Finds workshop mods.
         */
        private List<SteamSearch.WorkshopInfo> findWorkshopMods() {
            List<SteamSearch.WorkshopInfo> workshopInfos = new ArrayList<>();

            try {
                String path = SteamWorkshop.class.getProtectionDomain().getCodeSource().getLocation().getPath();

                path = URLDecoder.decode(path, "utf-8");
                path = new File(path).getPath();

                ProcessBuilder builder = new ProcessBuilder(
                    SteamSearch.findJRE(),
                    "-cp", path + File.pathSeparatorChar + Loader.STS_JAR,
                    "com.evacipated.cardcrawl.modthespire.steam.SteamWorkshop"
                ).redirectError(ProcessBuilder.Redirect.INHERIT);

                Process process = builder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String title = null;
                    String id = null;
                    String installPath = null;
                    String timeUpdated = null;
                    String line;

                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);

                        if (title == null) {
                            title = line;
                        } else if (id == null) {
                            id = line;
                        } else if (installPath == null) {
                            installPath = line;
                        } else if (timeUpdated == null) {
                            timeUpdated = line;
                        } else {
                            SteamSearch.WorkshopInfo info = new SteamSearch.WorkshopInfo(title, id, installPath, timeUpdated, line);

                            if (!info.hasTag("tool") && !info.hasTag("tools")) {
                                workshopInfos.add(info);
                            }

                            title = null;
                            id = null;
                            installPath = null;
                            timeUpdated = null;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Loader.DEBUG) {
                System.out.println("#########################");

                for (SteamSearch.WorkshopInfo info : workshopInfos) {
                    System.out.println("#        Title: " + info.getTitle());
                    System.out.println("# Install Path: " + info.getInstallPath());
                    System.out.println("# Time Updated: " + info.getTimeUpdated());
                    System.out.println("#         Tags: " + Arrays.toString(info.getTags().toArray()));
                    System.out.println("#########################");
                }
            }

            return workshopInfos;
        }

        /**
         * Saves the workshop mod's "last updated" timestamp to file.
         */
        private void saveWorkshopTimestamps() {
            Map<String, Integer> lastUpdated = null;
            String path = SpireConfig.makeFilePath(null, "WorkshopUpdated", "json");

            if (new File(path).isFile()) {
                try {
                    String data = new String(Files.readAllBytes(Paths.get(path)));
                    Gson gson = new Gson();
                    java.lang.reflect.Type type = new TypeToken<Map<String, Integer>>() {
                    }.getType();

                    try {
                        lastUpdated = gson.fromJson(data, type);
                    } catch (JsonSyntaxException ignored) {
                    }

                    if (lastUpdated == null) lastUpdated = new HashMap<>();

                    for (SteamSearch.WorkshopInfo info : workshopInfos) {
                        if (info == null) continue;

                        int savedTime = lastUpdated.getOrDefault(info.getID(), 0);

                        if (savedTime < info.getTimeUpdated()) {
                            lastUpdated.put(info.getID(), info.getTimeUpdated());

                            if (savedTime != 0) {
                                System.out.println(info.getTitle() + " WAS UPDATED!");
                            }
                        }
                    }

                    Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
                    String data1 = gson1.toJson(lastUpdated);

                    Files.write(Paths.get(SpireConfig.makeFilePath(null, "WorkshopUpdated", "json")), data1.getBytes());

                } catch (IOException e) {
                    e.printStackTrace();  // TODO
                }
            }
        }

        /**
         * Locates the current Slay the Spire version
         * from the game's jar file.
         */
        private void findGameVersion() throws NullPointerException {
            try {
                URLClassLoader tmpLoader = new URLClassLoader(new URL[]{new File(Loader.STS_JAR).toURI().toURL()});

                // Read CardCrawlGame.VERSION_NUM
                InputStream in = tmpLoader.getResourceAsStream("com/megacrit/cardcrawl/core/CardCrawlGame.class");
                ClassReader classReader = new ClassReader(Objects.requireNonNull(in));

                classReader.accept(new GameVersionFinder(), 0);

                // Read Settings.isBeta
                InputStream in2 = tmpLoader.getResourceAsStream("com/megacrit/cardcrawl/core/Settings.class");
                ClassReader classReader2 = new ClassReader(Objects.requireNonNull(in2));

                classReader2.accept(new GameBetaFinder(), 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
