package com.evacipated.cardcrawl.modthespire.ui;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.steam.SteamSearch;
import com.evacipated.cardcrawl.modthespire.steam.SteamWorkshop;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class ModDiscoveryWorker extends SwingWorker<ArrayList<ModInfo>, ModInfo> {
    @Override
    protected ArrayList<ModInfo> doInBackground() throws Exception {
        ArrayList<ModInfo> discovered = new ArrayList<>();

        try {
            // Since ModTheSpire crashes in dev environments
            Class.forName("com.badlogic.codedisaster.steamworks.SteamAPI");
            discovered = workshopSearch();

            for (ModInfo info : discovered) {
                publish(info);
            }

        } catch (ClassNotFoundException | LinkageError e) {
            System.out.println("[ModDiscoveryWorker] SteamAPI couldn't be loaded; only local mods will be loaded.");
            if (Loader.DEBUG) e.printStackTrace();
        }

        File[] localModFiles = new File(Loader.MOD_DIR).listFiles((d, name) -> name.endsWith(".jar"));
        if (localModFiles == null) localModFiles = new File[0];

        for (File f : localModFiles) {
            ModInfo info = ModInfo.ReadModInfo(f);

            if (info == null) {
                continue;
            }

            if (discovered.stream().noneMatch(i -> i.getIDName().equalsIgnoreCase(info.getIDName()))) {
                discovered.add(info);
            }
        }

        return discovered;
    }

    @Override
    protected void process(List<ModInfo> chunks) {
        for (ModInfo chunk : chunks) {
            System.out.println(String.format("[ModDiscoveryWorker] Discovered mod \"%s\" (v%s): Workshop→%s", chunk.getIDName(), chunk.ModVersion.toString(), Boolean.toString(chunk.isWorkshop)));
        }
    }

    private ArrayList<ModInfo> workshopSearch() {
        ArrayList<SteamSearch.WorkshopInfo> workshopInfos = new ArrayList<>();

        try {
            Path path = Paths.get(SteamWorkshop.class.getProtectionDomain().getCodeSource().getLocation().getPath());

            ProcessBuilder builder = new ProcessBuilder(
                SteamSearch.findJRE(),
                "-cp", path.resolve(Loader.STS_JAR).toAbsolutePath().toString(),
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
                    System.out.println("[ModDiscoveryWorker] Workshop Discovery » " + line);

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
                if (Loader.DEBUG) e.printStackTrace();
            }
        } catch (IOException e) {
            if (Loader.DEBUG) e.printStackTrace();
        }

        if (Loader.DEBUG && !workshopInfos.isEmpty()) {
            for (SteamSearch.WorkshopInfo info : workshopInfos) {
                System.out.println("[ModDiscoveryWorker] Workshop info:");
                System.out.println("[ModDiscoveryWorker]           Title » " + info.getTitle());
                System.out.println("[ModDiscoveryWorker]    Install path » " + info.getInstallPath());
                System.out.println("[ModDiscoveryWorker]    Time updated » " + info.getTimeUpdated());
                System.out.println("[ModDiscoveryWorker]            Tags » " + Arrays.toString(info.getTags().toArray()));
            }
        }

        return processWorkshopInfos(workshopInfos);
    }

    private ArrayList<ModInfo> processWorkshopInfos(ArrayList<SteamSearch.WorkshopInfo> infos) {
        ArrayList<ModInfo> transformed = new ArrayList<>();

        for (SteamSearch.WorkshopInfo workshopInfo : infos) {
            File[] files = workshopInfo.getInstallPath().toFile().listFiles((d, name) -> name.endsWith(".jar"));

            if (files == null) {
                continue;
            }

            for (File f : files) {
                ModInfo modInfo = ModInfo.ReadModInfo(f);

                if (modInfo == null) {
                    continue;
                }

                modInfo.UpdateJSON = null;
                modInfo.isWorkshop = true;

                boolean doAdd = true;
                Iterator<ModInfo> it = transformed.iterator();

                while (it.hasNext()) {
                    ModInfo i = it.next();

                    if (modInfo.getIDName().equalsIgnoreCase(i.getIDName())) {
                        if (modInfo.ModVersion == null || i.ModVersion == null) {
                            doAdd = false;
                            break;
                        } else if (modInfo.ModVersion.isGreaterThan(i.ModVersion)) {
                            it.remove();
                        } else {
                            doAdd = false;
                            break;
                        }
                    }
                }

                if (doAdd) {
                    transformed.add(modInfo);
                }
            }
        }

        return transformed;
    }
}
