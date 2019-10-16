package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.Pack;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.nio.file.Path;

public class PackLoadWorker extends SwingWorker<Pack, Void> {
    private Path packLocation;

    public PackLoadWorker(Path loc) {
        packLocation = loc.toAbsolutePath();
    }

    @Override
    protected Pack doInBackground() throws Exception {
        // TODO: Clean up print statements
        if (Loader.DEBUG) System.out.println("[PackLoadWorker] Validating path \"" + packLocation.toString() + "\"...");
        if (packLocation.normalize().toFile().isDirectory())
            throw new PackLoadException("Packs cannot be a directory!");

        try {
            System.out.println("[PackLoadWorker] Loading pack @ " + packLocation.toString());
            Pack p = Pack.fromDisk(packLocation);
            System.out.println("[PackLoadWorker] Pack successfully loaded!");

            return p;
        } catch (FileNotFoundException e) {
            System.out.println("[PackLoadWorker] Pack @ " + packLocation.toString() + " doesn't exist!  Did you delete it?");
            throw new PackLoadException("Pack file not found", e);
        } catch (JsonIOException e) {
            System.out.println("[PackLoadWorker] Pack file can't be read.");
            throw new PackLoadException("Pack file couldn't be read from", e);
        } catch (JsonSyntaxException e) {
            System.out.println("[PackLoadWorker] Pack file malformed!");
            throw new PackLoadException("Pack file malformed", e);
        }
    }
}
