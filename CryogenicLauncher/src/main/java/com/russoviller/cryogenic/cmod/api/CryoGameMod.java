package com.russoviller.cryogenic.cmod.api;

/**
 * API 2: in-game side of a Launcher Mod. Declare the class in cmod.json as "gameMain". It runs in
 * the game screen (the game process), not in the Launcher: it can draw over the game, press
 * keys, move the mouse, type in the chat, read the game log and react to it.
 *
 * <p>The same classes.dex is used, but it is a different process: static fields of the Launcher
 * side are not visible here. Use {@link CryoGameApi#getPreferences()} (shared file) to pass
 * settings from the Launcher side to the game side.</p>
 */
public interface CryoGameMod {
    /** The game screen opened (the game is starting). Main thread. */
    void onGameStart(CryoGameApi api) throws Exception;

    /** The game closed (exitCode 0 = normal). Main thread. Not called if the process is killed. */
    default void onGameExit(int exitCode) throws Exception {}
}
