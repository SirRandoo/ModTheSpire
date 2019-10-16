package com.evacipated.cardcrawl.modthespire;

import com.evacipated.cardcrawl.modthespire.lib.ConfigUtils;
import com.evacipated.cardcrawl.modthespire.ui.Launcher2;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.vdurmont.semver4j.Semver;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Pack {
    private static transient Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private ModPreset[] mods;
    @SerializedName("ModTheSpire")
    private MTSPreset mtsPreset;
    private transient Path path;
    private transient boolean dirty;

    public static Pack fromDisk(Path loc) throws FileNotFoundException, JsonSyntaxException, JsonIOException {
        Gson builder = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        Pack p = builder.fromJson(new FileReader(loc.toFile()), Pack.class);
        p.path = loc;
        p.gatherModInfos();

        return p;
    }

    public static Pack getDefaultPreset(ArrayList<ModInfo> discovery) {
        Pack p = new Pack();
        ArrayList<ModPreset> mods = new ArrayList<>();

        for (ModInfo mod : discovery) {
            ModPreset m = new ModPreset(mod.getIDName());
            m.info = mod;

            mods.add(m);
        }

        p.mods = mods.toArray(new ModPreset[0]);
        p.path = Paths.get(ConfigUtils.CONFIG_DIR, "presets", "default.mts");
        p.mtsPreset = new MTSPreset();
        p.gatherModInfos();

        return p;
    }

    private void gatherModInfos() {
        HashMap<String, ModInfo> mapped = Launcher2.getInstance().getMappedDiscovery();

        for (ModPreset mod : mods) {
            ModInfo info = mapped.get(mod.id);

            if (info == null) {
                if (Loader.DEBUG) System.out.println("[Pack] Mod \"" + mod.id + "\" wasn't discovered!");
                continue;
            }

            mod.info = info;
        }
    }

    public String[] getMissingMods() {
        HashMap<String, ModInfo> mapped = Launcher2.getInstance().getMappedDiscovery();

        return (String[]) Arrays.stream(mods).filter(mod -> !mapped.containsKey(mod.id) && mod.enabled).map(mod -> mod.id).toArray();
    }

    public void save() throws IOException {
        if (path == null) throw new IOException("File location is null");

        try (FileWriter writer = new FileWriter(path.toFile())) {
            gson.toJson(this, Pack.class, writer);
            dirty = false;
        }
    }

    public ModPreset[] getModList() {
        return mods;
    }

    public void setModList(ArrayList<ModPreset> list) {
        mods = list.toArray(new ModPreset[0]);
        dirty = true;
    }

    public MTSPreset getMtsPreset() {
        return mtsPreset;
    }

    public Path getFilePath() {
        return path;
    }

    public boolean isDirty() {
        return dirty;
    }

    private int findOptimalLoadPosition(ArrayList<ModPreset> ref, ModPreset target) {
        ModInfo info = target.getModInfo();
        int lowest = -1;

        for (ModPreset mod : ref) {
            if (Arrays.stream(info.Dependencies).anyMatch(e -> e.equalsIgnoreCase(mod.getModInfo().getIDName())) || Arrays.stream(info.OptionalDependencies).anyMatch(e -> e.equalsIgnoreCase(mod.getModInfo().getIDName()))) {
                int index = ref.lastIndexOf(mod);

                if (index > lowest) {
                    lowest = index;
                }
            }
        }

        return lowest + 1;
    }

    public void sortModList() {
        ArrayList<ModPreset> modList = Arrays.stream(mods)
            .filter(mod -> mod.getModInfo().Dependencies.length <= 0 && mod.getModInfo().OptionalDependencies.length <= 0)
            .collect(Collectors.toCollection(ArrayList::new));

        for (ModPreset mod : mods) {
            if (modList.indexOf(mod) == -1) {
                int pos = findOptimalLoadPosition(modList, mod);

                if (pos == -1) {
                    modList.add(mod);
                } else {
                    modList.add(pos, mod);
                }
            }
        }

        mods = (ModPreset[]) modList.toArray();
        dirty = true;
    }

    @SuppressWarnings("FieldCanBeLocal")
    public static class MTSPreset {
        private boolean debug = false;
        private boolean beta = false;
        private boolean bypass = false;

        public boolean isBypassEnabled() {
            return bypass;
        }

        public boolean isBetaEnabled() {
            return beta;
        }

        public boolean isDebugEnabled() {
            return debug;
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    public static class ModPreset {
        private String id;
        private boolean enabled = false;
        private transient ModInfo info;

        public ModPreset(String id) {
            this.id = id;
        }

        public ModPreset(String id, Boolean enabled) {
            this.id = id;
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public ModInfo getModInfo() {
            return info;
        }

        public boolean canLoad() {
            return info.MTS_Version.isGreaterThan(Loader.MTS_VERSION) && new Semver(info.STS_Version, Semver.SemverType.LOOSE).isGreaterThan(Loader.MTS_VERSION);
        }
    }
}
