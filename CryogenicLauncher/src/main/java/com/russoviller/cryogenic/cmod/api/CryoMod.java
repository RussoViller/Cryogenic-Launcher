package com.russoviller.cryogenic.cmod.api;

/**
 * Ponto de entrada de um mod do Launcher (.cmod).
 * <p>
 * A classe indicada em "main" no cmod.json precisa implementar esta interface e ter um construtor
 * público sem parâmetros. O Launcher chama {@link #onEnable} quando o mod é ativado (ou quando o
 * Launcher abre com ele ativo) e {@link #onDisable} quando ele é desativado ou apagado.
 * <p>
 * Tudo é chamado na thread principal (da tela). Trabalho pesado: use
 * {@link CryoModApi#runInBackground(Runnable)}.
 */
public interface CryoMod {

    /** O mod foi ativado: registre aqui os seus botões ({@link CryoModApi#addButton}). */
    void onEnable(CryoModApi api) throws Exception;

    /** O mod foi desativado/apagado: pare o que estiver rodando. Os botões são removidos sozinhos. */
    default void onDisable() {
    }
}
