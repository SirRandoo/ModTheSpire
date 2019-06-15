package com.evacipated.cardcrawl.modthespire.ui;

import javax.swing.*;
import java.util.HashMap;

public class ModListComponent extends JList<ComplexListItem> {
    private final HashMap<String, ComplexListItem> mods = new HashMap<>();

    public ModListComponent() {
        setDragEnabled(true);
        setDropMode(DropMode.INSERT);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setTransferHandler(new ListItemTransferHandler());
    }

    /**
     * Returns whether or not a mod is checked on the list.
     *
     * @param modId The mod id to check for.
     * @return The current state of a mod.
     */
    public boolean isModChecked(String modId) {
        ComplexListItem item = mods.get(modId);

        return item != null && item.getCheckState();
    }

    /**
     * Sets a mod's state.
     *
     * @param modId The mod to update.
     * @param state The new state for the mod.
     */
    public void setModState(String modId, boolean state) {
        ComplexListItem item = mods.get(modId);

        if (item == null) {
            item = new ComplexListItem(modId, null);

            mods.put(modId, item);
        }

        item.setCheckState(state);
    }

    /**
     * Sets a mod's state to enabled.
     *
     * @param modId The mod to update.
     */
    public void setModEnabled(String modId) {
        setModState(modId, true);
    }

    /**
     * Sets a mod's state to disabled.
     *
     * @param modId The mod to update.
     */
    public void setModDisabled(String modId) {
        setModState(modId, false);
    }
}
