package com.evacipated.cardcrawl.modthespire.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.HashMap;

/**
 * A dialog for prompting the user about updates.
 * <p>
 * The dialog allows the user to select which items should be updated via
 * the enable/disable button located directly left to the item label.
 */
public class UpdateDialog extends JDialog implements WindowListener {
    private HashMap<String, URL> files;
    private HashMap<String, String> releaseNotes;

    private DefaultListModel<CheckableListItem> itemModel;
    private JList<CheckableListItem> items;
    private JTextPane content;
    private JButton updateBtn;
    private JButton postponeBtn;

    private UpdateDialog() {
        setResizable(true);

        itemModel = new DefaultListModel<>();
        items = new JList<>(itemModel);
        content = new JTextPane();
        updateBtn = new JButton("Update");
        postponeBtn = new JButton("Postpone");

        items.addListSelectionListener(e -> content.setText(releaseNotes.get(itemModel.getElementAt(e.getLastIndex()).getText())));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, items, content);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridwidth = 3;
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.weighty = 0.9;

        setLayout(new GridBagLayout());
        add(splitPane);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 0.1;
        constraints.gridwidth = 1;
        constraints.gridx = 1;
        constraints.gridy = 1;
        add(updateBtn);

        constraints.gridx = 2;
        add(postponeBtn);
    }

    /**
     * Creates a new update dialog instance.
     */
    public static UpdateDialog create() {
        return new UpdateDialog();
    }

    /**
     * Updates the dialog's title.
     *
     * @param title The new title to display.
     */
    public UpdateDialog withTitle(String title) {
        setTitle(title);

        return this;
    }

    /**
     * Adds a new item to the dialog.
     *
     * @param item         The display name of the item.
     * @param releaseNotes The release notes to display when the user clicks on the item from the list.
     * @param updateUrl    The url to download the newest item from.
     */
    public UpdateDialog withItem(String item, String releaseNotes, URL updateUrl) {
        files.put(item, updateUrl);
        this.releaseNotes.put(item, releaseNotes);

        CheckableListItem i = new CheckableListItem(item);
        i.setCheckState(true);

        itemModel.addElement(i);

        return this;
    }

    /**
     * Adds a new required item to the dialog.
     * <p>
     * Required items are items that default to checked, and cannot be unchecked.
     *
     * @param item         The display name of the item.
     * @param releaseNotes The release notes to display when the user clicks on the item from the list.
     * @param updateUrl    The url to download the newest item from.
     */
    public UpdateDialog withRequiredItem(String item, String releaseNotes, URL updateUrl) {
        files.put(item, updateUrl);
        this.releaseNotes.put(item, releaseNotes);

        CheckableListItem i = new CheckableListItem(item);
        i.setCheckState(true);
        i.setEnabledState(false);

        itemModel.addElement(i);

        return this;
    }

    /**
     * Finalizes the update dialog.
     *
     * @param update   The callable to invoke when the update button is pressed.
     * @param postpone The callable to invoke when the postpone button is pressed.
     */
    public void done(ActionListener update, ActionListener postpone) {
        updateBtn.addActionListener(update);
        postponeBtn.addActionListener(postpone);
    }

    @Override
    public void windowOpened(WindowEvent e) {

    }

    @Override
    public void windowClosing(WindowEvent e) {
        postponeBtn.doClick();
    }

    @Override
    public void windowClosed(WindowEvent e) {

    }

    @Override
    public void windowIconified(WindowEvent e) {

    }

    @Override
    public void windowDeiconified(WindowEvent e) {

    }

    @Override
    public void windowActivated(WindowEvent e) {

    }

    @Override
    public void windowDeactivated(WindowEvent e) {

    }
}
