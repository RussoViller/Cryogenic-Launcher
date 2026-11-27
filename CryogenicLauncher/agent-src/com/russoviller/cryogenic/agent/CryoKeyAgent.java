package com.russoviller.cryogenic.agent;

import java.lang.instrument.Instrumentation;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;

public final class CryoKeyAgent {
    private static final String KEY_HOLDER = "moe.yushi.authlibinjector.transform.support.YggdrasilKeyTransformUnit";

    private CryoKeyAgent() {
    }

    public static void premain(String args, Instrumentation inst) {
        addKeys(args);
    }

    public static void agentmain(String args, Instrumentation inst) {
        addKeys(args);
    }

    @SuppressWarnings("unchecked")
    private static void addKeys(String args) {
        if (args == null || args.trim().isEmpty()) return;
        try {
            Class<?> holder = Class.forName(KEY_HOLDER, true, ClassLoader.getSystemClassLoader());
            List<PublicKey> keys = (List<PublicKey>) holder.getField("PUBLIC_KEYS").get(null);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            for (String part : args.split(",")) {
                String encoded = part.trim();
                if (encoded.isEmpty()) continue;
                byte[] der = Base64.getDecoder().decode(encoded);
                PublicKey key = factory.generatePublic(new X509EncodedKeySpec(der));
                if (!keys.contains(key)) keys.add(key);
            }
            System.out.println("[CryoKeyAgent] Trusted upstream Yggdrasil keys: " + keys.size());
        } catch (Throwable t) {
            System.err.println("[CryoKeyAgent] Failed to add upstream Yggdrasil key: " + t);
        }
    }
}
