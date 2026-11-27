package com.russoviller.cryohud;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.russoviller.cryogenic.cmod.api.CryoGameApi;
import com.russoviller.cryogenic.cmod.api.CryoGameMod;
import com.russoviller.cryogenic.cmod.api.CryoKeys;
import com.russoviller.cryogenic.cmod.api.CryoModPrefs;
import com.russoviller.cryogenic.cmod.api.CryoModUi;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * In-game side of CryoHUD (runs in the game screen): HUD pill, quick buttons and chat alerts.
 */
public final class HudGameMod implements CryoGameMod {

    private static final int[] GRAVITY = {
            Gravity.TOP | Gravity.START, Gravity.TOP | Gravity.END,
            Gravity.BOTTOM | Gravity.START, Gravity.BOTTOM | Gravity.END
    };

    private CryoGameApi api;
    private boolean walking;

    @Override
    public void onGameStart(final CryoGameApi api) {
        this.api = api;
        final CryoModPrefs prefs = api.getPreferences();
        final CryoModUi ui = api.getUi();
        final boolean fps = prefs.getBoolean("fps", true);
        final boolean clock = prefs.getBoolean("clock", true);
        final int corner = Math.max(0, Math.min(3, prefs.getInt("corner", 1)));

        // ---------------- HUD pill (not clickable: touches go to the game)
        if (fps || clock) {
            final TextView pill = ui.chip("", false, null);
            pill.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            api.addOverlayView(pill, GRAVITY[corner], 12, 10);
            final SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.ROOT);
            api.scheduleRepeating(0, 500, () -> {
                StringBuilder sb = new StringBuilder();
                if (fps) sb.append(api.getFps()).append(" FPS");
                if (fps && clock) sb.append("  ·  ");
                if (clock) sb.append(time.format(new Date()));
                pill.setText(sb);
            });
        }

        // ---------------- quick buttons (clickable)
        LinearLayout column = new LinearLayout(api.getActivity());
        column.setOrientation(LinearLayout.VERTICAL);
        if (prefs.getBoolean("cmdButton", true)) {
            final String command = prefs.getString("command", "/time set day");
            TextView cmd = ui.chip("⚡", true, v -> {
                if (command.isEmpty()) return;
                if (!api.sendChatMessage(command)) api.toast("Close the menu first");
            });
            cmd.setTextSize(20);
            column.addView(cmd);
        }
        if (prefs.getBoolean("walkButton", true)) {
            final TextView walk = ui.chip("🏃", false, null);
            walk.setTextSize(20);
            walk.setOnClickListener(v -> {
                walking = !walking;
                api.pressKey(CryoKeys.W, walking);
                walk.setBackground(walking ? ui.gradientBackground(14) : ui.glassBackground(14));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = ui.dp(8);
            column.addView(walk, lp);
        }
        if (column.getChildCount() > 0) api.addOverlayView(column, Gravity.CENTER_VERTICAL | Gravity.END, 14, 0);

        // ---------------- chat alerts (log lines)
        final String keyword = prefs.getString("keyword", "").toLowerCase(Locale.ROOT);
        if (!keyword.isEmpty()) {
            api.addLogListener(line -> {
                if (!line.contains("[CHAT]") || !line.toLowerCase(Locale.ROOT).contains(keyword)) return;
                final String text = line.substring(line.indexOf("[CHAT]") + 6).trim();
                api.runOnUiThread(() -> alert(ui, text));
            });
        }
        api.log("CryoHUD ready on " + api.getVersionName() + " (Minecraft " + api.getMinecraftVersion() + ")");
    }

    private void alert(CryoModUi ui, String text) {
        final TextView banner = ui.chip("💬 " + text, true, null);
        banner.setTextColor(Color.BLACK);
        banner.setMaxWidth(ui.dp(420));
        api.addOverlayView(banner, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 48);
        api.schedule(4000, () -> api.removeOverlayView(banner));
    }

    @Override
    public void onGameExit(int exitCode) {
        if (walking && api != null) api.pressKey(CryoKeys.W, false);
        api = null;
    }
}
