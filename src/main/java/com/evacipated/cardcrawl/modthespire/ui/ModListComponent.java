package com.evacipated.cardcrawl.modthespire.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DragSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.stream.IntStream;

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

    protected class CellRenderer implements ListCellRenderer<ComplexListItem> {
        @Override
        public Component getListCellRendererComponent(JList<? extends ComplexListItem> list, ComplexListItem value, int index, boolean isSelected, boolean cellHasFocus) {
            JCheckBox checkbox = value.getCheckBox();

            checkbox.setBackground(isSelected ? getSelectionBackground() : getBackground());
            checkbox.setForeground(isSelected ? getSelectionForeground() : getForeground());

            checkbox.setFont(getFont());
            checkbox.setFocusPainted(false);
            checkbox.setBorderPainted(false);

            return checkbox;
        }
    }
}

// from https://stackoverflow.com/questions/16586562/reordering-jlist-with-drag-and-drop/16591678

// @camickr already suggested above.
// https://docs.oracle.com/javase/tutorial/uiswing/dnd/dropmodedemo.html
@SuppressWarnings("serial")
class ModListTransferHandler extends TransferHandler {
    private final DataFlavor localObjectFlavor;
    private int[] indices;
    private int addIndex = -1;
    private int addCount;

    public ModListTransferHandler() {
        super();
        // localObjectFlavor = new ActivationDataFlavor(
        // Object[].class, DataFlavor.javaJVMLocalObjectMimeType, "Array of
        // items");
        localObjectFlavor = new DataFlavor(Object[].class, "Array of items");
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JList<?> source = (JList<?>) c;
        c.getRootPane().getGlassPane().setVisible(true);

        indices = source.getSelectedIndices();
        Object[] transferedObjects = source.getSelectedValuesList().toArray(new Object[0]);
        // return new DataHandler(transferedObjects,
        // localObjectFlavor.getMimeType());
        return new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{localObjectFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return Objects.equals(localObjectFlavor, flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if (isDataFlavorSupported(flavor)) {
                    return transferedObjects;
                } else {
                    throw new UnsupportedFlavorException(flavor);
                }
            }
        };
    }

    @Override
    public boolean canImport(TransferSupport info) {
        return info.isDrop() && info.isDataFlavorSupported(localObjectFlavor);
    }

    @Override
    public int getSourceActions(JComponent c) {
        Component glassPane = c.getRootPane().getGlassPane();
        glassPane.setCursor(DragSource.DefaultMoveDrop);
        return MOVE; // COPY_OR_MOVE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferSupport info) {
        TransferHandler.DropLocation tdl = info.getDropLocation();
        if (!canImport(info) || !(tdl instanceof JList.DropLocation)) {
            return false;
        }

        JList.DropLocation dl = (JList.DropLocation) tdl;
        @SuppressWarnings("rawtypes")
        JList target = (JList) info.getComponent();
        @SuppressWarnings("rawtypes")
        DefaultListModel listModel = (DefaultListModel) target.getModel();
        int max = listModel.getSize();
        int index = dl.getIndex();
        index = index < 0 ? max : index; // If it is out of range, it is
        // appended to the end
        index = Math.min(index, max);

        addIndex = index;

        try {
            Object[] values = (Object[]) info.getTransferable().getTransferData(localObjectFlavor);
            for (Object value : values) {
                int idx = index++;
                ((ModPanel) value).checkBox.addItemListener((event) -> {
                    ((JModPanelCheckBoxList) target).publishBoxChecked();
                });
                listModel.add(idx, value);
                target.addSelectionInterval(idx, idx);
            }
            addCount = values.length;
            return true;
        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
        }

        return false;
    }

    @Override
    protected void exportDone(JComponent c, Transferable data, int action) {
        c.getRootPane().getGlassPane().setVisible(false);
        cleanup(c, action == MOVE);
    }

    private void cleanup(JComponent c, boolean remove) {
        if (remove && Objects.nonNull(indices)) {
            if (addCount > 0) {
                // https://github.com/aterai/java-swing-tips/blob/master/DragSelectDropReordering/src/java/example/MainPanel.java
                int bound = indices.length;
                IntStream.range(0, bound).filter(i -> indices[i] >= addIndex).forEach(i -> indices[i] += addCount);
            }
            @SuppressWarnings("rawtypes")
            JList source = (JList) c;
            @SuppressWarnings("rawtypes")
            DefaultListModel model = (DefaultListModel) source.getModel();
            for (int i = indices.length - 1; i >= 0; i--) {
                model.remove(indices[i]);
            }
        }

        indices = null;
        addCount = 0;
        addIndex = -1;
    }
}
