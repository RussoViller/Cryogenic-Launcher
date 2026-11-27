package org.lwjgl.util.tinyfd;

import java.nio.ByteBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.CallbackBridge;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Cryogenic Launcher: no Android não existe zenity/kdialog, e o tinyfd cai no modo console
 * (lê do stdin e retorna null). Aqui o diálogo de abrir arquivo é enviado ao seletor de
 * arquivos do Android pelo launcher; o arquivo escolhido é copiado para o cache do app e o
 * caminho real é devolvido ao mod.
 */
final class AndroidFileDialog {
    /** Marcador (comparado por identidade): o launcher não atendeu, usar o tinyfd nativo. */
    static final String UNAVAILABLE = new String("tinyfd-native");

    private static final boolean ANDROID = System.getenv("POJAV_NATIVEDIR") != null;

    private AndroidFileDialog() {
    }

    static String str(CharSequence value) {
        return value == null ? null : value.toString();
    }

    static String str(ByteBuffer value) {
        return value == null ? null : memUTF8(memAddress(value));
    }

    static String[] patterns(PointerBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        int from = buffer.position();
        int count = buffer.remaining();
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            long address = buffer.get(from + i);
            result[i] = address == NULL ? "" : memUTF8(address);
        }
        return result;
    }

    /**
     * @return {@link #UNAVAILABLE} para seguir com o tinyfd nativo, null se o usuário cancelou,
     * ou o(s) caminho(s) escolhido(s) separados por '|' (mesmo formato do tinyfd)
     */
    static String open(String title, String defaultPath, String[] patterns, String description, boolean multiple) {
        if (!ANDROID || "tinyfd_query".equals(title)) {
            return UNAVAILABLE;
        }
        String joined = null;
        if (patterns != null && patterns.length > 0) {
            StringBuilder builder = new StringBuilder();
            for (String pattern : patterns) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(pattern);
            }
            joined = builder.toString();
        }
        String result;
        try {
            result = CallbackBridge.nativeOpenFileDialog(title, defaultPath, joined, description, multiple);
        } catch (Throwable t) {
            // libpojavexec antiga / ausente: comportamento original
            return UNAVAILABLE;
        }
        if (result == null) {
            return UNAVAILABLE;
        }
        return result.isEmpty() ? null : result;
    }
}
