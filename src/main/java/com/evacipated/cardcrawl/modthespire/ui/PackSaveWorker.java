package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Pack;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;

public class PackSaveWorker extends SwingWorker<Void, Void> {
    private Pack pack;
    private ArrayList<Pack.ModPreset> mods;

    public PackSaveWorker(Pack pack, ArrayList<Pack.ModPreset> data) {
        this.pack = pack;
        mods = data;
    }

    public PackSaveWorker(Pack pack) {
        this(pack, null);
    }

    @Override
    protected Void doInBackground() throws IOException {
        if (mods != null) pack.setModList(mods);

        if (!pack.isDirty()) {
            System.out.println("[PackSaveWorker] Pack " + pack.getFilePath().getFileName().toString() + " doesn't need to be saved.");
            throw new IOException();
        }

        pack.sortModList();
        pack.save();

        return null;
    }
}
