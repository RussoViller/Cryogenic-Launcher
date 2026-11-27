package com.russoviller.cryogenic.cmod.api;

/** API 2: a scheduled action ({@link CryoModApi#schedule}, {@link CryoGameApi#schedule}). */
public interface CryoModTask {
    /** Stops the task (does nothing if it already ran or was cancelled). */
    void cancel();

    boolean isCancelled();
}
