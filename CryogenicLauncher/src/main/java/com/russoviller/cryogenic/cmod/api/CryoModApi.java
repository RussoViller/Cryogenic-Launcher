package com.russoviller.cryogenic.cmod.api;

import android.content.Context;

import java.io.File;
import java.util.List;

/**
 * Everything the Launcher offers to a Launcher Mod (.cmod) running inside the Launcher.
 * API version: {@link #API_VERSION}. Methods marked "API 2" do not exist on launchers with API 1:
 * check {@link #getApiVersion()} if your mod also wants to run on older launchers.
 *
 * <p>The mod is plain Android code: besides this API it can use any Android/Java class
 * (network, files, Views, threads, sensors, its own libraries...). The API only gives the
 * shortcuts and the hooks that a normal app could not reach.</p>
 */
public interface CryoModApi {

    /** Version of this API. A mod with a bigger "apiVersion" in cmod.json is not loaded. */
    int API_VERSION = 2;

    /** API version of the installed Launcher. */
    int getApiVersion();

    /** Launcher version (e.g. "r1.8.0"). */
    String getLauncherVersion();

    /** Application context of the Launcher. */
    Context getContext();

    /** "id" from cmod.json. */
    String getModId();

    /** Folder with the .cmod files (cmod.json, icon, assets/ and game/). Read only. */
    File getModDirectory();

    /** Private folder of the mod to save data (kept between Launcher sessions). */
    File getDataDirectory();

    /** Current .minecraft folder of the Launcher (versions, saves, mods...). May be null. */
    File getGameDirectory();

    /**
     * Adds the mod function (button) to the CryoMods page. Each mod has ONE function: calling
     * again with the same id updates the button; another id throws IllegalStateException (remove
     * the old one with {@link #removeButton} first if you want to swap it).
     */
    void addButton(CryoModButton button);

    /** Removes the function of this mod. */
    void removeButton(String id);

    /** Short message on the screen (Toast). Any thread. */
    void toast(String message);

    /** Writes to the Launcher log and to the mod log (Launcher Mods › Logs). */
    void log(String message);

    /** Runs on the main thread (to touch Views). */
    void runOnUiThread(Runnable action);

    /** Runs on a background thread (files, network, conversions). */
    void runInBackground(Runnable action);

    // ------------------------------------------------------------------ API 2

    /** API 2: key/value settings of this mod, shared with the in-game side of the mod. */
    CryoModPrefs getPreferences();

    /** API 2: runs the action on the main thread after delayMs. Cancelled when the mod stops. */
    CryoModTask schedule(long delayMs, Runnable action);

    /** API 2: runs the action on the main thread every periodMs (min 16 ms). Cancelled when the mod stops. */
    CryoModTask scheduleRepeating(long initialDelayMs, long periodMs, Runnable action);

    /** API 2: opens a link in the browser (or the app that handles it). */
    void openUrl(String url);

    /** API 2: copies text to the clipboard. */
    void copyToClipboard(String text);

    /** API 2: opens the Android share sheet with this text. */
    void shareText(String text);

    /**
     * API 2: small badge on the mod button in the CryoMods page (e.g. "3", "NEW", "●").
     * null or empty removes it. Any thread.
     */
    void setButtonBadge(String badge);

    /** API 2: language of the Launcher interface (e.g. "pt", "en", "zh"). */
    String getLanguage();

    /** API 2: all installed Launcher Mods (including this one). */
    List<CryoModInfo> getInstalledMods();

    /** API 2: true if the mod with this id is installed and enabled. */
    boolean isModEnabled(String modId);

    /** API 2: names of the installed game versions (instances). */
    List<String> getGameVersions();

    /** API 2: name of the selected game version, or null. */
    String getSelectedGameVersion();

    /** API 2: selects a game version by name (same as tapping it in Versions). False if it does not exist. */
    boolean selectGameVersion(String name);

    /**
     * API 2: sends a message to another enabled mod (delivered on the main thread to its
     * {@link CryoModMessageListener}s). The payload can be any object (String, Map, your own
     * interface...). False if the target is not running.
     */
    boolean sendMessage(String targetModId, String channel, Object payload);

    /** API 2: sends a message to every other running mod. */
    void broadcast(String channel, Object payload);

    /** API 2: receives messages from other mods. Removed automatically when the mod stops. */
    void addMessageListener(CryoModMessageListener listener);

    /** API 2: stops receiving messages on this listener. */
    void removeMessageListener(CryoModMessageListener listener);

    /**
     * API 2: shares an object with other mods under a name (an "API" of your mod). Other mods get
     * it with {@link #getService}. Removed when the mod stops. Use interfaces from the Android/Java
     * SDK (Runnable, Map, java.util.function...) or reflection, because each mod has its own
     * class loader.
     */
    void exposeService(String name, Object service);

    /** API 2: object shared by another running mod with {@link #exposeService}, or null. */
    Object getService(String modId, String name);

    /** API 2: receives Launcher events (game launch, Launcher in foreground/background, mods on/off). */
    void addLauncherListener(CryoLauncherListener listener);

    /** API 2: stops receiving Launcher events on this listener. */
    void removeLauncherListener(CryoLauncherListener listener);
}
