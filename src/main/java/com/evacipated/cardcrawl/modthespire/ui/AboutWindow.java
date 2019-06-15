package com.evacipated.cardcrawl.modthespire.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A simple dialog for displaying basic about
 * information.
 * <p>
 * TODO: Consider reworking the about dialog's
 * design.
 */
public class AboutWindow extends JDialog {
    private JLabel banner;
    private JLabel icon;
    private JLabel contents;

    public AboutWindow(JFrame parent) {
        super(parent, parent.getTitle() + " - About", ModalityType.APPLICATION_MODAL);

        setupUI();
    }

    /**
     * Sets up the ui for the about dialog.
     * <p>
     * You shouldn't need to call this directly.
     */
    private void setupUI() {
        banner = new JLabel();
        icon = new JLabel();
        contents = new JLabel();

        banner.setIconTextGap(0);
        banner.setVisible(false);

        icon.setIconTextGap(0);
        icon.setVisible(false);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(banner);

        JPanel subPanel = new JPanel();
        subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.X_AXIS));
        subPanel.add(icon);
        subPanel.add(contents);

        panel.add(subPanel);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        add(panel);
        setSize(new Dimension(400, 300));
        setMinimumSize(getSize());
    }

    /**
     * Sets the raw content for the about dialog.
     *
     * @param text The content to display.
     */
    public void setContents(String text) {
        contents.setText(text);
    }

    /**
     * Sets the banner for the about dialog.
     * <p>
     * Banners and icons cannot be used together.  If
     * this method is called, it will automatically hide
     * the icon.
     *
     * @param banner The image to display as a banner.
     */
    public void setBanner(ImageIcon banner) {
        this.banner.setIcon(banner);

        if (!this.banner.isVisible()) this.banner.setVisible(true);
        if (this.icon.isVisible()) this.icon.setVisible(false);
    }

    /**
     * Sets the icon for the about dialog.
     * <p>
     * Banners and icons cannot be used together.  If
     * this method is called, it will automatically
     * hide the banner.
     *
     * @param icon The image to display as an icon.
     */
    public void setIcon(ImageIcon icon) {
        this.icon.setIcon(icon);

        if (!this.icon.isVisible()) this.icon.setVisible(true);
        if (banner.isVisible()) banner.setVisible(false);
    }
}
