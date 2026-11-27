package com.russoviller.cryogenic.ui.preview;

import android.content.SharedPreferences;
import android.os.Parcelable;

import com.tencent.mmkv.MMKV;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashMap;
import java.util.Map;

/** MMKV em memória para testes (a biblioteca nativa não existe na JVM). Um armazenamento por id. */
@Implements(MMKV.class)
public class ShadowMMKV {
    private static final Map<Long, Map<String, Object>> STORES = new HashMap<>();
    private static final Map<String, MMKV> INSTANCES = new HashMap<>();

    @RealObject
    private MMKV real;

    @Implementation
    protected static void __staticInitializer__() {
    }

    @Implementation
    protected static MMKV mmkvWithID(String id, int mode) {
        MMKV m = INSTANCES.get(id);
        if (m == null) {
            long handle = INSTANCES.size() + 1L;
            m = ReflectionHelpers.callConstructor(MMKV.class, ReflectionHelpers.ClassParameter.from(long.class, handle));
            INSTANCES.put(id, m);
            STORES.put(handle, new HashMap<>());
        }
        return m;
    }

    private Map<String, Object> store() {
        long handle = ReflectionHelpers.getField(real, "nativeHandle");
        Map<String, Object> s = STORES.get(handle);
        if (s == null) {
            s = new HashMap<>();
            STORES.put(handle, s);
        }
        return s;
    }

    @Implementation
    protected String getString(String key, String def) {
        Object v = store().get(key);
        return v instanceof String ? (String) v : def;
    }

    @Implementation
    protected int getInt(String key, int def) {
        Object v = store().get(key);
        return v instanceof Integer ? (Integer) v : def;
    }

    @Implementation
    protected long getLong(String key, long def) {
        Object v = store().get(key);
        return v instanceof Long ? (Long) v : def;
    }

    @Implementation
    protected float getFloat(String key, float def) {
        Object v = store().get(key);
        return v instanceof Float ? (Float) v : def;
    }

    @Implementation
    protected boolean getBoolean(String key, boolean def) {
        Object v = store().get(key);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    @Implementation
    protected boolean containsKey(String key) {
        return store().containsKey(key);
    }

    @Implementation
    protected <T extends Parcelable> T decodeParcelable(String key, Class<T> clazz) {
        Object v = store().get(key);
        return clazz.isInstance(v) ? clazz.cast(v) : null;
    }

    @Implementation
    protected boolean encode(String key, Parcelable value) {
        store().put(key, value);
        return true;
    }

    @Implementation
    protected boolean encode(String key, int value) {
        store().put(key, value);
        return true;
    }

    @Implementation
    protected boolean encode(String key, String value) {
        store().put(key, value);
        return true;
    }

    @Implementation
    protected int decodeInt(String key, int def) {
        return getInt(key, def);
    }

    @Implementation
    protected String decodeString(String key, String def) {
        return getString(key, def);
    }

    @Implementation
    protected void removeValueForKey(String key) {
        store().remove(key);
    }

    @Implementation
    protected long count() {
        return store().size();
    }

    @Implementation
    protected SharedPreferences.Editor putString(String key, String value) {
        store().put(key, value);
        return real;
    }

    @Implementation
    protected SharedPreferences.Editor putInt(String key, int value) {
        store().put(key, value);
        return real;
    }

    @Implementation
    protected SharedPreferences.Editor putBoolean(String key, boolean value) {
        store().put(key, value);
        return real;
    }

    @Implementation
    protected SharedPreferences.Editor remove(String key) {
        store().remove(key);
        return real;
    }

    @Implementation
    protected void apply() {
    }

    @Implementation
    protected String[] allKeys() {
        return store().keySet().toArray(new String[0]);
    }

    /** Limpa o armazenamento entre testes. */
    public static void clearAll() {
        for (Map<String, Object> s : STORES.values()) s.clear();
    }
}
