package com.russoviller.cryogenic.cmod.api;

import android.app.Activity;
import android.view.View;
import android.widget.FrameLayout;

import java.io.File;

/**
 * API 2: what the game screen offers to the in-game side of a mod ({@link CryoGameMod}).
 * Keys and mouse buttons use GLFW codes: see {@link CryoKeys}. Input methods can be called from
 * any thread.
 */
public interface CryoGameApi {

    int getApiVersion();

    String getLauncherVersion();

    String getModId();

    /** Activity of the game screen (to create Views and dialogs). */
    Activity getActivity();

    /** Folder with the .cmod files (cmod.json, assets/, game/). Read only. */
    File getModDirectory();

    /** Private data folder of the mod (same as the Launcher side). */
    File getDataDirectory();

    /** Game folder of the running version (where saves, mods, config... are). */
    File getGameDirectory();

    /** Name of the running version (instance). */
    String getVersionName();

    /** Minecraft version of the running instance (e.g. "1.21.1"), or the version name. */
    String getMinecraftVersion();

    /** Settings shared with the Launcher side of the mod. */
    CryoModPrefs getPreferences();

    /** Views in the Launcher style (same as the CryoMods page). */
    CryoModUi getUi();

    // ------------------------------------------------------------------ overlay

    /**
     * Layer over the game and the controls. Touches on empty areas go to the game/controls; only
     * Views that are clickable (or handle touches) catch them. Main thread.
     */
    FrameLayout getOverlay();

    /**
     * Adds a View to the overlay at a corner/side ({@link android.view.Gravity}, e.g.
     * Gravity.TOP | Gravity.END) with a margin in dp. Main thread.
     */
    void addOverlayView(View view, int gravity, int marginXdp, int marginYdp);

    /** Removes a View from the overlay. Main thread. */
    void removeOverlayView(View view);

    // ------------------------------------------------------------------ input

    /** Presses (down = true) or releases a key (GLFW code). */
    void pressKey(int glfwKey, boolean down);

    /** Presses and releases a key. */
    void tapKey(int glfwKey);

    /** Holds the key for holdMs and releases it. */
    void holdKey(int glfwKey, long holdMs);

    /** Types the text as if it came from a keyboard (only goes somewhere if a text field is open). */
    void typeText(String text);

    /** Presses or releases a mouse button ({@link CryoKeys#MOUSE_LEFT}...). */
    void mouseButton(int button, boolean down);

    /** Clicks a mouse button. */
    void click(int button);

    /** Moves the cursor to x,y (game window pixels). */
    void setCursor(float x, float y);

    /** Moves the cursor/camera by dx,dy (with the cursor grabbed this turns the camera). */
    void moveCursor(float dx, float dy);

    float getCursorX();

    float getCursorY();

    /** Mouse wheel. */
    void scroll(double dx, double dy);

    /** True while playing (cursor grabbed: no menu, inventory or chat open). */
    boolean isCursorGrabbed();

    int getWindowWidth();

    int getWindowHeight();

    /** Current FPS of the game. */
    int getFps();

    /**
     * Sends a chat message or a command (starting with "/"): opens the chat, types and presses
     * Enter. Only works while playing (no menu open): returns false otherwise.
     */
    boolean sendChatMessage(String message);

    // ------------------------------------------------------------------ events

    /** Receives each line of the game log. Removed when the game closes. */
    void addLogListener(CryoGameLogListener listener);

    void removeLogListener(CryoGameLogListener listener);

    // ------------------------------------------------------------------ utilities

    void toast(String message);

    /** Writes to the game log and to the mod log (Launcher Mods › Logs). */
    void log(String message);

    void runOnUiThread(Runnable action);

    void runInBackground(Runnable action);

    CryoModTask schedule(long delayMs, Runnable action);

    CryoModTask scheduleRepeating(long initialDelayMs, long periodMs, Runnable action);
}
