package com.russoviller.cryogenic.cmod.api;

/**
 * API 2: one line of the game log (latestlog). Chat messages arrive as lines with "[CHAT]".
 * Called on the thread that reads the game output: be fast (use runInBackground/runOnUiThread
 * for heavy work).
 */
public interface CryoGameLogListener {
    void onLine(String line) throws Exception;
}
