package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.ModInfo;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;

/**
 * A basic panel for displaying mod information.
 * <p>
 * The mod view is a top-down overview of a mod in
 * a slightly less cluttered layout.  In the top-left
 * corner, the mod's name, version, MTS version, and
 * StS version are displayed.  In the center, the
 * mod's description and authors are displayed.  In
 * the bottom, the dependency and mod credits are
 * displayed.
 * <p>
 * The dependency and credit information are currently
 * hidden inside a spoiler implementation to reduce the
 * immediate clutter.  Should the end-user decide to
 * expand one, or both, of the panels, their contents
 * will be revealed.  If the content would overflow the
 * mod view, a scrollbar will be accessible.
 */
public class ModView extends JScrollPane {
    private JLabel version;
    private JLabel mtsVersion;
    private JLabel stsVersion;
    private JTextPane description;
    private JTextPane authors;

    private CollapsiblePanel dependencyPanel;
    private CollapsiblePanel creditPanel;
    private JTextArea credits;
    private JTextArea dependencies;

    private JLabel name;

    private JPanel outer;

    public ModView() {
        setupUI();
    }

    /**
     * Creates a new ModView from a ModInfo definition.
     *
     * @param mod A ModInfo object to populate the display from.
     */
    public ModView(ModInfo mod) {
        this();

        setModName(mod.Name, mod.ID);
        setModVersion(mod.ModVersion.toString());
        setMtsVersion(mod.MTS_Version.toString());
        setStsVersion(mod.STS_Version);
        setDependencies(mod.Dependencies);
        setCredits(mod.Credits);
    }

    /**
     * Sets up the ui for the mod view.
     * <p>
     * You shouldn't need to call this yourself.
     */
    private void setupUI() {
        // Panels
        JPanel meta = new JPanel();
        JPanel meta2 = new JPanel();
        outer = new JPanel();

        dependencyPanel = new CollapsiblePanel("Dependencies");
        creditPanel = new CollapsiblePanel("Credits");

        // Panel prep
        meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
        meta2.setLayout(new BoxLayout(meta2, BoxLayout.Y_AXIS));
        outer.setLayout(new GridBagLayout());

        // Meta info
        name = new JLabel("Name: UNKNOWN");
        version = new JLabel("Version: UNKNOWN");
        mtsVersion = new JLabel("MTS Version: UNKNOWN");
        stsVersion = new JLabel("STS Version: UNKNOWN");
        description = new JTextPane();
        authors = new JTextPane();

        description.setEditorKit(new HTMLEditorKit());
        description.setFont(this.getFont());
        description.setAutoscrolls(false);
        description.setEditable(false);
        description.setOpaque(false);
        description.setBorder(null);

        authors.setBorder(null);
        authors.setOpaque(false);
        authors.setEditable(false);
        authors.setAutoscrolls(false);
        authors.setFont(this.getFont());
        authors.setEditorKit(new HTMLEditorKit());

        description.setText("<center><i>No description provided!</i></center>");
        authors.setText("<center>by <i>UNKNOWN</i></center>");

        meta.add(name);
        meta.add(version);
        meta.add(mtsVersion);
        meta.add(stsVersion);

        meta2.add(description, CENTER_ALIGNMENT);
        meta2.add(authors, CENTER_ALIGNMENT);

        // Dependency info
        dependencies = new JTextArea();
        dependencies.setBorder(null);
        dependencies.setOpaque(false);
        dependencies.setLineWrap(true);
        dependencies.setEditable(false);
        dependencies.setAutoscrolls(false);
        dependencies.setWrapStyleWord(true);
        dependencies.setFont(this.getFont());

        dependencyPanel.getContents().add(dependencies);

        // Credit info
        credits = new JTextArea();
        credits.setBorder(null);
        credits.setOpaque(false);
        credits.setLineWrap(true);
        credits.setEditable(false);
        credits.setAutoscrolls(false);
        credits.setWrapStyleWord(true);
        credits.setFont(this.getFont());

        creditPanel.getContents().add(credits);

        // Outer panel insertion
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.gridwidth = 2;

        outer.add(meta, constraints);

        constraints.gridy = 1;
        constraints.gridwidth = 3;
        outer.add(meta2, constraints);

        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.PAGE_START;
        constraints.insets = new Insets(0, 0, 0, 10);
        outer.add(dependencyPanel, constraints);

        constraints.gridx = 2;
        constraints.insets = new Insets(0, 10, 0, 0);
        outer.add(creditPanel, constraints);

        this.setViewportView(outer);
    }

    /**
     * Updates the mod view with a mod's current name.
     *
     * @param modId   The internal mod id to display along side
     *                the display name for the mod.
     * @param modName The display name for the mod.
     */
    public void setModName(String modId, String modName) {
        if (modId != null && modName != null) {
            name.setText(modName + " (" + modId + ")");
            name.setVisible(true);
        } else {
            name.setText("");
            name.setVisible(false);
        }
    }

    /**
     * Updates the mod view with a mod's current name.
     *
     * @param modId The internal mod id to display along side
     *              the display name for the mod.  The mod id
     *              is transformed into a display name, then
     *              passed to the true implementation.
     */
    public void setModName(String modId) {
        if (modId != null) {
            String first = String.valueOf(modId.charAt(0));

            setModName(modId, first.toUpperCase() + modId.substring(1));
        } else {
            setModName(null, null);
        }
    }

    /**
     * Updates the mod view with a mod's current version.
     *
     * @param modVersion A mod's version string.
     */
    public void setModVersion(String modVersion) {
        if (modVersion != null) {
            version.setText("Version: " + modVersion);
            version.setVisible(true);
        } else {
            version.setText("");
            version.setVisible(false);
        }
    }

    /**
     * Updates the mod view with a mod's required ModTheSpire
     * version information.
     *
     * @param mtsVersion A mod's ModTheSpire version string.
     */
    public void setMtsVersion(String mtsVersion) {
        if (mtsVersion != null) {
            this.mtsVersion.setText("MTS Version: " + mtsVersion);
            this.mtsVersion.setVisible(true);
        } else {
            this.mtsVersion.setText("");
            this.mtsVersion.setVisible(false);
        }
    }

    /**
     * Updates the mod view with a mod's required Slay the Spire
     * version information.
     *
     * @param stsVersion A mod's Slay the Spire version string.
     */
    public void setStsVersion(String stsVersion) {
        if (stsVersion != null) {
            this.stsVersion.setText("STS Version: " + stsVersion);
            this.stsVersion.setVisible(true);
        } else {
            this.stsVersion.setText("");
            this.stsVersion.setVisible(false);
        }
    }

    /**
     * Updates the mod view with credit information from a mod.
     *
     * @param credits A mod's credit information.
     */
    public void setCredits(String credits) {
        if (credits != null) {
            this.credits.setText(credits);
            this.creditPanel.setVisible(true);

        } else {
            this.credits.setText("");
            this.creditPanel.setVisible(false);
        }
    }

    /**
     * Updates the mod view with dependency information from a mod.
     *
     * @param dependencies A mod's dependencies.
     */
    public void setDependencies(String[] dependencies) {
        if (dependencies != null) {
            StringBuilder d = new StringBuilder();

            for (String sub : dependencies) d.append(sub).append(", ");

            // Strip the trailing comma
            if (d.lastIndexOf(",") > 0) {
                d.delete(d.lastIndexOf(","), d.length());
            }

            this.dependencies.setText(d.toString());
            this.dependencyPanel.setVisible(true);

        } else {
            this.dependencies.setText("");
            this.dependencyPanel.setVisible(false);
        }
    }

    /**
     * Updates the mod view with a mod's description.
     *
     * @param description A mod's description.
     */
    public void setDescription(String description) {
        if (description != null) {
            this.description.setText("<center>" + description + "</center>");
            this.description.setVisible(true);
        } else {
            this.description.setText("<center><i>No description provided!</i></center>");
        }
    }

    /**
     * Updates the mod view with a mod's author list.
     *
     * @param authors A mod's author list.
     */
    public void setAuthors(String[] authors) {
        if (authors != null) {
            StringBuilder a = new StringBuilder();

            for (String sub : authors) a.append(sub).append(", ");

            // Strip the trailing comma
            if (a.lastIndexOf(",") > 0) {
                a.delete(a.lastIndexOf(","), a.length());
            }

            this.authors.setText("<center>" + a.toString() + "</center>");
            this.authors.setVisible(true);
        } else {
            this.authors.setText("<center>by <i>unknown</i></center>");
        }
    }

    /**
     * Returns the mod view's current visibility status.
     */
    public boolean isViewVisible() {
        return outer.isVisible();
    }

    /**
     * Updates the mod view's current visibility.
     *
     * @param visible Whether or not the mod view should
     *                be visible to the end-user.
     */
    public void setViewVisible(boolean visible) {
        outer.setVisible(visible);
    }
}
