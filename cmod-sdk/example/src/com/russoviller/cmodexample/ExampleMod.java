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
 * Mod de exemplo do Cryogenic Launcher.
 *
 * Adiciona UMA função na tela CryoMods ("Example Tools"). Ao abrir, mostra três cartões feitos com
 * as peças de interface do Launcher (CryoModUi):
 *  1. Informações de arquivo: escolhe um arquivo e mostra nome e tamanho.
 *  2. Meus mundos: lista os mundos da pasta do jogo (em segundo plano, com barra de progresso).
 *  3. Controles: campo de texto, interruptor e slider, salvando no arquivo de dados do mod.
 */
public final class ExampleMod implements CryoMod {

    private CryoModApi api;

    @Override
    public void onEnable(CryoModApi api) {
        this.api = api;
        api.log("Example Tools ligado (API v" + api.getApiVersion() + ", launcher " + api.getLauncherVersion() + ")");

        // Cada mod tem uma função só: este é o botão que aparece na tela CryoMods.
        api.addButton(new CryoModButton(
                "example_tools",
                "Example Tools",
                "Arquivos, mundos e controles de exemplo",
                null,               // null = usa o icon.png do mod
                this::open));
    }

    @Override
    public void onDisable() {
        // o launcher remove o botão sozinho
        api = null;
    }

    private void open(CryoModScreen screen) {
        CryoModUi ui = screen.getUi();
        LinearLayout page = ui.column();
        page.addView(ui.title("Example Tools"));
        page.addView(ui.caption("Mod de exemplo do Cryogenic Launcher (CryoMod API)."));
        page.addView(fileCard(screen, ui));
        page.addView(worldsCard(ui));
        page.addView(controlsCard(ui));
        screen.setContent(ui.scroll(page));
    }

    // ------------------------------------------------------------ 1. Informações de arquivo

    private LinearLayout fileCard(final CryoModScreen screen, CryoModUi ui) {
        final Activity activity = screen.getActivity();
        LinearLayout card = ui.card();
        card.addView(ui.title("Informações de arquivo"));
        final TextView result = ui.text("Nenhum arquivo escolhido.");
        card.addView(ui.button("Escolher arquivo", v -> screen.pickFile(new String[]{"*/*"}, uri -> {
            result.setText(uri == null ? "Cancelado." : describe(activity, uri));
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
        return "Nome: " + name + "\nTamanho: " + (size < 0 ? "desconhecido" : formatSize(size));
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // ------------------------------------------------------------ 2. Meus mundos

    private LinearLayout worldsCard(CryoModUi ui) {
        LinearLayout card = ui.card();
        card.addView(ui.title("Meus mundos"));
        final ProgressBar progress = ui.progress(true);
        final TextView list = ui.text("Toque em Atualizar.");
        progress.setVisibility(android.view.View.GONE);
        card.addView(ui.secondaryButton("Atualizar", v -> loadWorlds(progress, list)));
        card.addView(progress);
        card.addView(list);
        loadWorlds(progress, list);
        return card;
    }

    private void loadWorlds(final ProgressBar progress, final TextView list) {
        final CryoModApi api = this.api;
        if (api == null) return;
        progress.setVisibility(android.view.View.VISIBLE);
        // disco fora da thread principal; a tela é atualizada de volta na principal
        api.runInBackground(() -> {
            File game = api.getGameDirectory();
            File saves = game == null ? null : new File(game, "saves");
            File[] worlds = saves == null ? null : saves.listFiles(File::isDirectory);
            final String text;
            if (worlds == null || worlds.length == 0) {
                text = "Nenhum mundo encontrado.";
            } else {
                Arrays.sort(worlds, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
                StringBuilder sb = new StringBuilder(worlds.length + " mundo(s):\n");
                for (File w : worlds) sb.append("• ").append(w.getName()).append('\n');
                text = sb.toString().trim();
            }
            api.runOnUiThread(() -> {
                progress.setVisibility(android.view.View.GONE);
                list.setText(text);
            });
        });
    }

    // ------------------------------------------------------------ 3. Controles

    private LinearLayout controlsCard(final CryoModUi ui) {
        LinearLayout card = ui.card();
        card.addView(ui.title("Controles"));
        final EditText name = ui.input("Seu nome");
        card.addView(name);

        final TextView volumeText = ui.caption("Volume: 50");
        card.addView(ui.switchButton("Modo turbo", false, (b, on) -> {
            if (api != null) api.toast(on ? "Turbo ligado" : "Turbo desligado");
        }));
        card.addView(volumeText);
        card.addView(ui.slider(100, 50, value -> volumeText.setText("Volume: " + value)));

        LinearLayout row = ui.row();
        TextView hello = ui.button("Dizer oi", v -> {
            String who = name.getText().toString().trim();
            if (api != null) api.toast("Oi, " + (who.isEmpty() ? "player" : who) + "!");
        });
        TextView save = ui.secondaryButton("Salvar nome", v -> saveName(name.getText().toString()));
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
                api.toast("Salvo em " + file.getName());
            } catch (Exception e) {
                api.log("Falha ao salvar: " + e);
                api.toast("Não foi possível salvar");
            }
        });
    }
}
