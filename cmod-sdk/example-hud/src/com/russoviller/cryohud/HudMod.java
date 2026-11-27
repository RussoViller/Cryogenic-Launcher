package com.russoviller.cryohud;

import android.widget.EditText;
import android.widget.LinearLayout;

import com.russoviller.cryogenic.cmod.api.CryoLauncherListener;
import com.russoviller.cryogenic.cmod.api.CryoMod;
import com.russoviller.cryogenic.cmod.api.CryoModApi;
import com.russoviller.cryogenic.cmod.api.CryoModButton;
import com.russoviller.cryogenic.cmod.api.CryoModPrefs;
import com.russoviller.cryogenic.cmod.api.CryoModScreen;
import com.russoviller.cryogenic.cmod.api.CryoModUi;

/**
 * Launcher side of CryoHUD: the settings page on the CryoMods page. Everything is saved in the
 * mod preferences, which the in-game side ({@link HudGameMod}) reads when the game opens.
 */
public final class HudMod implements CryoMod {

    static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};

    private CryoModApi api;

    @Override
    public void onEnable(final CryoModApi api) {
        this.api = api;
        final CryoModPrefs prefs = api.getPreferences();
        updateBadge();

        api.addButton(new CryoModButton("cryohud", "CryoHUD", "HUD, quick command and alerts in game", null, this::open));

        // Launcher events: counts game launches (shown as a badge on the button)
        api.addLauncherListener(new CryoLauncherListener() {
            @Override
            public void onGameLaunch(String versionName) {
                prefs.putInt("launches", prefs.getInt("launches", 0) + 1);
                prefs.putString("lastVersion", versionName);
                updateBadge();
            }
        });

        // A tiny service other mods can call: api.getService("russoviller.cryohud", "launches")
        api.exposeService("launches", (Runnable) () -> api.toast("CryoHUD: " + prefs.getInt("launches", 0) + " launches"));
    }

    @Override
    public void onDisable() {
        api = null;
    }

    private void updateBadge() {
        if (api == null) return;
        int n = api.getPreferences().getInt("launches", 0);
        api.setButtonBadge(n > 0 ? String.valueOf(n) : null);
    }

    private void open(final CryoModScreen screen) {
        final CryoModUi ui = screen.getUi();
        final CryoModPrefs prefs = api.getPreferences();
        LinearLayout page = ui.column();
        page.addView(ui.title("CryoHUD"));
        page.addView(ui.caption("Settings are applied the next time the game opens. Last version: "
                + prefs.getString("lastVersion", "—") + " · launches: " + prefs.getInt("launches", 0)));

        // HUD
        LinearLayout hud = ui.card();
        hud.addView(ui.title("HUD"));
        hud.addView(ui.switchButton("Show FPS", prefs.getBoolean("fps", true), (b, on) -> prefs.putBoolean("fps", on)));
        hud.addView(ui.switchButton("Show clock", prefs.getBoolean("clock", true), (b, on) -> prefs.putBoolean("clock", on)));
        hud.addView(ui.caption("Corner"));
        hud.addView(ui.choice(CORNERS, prefs.getInt("corner", 1), (i, option) -> prefs.putInt("corner", i)));
        page.addView(hud);

        // Quick buttons
        LinearLayout quick = ui.card();
        quick.addView(ui.title("Quick buttons"));
        quick.addView(ui.checkbox("⚡ Quick command button", prefs.getBoolean("cmdButton", true), (b, on) -> prefs.putBoolean("cmdButton", on)));
        final EditText command = ui.input("Command or message");
        command.setText(prefs.getString("command", "/time set day"));
        quick.addView(command);
        quick.addView(ui.checkbox("🏃 Auto-walk button (holds W)", prefs.getBoolean("walkButton", true), (b, on) -> prefs.putBoolean("walkButton", on)));
        quick.addView(ui.button("Save command", v -> {
            prefs.putString("command", command.getText().toString().trim());
            api.toast("Saved");
        }));
        page.addView(quick);

        // Chat alerts
        LinearLayout chat = ui.card();
        chat.addView(ui.title("Chat alerts"));
        chat.addView(ui.text("Shows an alert over the game when a chat message contains this word (e.g. your nick)."));
        chat.addView(ui.listItem(null, "Keyword: " + prefs.getString("keyword", "(none)"), "Tap to change", v ->
                ui.prompt("Chat keyword", "Your nick or any word", prefs.getString("keyword", ""), text -> {
                    if (text == null) return;
                    prefs.putString("keyword", text.trim());
                    open(screen);
                })));
        page.addView(chat);

        LinearLayout danger = ui.row();
        danger.addView(ui.secondaryButton("Reset counter", v -> ui.confirm("Reset", "Reset the launch counter?", ok -> {
            if (!ok) return;
            prefs.putInt("launches", 0);
            updateBadge();
            open(screen);
        })));
        page.addView(danger);

        screen.setContent(ui.scroll(page));
    }
}
