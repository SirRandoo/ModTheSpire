package com.evacipated.cardcrawl.modthespire.lib;

import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigUtils
{
    private static final String APP_NAME = "ModTheSpire";
    public static final Path CONFIG_DIR;

    static {
        Path basedir = Paths.get(SystemUtils.USER_HOME);

        if (SystemUtils.IS_OS_WINDOWS) {
            // %LOCALAPPDATA%/APP_NAME/
            // Fallback to %APPDATA%/APP_NAME/
            String appdata = System.getenv("LOCALAPPDATA");
            if (appdata == null || appdata.isEmpty()) {
                appdata = System.getenv("APPDATA");
            }

            basedir = Paths.get(appdata);
        } else if (SystemUtils.IS_OS_LINUX) {
            // /home/x/.config/APP_NAME/
            basedir = Paths.get(SystemUtils.USER_HOME, ".config");
        } else if (SystemUtils.IS_OS_MAC) {
            // /Users/x/Library/Preferences/APP_NAME/
            basedir = Paths.get(SystemUtils.USER_HOME, "Library", "Preferences");
        }

        CONFIG_DIR = basedir.resolve(APP_NAME);

        // Make config directory
        CONFIG_DIR.toFile().mkdirs();
    }
}
