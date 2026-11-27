package com.russoviller.cryogenic.cmod.api;

/** API 2: Launcher events. Override only what you need. Main thread. */
public interface CryoLauncherListener {
    /** The game is starting with this version (the game opens in its own screen/process). */
    default void onGameLaunch(String versionName) throws Exception {}

    /** The Launcher came back to the screen. */
    default void onLauncherForeground() throws Exception {}

    /** The Launcher left the screen (home, another app or the game). */
    default void onLauncherBackground() throws Exception {}

    /** Another mod was enabled (it is already running). */
    default void onModEnabled(String modId) throws Exception {}

    /** Another mod was disabled. */
    default void onModDisabled(String modId) throws Exception {}

    /** The selected game version changed (null = none). */
    default void onGameVersionSelected(String versionName) throws Exception {}
}
