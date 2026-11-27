/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 * MACHINE GENERATED FILE, DO NOT EDIT
 *
 * Cryogenic Launcher: mesma API do ALC11 original do LWJGL; as funções de captura também
 * entregam o áudio do microfone (Voice Chat) para a live do launcher (CryoVoiceTee).
 */
package org.lwjgl.openal;


import java.nio.*;

import org.lwjgl.system.*;

import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.JNI.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Native bindings to ALC 1.1 functionality. */
public class ALC11 extends ALC10 {

    /** Context creation attributes. */
    public static final int
        ALC_MONO_SOURCES   = 0x1010,
        ALC_STEREO_SOURCES = 0x1011;

    /** String queries. */
    public static final int
        ALC_DEFAULT_ALL_DEVICES_SPECIFIER    = 0x1012,
        ALC_ALL_DEVICES_SPECIFIER            = 0x1013,
        ALC_CAPTURE_DEVICE_SPECIFIER         = 0x310,
        ALC_CAPTURE_DEFAULT_DEVICE_SPECIFIER = 0x311;

    /** Integer queries. */
    public static final int ALC_CAPTURE_SAMPLES = 0x312;

    protected ALC11() {
        throw new UnsupportedOperationException();
    }

    // --- [ alcCaptureOpenDevice ] ---

    /** Unsafe version of: {@link #alcCaptureOpenDevice CaptureOpenDevice} */
    public static long nalcCaptureOpenDevice(long deviceName, int frequency, int format, int samples) {
        long __functionAddress = ALC.getICD().alcCaptureOpenDevice;
        if (CHECKS) {
            check(__functionAddress);
        }
        long device = invokePP(deviceName, frequency, format, samples, __functionAddress);
        CryoVoiceTee.opened(device, frequency, format);
        return device;
    }

    @NativeType("ALCdevice *")
    public static long alcCaptureOpenDevice(@NativeType("ALCchar const *") ByteBuffer deviceName, @NativeType("ALCuint") int frequency, @NativeType("ALCenum") int format, @NativeType("ALCsizei") int samples) {
        if (CHECKS) {
            checkNT1Safe(deviceName);
        }
        return nalcCaptureOpenDevice(memAddressSafe(deviceName), frequency, format, samples);
    }

    @NativeType("ALCdevice *")
    public static long alcCaptureOpenDevice(@NativeType("ALCchar const *") CharSequence deviceName, @NativeType("ALCuint") int frequency, @NativeType("ALCenum") int format, @NativeType("ALCsizei") int samples) {
        MemoryStack stack = stackGet(); int stackPointer = stack.getPointer();
        try {
            stack.nUTF8Safe(deviceName, true);
            long deviceNameEncoded = deviceName == null ? NULL : stack.getPointerAddress();
            return nalcCaptureOpenDevice(deviceNameEncoded, frequency, format, samples);
        } finally {
            stack.setPointer(stackPointer);
        }
    }

    // --- [ alcCaptureCloseDevice ] ---

    @NativeType("ALCboolean")
    public static boolean alcCaptureCloseDevice(@NativeType("ALCdevice *") long device) {
        long __functionAddress = ALC.getICD().alcCaptureCloseDevice;
        if (CHECKS) {
            check(__functionAddress);
            check(device);
        }
        CryoVoiceTee.closed(device);
        return invokePZ(device, __functionAddress);
    }

    // --- [ alcCaptureStart ] ---

    @NativeType("ALCvoid")
    public static void alcCaptureStart(@NativeType("ALCdevice *") long device) {
        long __functionAddress = ALC.getICD().alcCaptureStart;
        if (CHECKS) {
            check(__functionAddress);
            check(device);
        }
        invokePV(device, __functionAddress);
    }

    // --- [ alcCaptureStop ] ---

    @NativeType("ALCvoid")
    public static void alcCaptureStop(@NativeType("ALCdevice *") long device) {
        long __functionAddress = ALC.getICD().alcCaptureStop;
        if (CHECKS) {
            check(__functionAddress);
            check(device);
        }
        invokePV(device, __functionAddress);
    }

    // --- [ alcCaptureSamples ] ---

    /** Unsafe version of: {@link #alcCaptureSamples CaptureSamples} */
    public static void nalcCaptureSamples(long device, long buffer, int samples) {
        long __functionAddress = ALC.getICD().alcCaptureSamples;
        if (CHECKS) {
            check(__functionAddress);
            check(device);
        }
        invokePPV(device, buffer, samples, __functionAddress);
        CryoVoiceTee.captured(device, buffer, samples);
    }

    @NativeType("ALCvoid")
    public static void alcCaptureSamples(@NativeType("ALCdevice *") long device, @NativeType("ALCvoid *") ByteBuffer buffer, @NativeType("ALCsizei") int samples) {
        nalcCaptureSamples(device, memAddress(buffer), samples);
    }

    @NativeType("ALCvoid")
    public static void alcCaptureSamples(@NativeType("ALCdevice *") long device, @NativeType("ALCvoid *") ShortBuffer buffer, @NativeType("ALCsizei") int samples) {
        nalcCaptureSamples(device, memAddress(buffer), samples);
    }

    @NativeType("ALCvoid")
    public static void alcCaptureSamples(@NativeType("ALCdevice *") long device, @NativeType("ALCvoid *") IntBuffer buffer, @NativeType("ALCsizei") int samples) {
        nalcCaptureSamples(device, memAddress(buffer), samples);
    }

    @NativeType("ALCvoid")
    public static void alcCaptureSamples(@NativeType("ALCdevice *") long device, @NativeType("ALCvoid *") FloatBuffer buffer, @NativeType("ALCsizei") int samples) {
        nalcCaptureSamples(device, memAddress(buffer), samples);
    }

    @NativeType("ALCvoid")
    public static void alcCaptureSamples(@NativeType("ALCdevice *") long device, @NativeType("ALCvoid *") short[] buffer, @NativeType("ALCsizei") int samples) {
        long __functionAddress = ALC.getICD().alcCaptureSamples;
        if (CHECKS) {
            check(__functionAddress);
            check(device);
        }
        invokePPV(device, buffer, samples, __functionAddress);
        CryoVoiceTee.captured(device, buffer, samples);
    }

    @NativeType("ALCvoid")
    public static void alcCaptureSamples(@NativeType("ALCdevice *") long device, @NativeType("ALCvoid *") int[] buffer, @NativeType("ALCsizei") int samples) {
        long __functionAddress = ALC.getICD().alcCaptureSamples;
        if (CHECKS) {
            check(__functionAddress);
            check(device);
        }
        invokePPV(device, buffer, samples, __functionAddress);
        CryoVoiceTee.captured(device, buffer, samples);
    }

    @NativeType("ALCvoid")
    public static void alcCaptureSamples(@NativeType("ALCdevice *") long device, @NativeType("ALCvoid *") float[] buffer, @NativeType("ALCsizei") int samples) {
        long __functionAddress = ALC.getICD().alcCaptureSamples;
        if (CHECKS) {
            check(__functionAddress);
            check(device);
        }
        invokePPV(device, buffer, samples, __functionAddress);
        CryoVoiceTee.captured(device, buffer, samples);
    }

}
