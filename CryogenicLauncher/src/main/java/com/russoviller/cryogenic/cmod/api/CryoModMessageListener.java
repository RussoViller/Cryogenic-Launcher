package com.russoviller.cryogenic.cmod.api;

/** API 2: message sent by another mod ({@link CryoModApi#sendMessage}). Main thread. */
public interface CryoModMessageListener {
    void onMessage(String fromModId, String channel, Object payload) throws Exception;
}
