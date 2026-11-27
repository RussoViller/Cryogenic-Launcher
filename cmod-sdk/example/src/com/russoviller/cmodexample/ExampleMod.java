package com.russoviller.cmodexample;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.russoviller.cryogenic.cmod.api.CryoMod;
import com.russoviller.cryogenic.cmod.api.CryoModApi;
import com.russoviller.cryogenic.cmod.api.CryoModButton;
import com.russoviller.cryogenic.cmod.api.CryoModScreen;
import com.russoviller.cryogenic.cmod.api.CryoModUi;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

/**
 * Example mod for Cryogenic Launcher.
 *
 * Adds ONE feature to the CryoMods page ("Example Tools"). When opened, it shows three cards built
 * with the launcher's UI pieces (CryoModUi):
 *  1. File info: pick a file and show its name and size.
 *  2. My worlds: list the worlds in the game folder (in the background, with a progress bar).
 *  3. Controls: text field, switch and slider, saving to the mod's data folder.
 */
public final class ExampleMod implements CryoMod {

    private CryoModApi api;

    @Override
    public void onEnable(CryoModApi api) {
        this.api = api;
        api.log("Example Tools enabled (API v" + api.getApiVersion() + ", launcher " + api.getLauncherVersion() + ")");

        // Each mod has only one feature: this is the button shown on the CryoMods page.
        api.addButton(new CryoModButton(
                "example_tools",
                "Example Tools",
                "Example files, worlds and controls",
                null,               // null = use the mod's icon.png
                this::open));
    }

    @Override
    public void onDisable() {
        // the launcher removes the button by itself
        api = null;
    }

    private void open(CryoModScreen screen) {
        CryoModUi ui = screen.getUi();
        LinearLayout page = ui.column();
        page.addView(ui.title("Example Tools"));
        page.addView(ui.caption("Example mod for Cryogenic Launcher (CryoMod API)."));
        page.addView(fileCard(screen, ui));
        page.addView(worldsCard(ui));
        page.addView(controlsCard(ui));
        screen.setContent(ui.scroll(page));
    }

    // ------------------------------------------------------------ 1. File info

    private LinearLayout fileCard(final CryoModScreen screen, CryoModUi ui) {
        final Activity activity = screen.getActivity();
        LinearLayout card = ui.card();
        card.addView(ui.title("File info"));
        final TextView result = ui.text("No file picked.");
        card.addView(ui.button("Pick a file", v -> screen.pickFile(new String[]{"*/*"}, uri -> {
            result.setText(uri == null ? "Canceled." : describe(activity, uri));
        })));
        card.addView(result);
        return card;
    }

    private static String describe(Activity activity, Uri uri) {
        String name = uri.getLastPathSegment();
        long size = -1;
        try (Cursor c = activity.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int s = c.getColumnIndex(OpenableColumns.SIZE);
                if (n >= 0) name = c.getString(n);
                if (s >= 0 && !c.isNull(s)) size = c.getLong(s);
            }
        } catch (Exception ignored) {
        }
        return "Name: " + name + "\nSize: " + (size < 0 ? "unknown" : formatSize(size));
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ------------------------------------------------------------ 2. My worlds

    private LinearLayout worldsCard(CryoModUi ui) {
        LinearLayout card = ui.card();
        card.addView(ui.title("My worlds"));
        final ProgressBar progress = ui.progress(true);
        final TextView list = ui.text("Tap Refresh.");
        progress.setVisibility(android.view.View.GONE);
        card.addView(ui.secondaryButton("Refresh", v -> loadWorlds(progress, list)));
        card.addView(progress);
        card.addView(list);
        loadWorlds(progress, list);
        return card;
    }

    private void loadWorlds(final ProgressBar progress, final TextView list) {
        final CryoModApi api = this.api;
        if (api == null) return;
        progress.setVisibility(android.view.View.VISIBLE);
        // disk work off the main thread; the screen is updated back on the main thread
        api.runInBackground(() -> {
            File game = api.getGameDirectory();
            File saves = game == null ? null : new File(game, "saves");
            File[] worlds = saves == null ? null : saves.listFiles(File::isDirectory);
            final String text;
            if (worlds == null || worlds.length == 0) {
                text = "No worlds found.";
            } else {
                Arrays.sort(worlds, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                StringBuilder sb = new StringBuilder(worlds.length + " world(s):\n");
                for (File w : worlds) sb.append("• ").append(w.getName()).append('\n');
                text = sb.toString().trim();
            }
            api.runOnUiThread(() -> {
                progress.setVisibility(android.view.View.GONE);
                list.setText(text);
            });
        });
    }

    // ------------------------------------------------------------ 3. Controls

    private LinearLayout controlsCard(final CryoModUi ui) {
        LinearLayout card = ui.card();
        card.addView(ui.title("Controls"));
        final EditText name = ui.input("Your name");
        card.addView(name);

        final TextView volumeText = ui.caption("Volume: 50");
        card.addView(ui.switchButton("Turbo mode", false, (b, on) -> {
            if (api != null) api.toast(on ? "Turbo on" : "Turbo off");
        }));
        card.addView(volumeText);
        card.addView(ui.slider(100, 50, value -> volumeText.setText("Volume: " + value)));

        LinearLayout row = ui.row();
        TextView hello = ui.button("Say hi", v -> {
            String who = name.getText().toString().trim();
            if (api != null) api.toast("Hi, " + (who.isEmpty() ? "player" : who) + "!");
        });
        TextView save = ui.secondaryButton("Save name", v -> saveName(name.getText().toString()));
        row.addView(hello, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(save, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(row);
        return card;
    }

    private void saveName(final String value) {
        final CryoModApi api = this.api;
        if (api == null) return;
        api.runInBackground(() -> {
            try {
                File file = new File(api.getDataDirectory(), "name.txt");
                java.nio.file.Files.write(file.toPath(), value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                api.toast("Saved to " + file.getName());
            } catch (Exception e) {
                api.log("Save failed: " + e);
                api.toast("Could not save");
            }
        });
    }
}
