package com.russoviller.cryogenic.cmod.api;

import android.content.Context;

import java.io.File;

/**
 * O que o Launcher oferece para um mod (.cmod). Versão da API: {@link #API_VERSION}.
 */
public interface CryoModApi {

    /** Versão desta API. Um mod com "apiVersion" maior que esta não é carregado. */
    int API_VERSION = 1;

    /** Versão da API do Launcher instalado. */
    int getApiVersion();

    /** Versão do Launcher (ex.: "r1.8.0"). */
    String getLauncherVersion();

    /** Contexto da aplicação do Launcher. */
    Context getContext();

    /** "id" do cmod.json. */
    String getModId();

    /** Pasta com os arquivos do .cmod (cmod.json, ícone e a pasta assets/). Só leitura. */
    File getModDirectory();

    /** Pasta só do mod para salvar dados (fica guardada entre aberturas do Launcher). */
    File getDataDirectory();

    /** Pasta .minecraft atual do Launcher (onde ficam versions, saves, mods...). Pode ser null. */
    File getGameDirectory();

    /**
     * Adiciona a função (botão) do mod na tela CryoMods. Cada mod tem UMA função só: chamar de novo
     * com o mesmo id atualiza o botão; com outro id lança IllegalStateException (remova a anterior
     * com {@link #removeButton} antes, se quiser trocar).
     */
    void addButton(CryoModButton button);

    /** Remove a função deste mod. */
    void removeButton(String id);

    /** Mensagem curta na tela (Toast). Pode ser chamado de qualquer thread. */
    void toast(String message);

    /** Escreve no log do Launcher (latestlog), com o id do mod. */
    void log(String message);

    /** Roda na thread principal (para mexer em Views). */
    void runOnUiThread(Runnable action);

    /** Roda numa thread de fundo (arquivos, rede, conversões). */
    void runInBackground(Runnable action);
}
