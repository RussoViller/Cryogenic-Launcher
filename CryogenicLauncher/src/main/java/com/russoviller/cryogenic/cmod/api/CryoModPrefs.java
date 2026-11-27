package com.russoviller.cryogenic.cmod.api;

import java.util.Set;

/**
 * API 2: key/value settings of a mod. Saved right away in the mod data folder and shared between
 * the Launcher side and the in-game side of the same mod. Thread safe.
 */
public interface CryoModPrefs {
    String getString(String key, String def);
    int getInt(String key, int def);
    long getLong(String key, long def);
    float getFloat(String key, float def);
    boolean getBoolean(String key, boolean def);

    void putString(String key, String value);
    void putInt(String key, int value);
    void putLong(String key, long value);
    void putFloat(String key, float value);
    void putBoolean(String key, boolean value);

    boolean contains(String key);
    void remove(String key);
    Set<String> keys();
    void clear();
}
