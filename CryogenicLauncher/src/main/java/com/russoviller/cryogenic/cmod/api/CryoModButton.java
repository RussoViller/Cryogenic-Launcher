package com.russoviller.cryogenic.cmod.api;

import android.graphics.Bitmap;

/**
 * Botão de um mod na tela CryoMods. Ao tocar, o Launcher abre uma área para o mod
 * ({@link CryoModScreen}) e chama {@link Action#onClick}.
 */
public final class CryoModButton {

    /** O que acontece ao tocar no botão. */
    public interface Action {
        void onClick(CryoModScreen screen) throws Exception;
    }

    private final String id;
    private final String title;
    private final String description;
    private final Bitmap icon;
    private final Action action;

    /**
     * @param id          identificador do botão (único dentro do mod)
     * @param title       nome do botão
     * @param description texto curto embaixo do nome (pode ser null)
     * @param icon        ícone (pode ser null: usa o ícone do mod)
     * @param action      o que fazer ao tocar
     */
    public CryoModButton(String id, String title, String description, Bitmap icon, Action action) {
        if (id == null || id.isEmpty()) throw new IllegalArgumentException("id");
        if (action == null) throw new IllegalArgumentException("action");
        this.id = id;
        this.title = title == null ? id : title;
        this.description = description;
        this.icon = icon;
        this.action = action;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Bitmap getIcon() { return icon; }
    public Action getAction() { return action; }
}
