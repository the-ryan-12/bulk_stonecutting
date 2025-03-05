package org.vee.bulk_stonecutting.client;

/// Config class that stores whether the checkbox was checked or not in the current minecraft instance. Does not save when game is closed.
public class ModConfig {
    private static boolean massCraftCheckEnabled = false;

    public static void setMassCraftCheckEnabled(boolean enabled) {
        massCraftCheckEnabled = enabled;
    }

    public static boolean isMassCraftCheckEnabled() {
        return massCraftCheckEnabled;
    }
}
