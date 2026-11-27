package com.russoviller.cryogenic.cmod.api;

import android.app.Activity;
import android.net.Uri;
import android.view.View;

/**
 * Área da tela CryoMods aberta para o botão de um mod. O mod mostra a própria interface com Views
 * do Android ({@link #setContent}) e pode pedir arquivos/pastas ao usuário.
 * Use só na thread principal.
 */
public interface CryoModScreen {

    /** Resultado de um seletor de arquivo/pasta: null se o usuário cancelou. */
    interface FileCallback {
        void onResult(Uri uri);
    }

    /** Activity do Launcher (para criar Views, diálogos, ContentResolver...). */
    Activity getActivity();

    /** Mostra esta View na área do mod (troca a anterior). */
    void setContent(View view);

    /** Troca o título da área do mod. */
    void setTitle(String title);

    /**
     * Mostra ou esconde a barra de cima da área do mod (título e botão de fechar). Escondida, a
     * View do mod ocupa a área inteira e o mod pode desenhar a interface do jeito que quiser
     * (use {@link #close()} no seu próprio botão de fechar).
     */
    void setHeaderVisible(boolean visible);

    /**
     * Peças de interface no estilo do Launcher (cores do tema atual, vidro, botões em degradê,
     * campos, interruptores...). Opcional: o mod pode usar qualquer View do Android.
     */
    CryoModUi getUi();

    /** Fecha a área do mod. */
    void close();

    /** Seletor de arquivo do sistema, ex.: new String[]{"application/zip"} (vazio = qualquer arquivo). */
    void pickFile(String[] mimeTypes, FileCallback callback);

    /** Seletor para criar/salvar um arquivo novo. */
    void createFile(String suggestedName, String mimeType, FileCallback callback);

    /** Seletor de pasta (acesso à pasta inteira, via DocumentFile/DocumentsContract). */
    void pickFolder(FileCallback callback);
}
