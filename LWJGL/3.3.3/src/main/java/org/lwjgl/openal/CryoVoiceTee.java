package org.lwjgl.openal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.lwjgl.glfw.CallbackBridge;

import static org.lwjgl.system.MemoryUtil.*;

/**
 * Cryogenic Launcher: cópia do microfone capturado pelo jogo (Voice Chat via OpenAL) para a live
 * do launcher.
 * <p>
 * O Android entrega o microfone a um só app/processo por vez: com o Voice Chat ligado, a live do
 * launcher deixa de ouvir a sua voz. Aqui o mesmo áudio que o Voice Chat recebe é enviado (16 bits,
 * mono) para a live, que mistura com o som do jogo. Só roda com uma live do launcher ligada e com o
 * microfone dela ativo; senão não faz nada.
 */
final class CryoVoiceTee {
    private static final boolean ANDROID = System.getenv("POJAV_NATIVEDIR") != null;

    // AL_FORMAT_*
    private static final int MONO8 = 0x1100, MONO16 = 0x1101, STEREO8 = 0x1102, STEREO16 = 0x1103;
    private static final int MONO_FLOAT32 = 0x10010, STEREO_FLOAT32 = 0x10011;

    /** dispositivo -> {frequência, canais, tipo (0 = 8 bits, 1 = 16 bits, 2 = float)} */
    private static final Map<Long, int[]> DEVICES = new ConcurrentHashMap<Long, int[]>();

    private static volatile boolean broken;
    private static volatile boolean wanted;
    private static volatile long lastCheck;

    private CryoVoiceTee() {
    }

    static void opened(long device, int frequency, int format) {
        if (!ANDROID || device == NULL) return;
        int channels;
        int type;
        switch (format) {
            case MONO8: channels = 1; type = 0; break;
            case MONO16: channels = 1; type = 1; break;
            case STEREO8: channels = 2; type = 0; break;
            case STEREO16: channels = 2; type = 1; break;
            case MONO_FLOAT32: channels = 1; type = 2; break;
            case STEREO_FLOAT32: channels = 2; type = 2; break;
            default: return; //formato desconhecido: não copia
        }
        DEVICES.put(device, new int[]{frequency, channels, type});
    }

    static void closed(long device) {
        DEVICES.remove(device);
    }

    /** Info do dispositivo, ou null se não deve copiar agora. */
    private static int[] target(long device, int samples) {
        if (!ANDROID || broken || samples <= 0) return null;
        int[] info = DEVICES.get(device);
        if (info == null) return null;
        long now = System.currentTimeMillis();
        if (now - lastCheck > 500L) {
            lastCheck = now;
            try {
                wanted = CallbackBridge.nativeVoiceTeeWanted();
            } catch (Throwable t) {
                //biblioteca do launcher antiga: desliga de vez
                broken = true;
                return null;
            }
        }
        return wanted ? info : null;
    }

    private static void send(byte[] pcm, int rate) {
        try {
            CallbackBridge.nativeVoiceCapture(pcm, pcm.length, rate);
        } catch (Throwable t) {
            broken = true;
        }
    }

    private static void put(byte[] out, int index, int value) {
        if (value > 32767) value = 32767;
        else if (value < -32768) value = -32768;
        out[index * 2] = (byte) value;
        out[index * 2 + 1] = (byte) (value >> 8);
    }

    static void captured(long device, long address, int samples) {
        int[] info = target(device, samples);
        if (info == null || address == NULL) return;
        int channels = info[1];
        byte[] out = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                int idx = i * channels + c;
                switch (info[2]) {
                    case 0: sum += ((memGetByte(address + idx) & 0xFF) - 128) << 8; break;
                    case 1: sum += memGetShort(address + idx * 2L); break;
                    default: sum += (int) (memGetFloat(address + idx * 4L) * 32767f); break;
                }
            }
            put(out, i, sum / channels);
        }
        send(out, info[0]);
    }

    static void captured(long device, short[] buffer, int samples) {
        int[] info = target(device, samples);
        if (info == null || buffer == null || info[2] != 1) return;
        int channels = info[1];
        int n = Math.min(samples, buffer.length / Math.max(1, channels));
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) sum += buffer[i * channels + c];
            put(out, i, sum / channels);
        }
        send(out, info[0]);
    }

    static void captured(long device, int[] buffer, int samples) {
        int[] info = target(device, samples);
        if (info == null || buffer == null) return;
        //int[] com amostras de 16 bits empacotadas (2 por int, little-endian)
        if (info[2] != 1) return;
        int channels = info[1];
        int total = Math.min(samples * channels, buffer.length * 2);
        int n = total / channels;
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            int sum = 0;
            for (int c = 0; c < channels; c++) {
                int idx = i * channels + c;
                int word = buffer[idx >> 1];
                sum += (short) ((idx & 1) == 0 ? word : (word >> 16));
            }
            put(out, i, sum / channels);
        }
        send(out, info[0]);
    }

    static void captured(long device, float[] buffer, int samples) {
        int[] info = target(device, samples);
        if (info == null || buffer == null || info[2] != 2) return;
        int channels = info[1];
        int n = Math.min(samples, buffer.length / Math.max(1, channels));
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            float sum = 0f;
            for (int c = 0; c < channels; c++) sum += buffer[i * channels + c];
            put(out, i, (int) (sum / channels * 32767f));
        }
        send(out, info[0]);
    }
}
