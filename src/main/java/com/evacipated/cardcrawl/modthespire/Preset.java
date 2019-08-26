package com.evacipated.cardcrawl.modthespire;

import com.evacipated.cardcrawl.modthespire.lib.ConfigUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * A class for loading ModTheSpire presets.
 */
public class Preset {
    private ModPreset[] mods;

    @SerializedName("ModTheSpire")
    private MTSPreset mtsPreset;

    private transient ArrayList<ModInfo> modInfos;
    private transient HashMap<String, ModInfo> mappedModList;
    private transient File file;
    private transient Gson gson;

    /**
     * Deserialize a serialized Preset object on disk.
     *
     * @param file The file to deserialize.
     * @return A Preset object representing the serialized data.
     *
     * @throws FileNotFoundException The file could not be found.
     * @throws JsonSyntaxException   The JSON file was malformed.
     * @throws JsonIOException       The file could not be read from.
     */
    public static Preset fromFile(File file) throws FileNotFoundException, JsonSyntaxException, JsonIOException {
        Gson builder = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

        Preset obj = builder.fromJson(new FileReader(file), Preset.class);
        obj.setGson(builder);
        obj.setPresetFile(file);

        return obj;
    }

    /**
     * Creates the default preset for ModTheSpire.
     *
     * @param discovery       A list of ModInfos discovered by the launcher.
     * @param mappedDiscovery A hash map of ModInfos discovered by the launcher.
     * @return The default preset for ModTheSpire.
     */
    public static Preset getDefaultPreset(ArrayList<ModInfo> discovery, HashMap<String, ModInfo> mappedDiscovery) {
        Preset preset = new Preset();
        preset.setGson(new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
        );

        ArrayList<ModPreset> presets = new ArrayList<>();

        for (ModInfo item : discovery) {
            ModPreset p = new ModPreset();
            p.enabled = false;
            p.id = item.ID;

            presets.add(p);
        }

        preset.mods = (ModPreset[]) presets.toArray();
        preset.modInfos = discovery;
        preset.mappedModList = mappedDiscovery;
        preset.file = Paths.get(ConfigUtils.CONFIG_DIR, "presets", "default.mts").toFile();

        preset.mtsPreset = new MTSPreset();

        return preset;
    }

    /**
     * Whether or not there are mods missing in the preset.
     */
    public boolean isMissingMods() {
        for (ModPreset mod : mods) {
            if (!mappedModList.containsKey(mod.id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns any mods this preset may be missing.
     *
     * @return An array of mod ids.
     */
    public String[] getMissingMods() {
        return (String[]) Arrays.stream(mods).filter(mod -> !mappedModList.containsKey(mod.id)).toArray();
    }

    /**
     * Whether or not is preset is currently viable.  A preset
     * isn't "viable" if there are any missing mods.
     */
    public boolean isPresetViable() {
        return getMissingMods().length == 0;
    }

    /**
     * Serializes a Preset object to disk.
     *
     * @throws IOException The file could not be created, written to,
     *                     the file is a directory, or no preset file
     *                     was set.
     */
    public void save() throws IOException {
        if (gson == null) {
            gson = new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create();
        }

        if (file == null) {
            throw new IOException("No preset file specified.");
        }

        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, Preset.class, writer);
        }
    }

    /**
     * Sets the file this preset will serialize to.
     *
     * @param file The file to save to.
     */
    public void setPresetFile(File file) {
        this.file = file;
    }

    /**
     * Sets the Gson object this preset will use to serialize itself.
     * You normally shouldn't need to call this, unless you're manually
     * creating a new Preset instance.
     *
     * @param gson The Gson object this preset will use to serialize itself.
     */
    public void setGson(Gson gson) {
        this.gson = gson;
    }

    /**
     * Gets the ModTheSpire settings for this preset.
     */
    public MTSPreset getMtsPreset() {
        return mtsPreset;
    }

    /**
     * Gets the full mod list for this preset.  This returns all mods
     * in the preset, including those that are not in the launcher's
     * discovery directories.
     */
    public ModPreset[] getFullModList() {
        return mods;
    }

    /**
     * A dataclass representing a mod JSON object.
     */
    public static class ModPreset {
        /**
         * The ID of a given mod.  This is taken at serialization
         * from a discovered mod's id.
         */
        private String id;

        /**
         * Whether or not a given mod is enabled.  This is populated
         * at serialization by the Launcher class.
         */
        @SuppressWarnings("FieldCanBeLocal")
        private boolean enabled = false;

        public String getModId() {
            return this.id;
        }

        public boolean isModEnabled() {
            return enabled;
        }
    }

    /**
     * A dataclass representing a ModTheSpire quick setting object.
     */
    @SuppressWarnings("FieldCanBeLocal")
    public static class MTSPreset {
        /**
         * Whether or not this preset should be ran with debug mode.
         */
        private boolean debug = false;

        /**
         * Whether or not this preset should be allowed to run with
         * a beta build of SlayTheSpire.
         */
        private boolean beta = false;

        /**
         * Whether or not this preset should only output a dump jar.
         */
        private boolean outJar = false;

        /**
         * Whether or not this preset should be ran with JRE51 instead
         * of the system default.
         */
        private boolean jre51 = false;

        public boolean isDebugEnabled() {
            return debug;
        }

        public boolean shouldAllowBetas() {
            return beta;
        }

        public boolean shouldOutputJar() {
            return outJar;
        }

        public boolean isJre51Mode() {
            return jre51;
        }
    }
}
