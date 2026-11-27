#include <EGL/egl.h>
#include <stdbool.h>
#include <stdint.h>
#include <string.h>
#include <time.h>
#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include "egl_loader.h"
#include "gpu_timer.h"

#define CRYO_GL_EXTENSIONS 0x1F03
#define CRYO_GL_TIMESTAMP_EXT 0x8E28
#define CRYO_GL_QUERY_COUNTER_BITS_EXT 0x8864
#define CRYO_GL_QUERY_RESULT_EXT 0x8866
#define CRYO_GL_QUERY_RESULT_AVAILABLE_EXT 0x8867
#define CRYO_GL_GPU_DISJOINT_EXT 0x8FBB
#define CRYO_SLOTS 8
#define CRYO_WINDOW_NS 1000000000LL

typedef const unsigned char* (*glGetString_t)(unsigned int);
typedef void (*glGetIntegerv_t)(unsigned int, int*);
typedef void (*glGenQueriesEXT_t)(int, unsigned int*);
typedef void (*glQueryCounterEXT_t)(unsigned int, unsigned int);
typedef void (*glGetQueryivEXT_t)(unsigned int, unsigned int, int*);
typedef void (*glGetQueryObjectuivEXT_t)(unsigned int, unsigned int, unsigned int*);
typedef void (*glGetQueryObjectui64vEXT_t)(unsigned int, unsigned int, uint64_t*);

static const char* TAG = "CryoGpuTimer";

static volatile int g_enabled = 0;
static volatile int g_percent = -1;
static volatile int g_supported = -1;
static volatile int g_reset_window = 0;

static EGLContext g_context = EGL_NO_CONTEXT;
static bool g_ready = false;
static glGetIntegerv_t p_glGetIntegerv;
static glQueryCounterEXT_t p_glQueryCounterEXT;
static glGetQueryObjectuivEXT_t p_glGetQueryObjectuivEXT;
static glGetQueryObjectui64vEXT_t p_glGetQueryObjectui64vEXT;

static unsigned int g_start[CRYO_SLOTS];
static unsigned int g_end[CRYO_SLOTS];
static bool g_pending[CRYO_SLOTS];
static int g_slot = 0;
static bool g_open = false;
static int64_t g_busy_ns = 0;
static int64_t g_window_start = 0;

static int64_t now_ns(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (int64_t) ts.tv_sec * 1000000000LL + ts.tv_nsec;
}

static void reset_state(void) {
    g_ready = false;
    g_open = false;
    g_slot = 0;
    g_busy_ns = 0;
    g_window_start = 0;
    memset(g_pending, 0, sizeof(g_pending));
}

static void* gl_proc(const char* name) {
    void* fn = eglGetProcAddress_p ? (void*) eglGetProcAddress_p(name) : NULL;
    if (fn == NULL) {
        static void* gles = NULL;
        if (gles == NULL) gles = dlopen("libGLESv2.so", RTLD_LOCAL | RTLD_LAZY);
        if (gles != NULL) fn = dlsym(gles, name);
    }
    return fn;
}

static bool setup(EGLContext ctx) {
    reset_state();
    g_context = ctx;
    glGetString_t p_glGetString = (glGetString_t) gl_proc("glGetString");
    p_glGetIntegerv = (glGetIntegerv_t) gl_proc("glGetIntegerv");
    glGenQueriesEXT_t p_glGenQueriesEXT = (glGenQueriesEXT_t) gl_proc("glGenQueriesEXT");
    glGetQueryivEXT_t p_glGetQueryivEXT = (glGetQueryivEXT_t) gl_proc("glGetQueryivEXT");
    p_glQueryCounterEXT = (glQueryCounterEXT_t) gl_proc("glQueryCounterEXT");
    p_glGetQueryObjectuivEXT = (glGetQueryObjectuivEXT_t) gl_proc("glGetQueryObjectuivEXT");
    p_glGetQueryObjectui64vEXT = (glGetQueryObjectui64vEXT_t) gl_proc("glGetQueryObjectui64vEXT");
    if (!p_glGetString || !p_glGetIntegerv || !p_glGenQueriesEXT || !p_glGetQueryivEXT
        || !p_glQueryCounterEXT || !p_glGetQueryObjectuivEXT || !p_glGetQueryObjectui64vEXT) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "timer query entry points unavailable");
        return false;
    }
    const char* ext = (const char*) p_glGetString(CRYO_GL_EXTENSIONS);
    if (ext == NULL || strstr(ext, "GL_EXT_disjoint_timer_query") == NULL) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "GL_EXT_disjoint_timer_query not exposed");
        return false;
    }
    int bits = 0;
    p_glGetQueryivEXT(CRYO_GL_TIMESTAMP_EXT, CRYO_GL_QUERY_COUNTER_BITS_EXT, &bits);
    if (bits <= 0) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "timestamp counter has no bits");
        return false;
    }
    p_glGenQueriesEXT(CRYO_SLOTS, g_start);
    p_glGenQueriesEXT(CRYO_SLOTS, g_end);
    if (g_start[0] == 0 || g_end[0] == 0) return false;
    int disjoint = 0;
    p_glGetIntegerv(CRYO_GL_GPU_DISJOINT_EXT, &disjoint);
    __android_log_print(ANDROID_LOG_INFO, TAG, "GPU timer ready (%d-bit timestamps)", bits);
    g_ready = true;
    return true;
}

static bool ensure_ready(void) {
    if (!g_enabled || eglGetCurrentContext_p == NULL) return false;
    EGLContext ctx = eglGetCurrentContext_p();
    if (ctx == EGL_NO_CONTEXT) return false;
    if (ctx != g_context) {
        g_supported = setup(ctx) ? 1 : 0;
        if (!g_supported) g_percent = -1;
    }
    return g_ready;
}

static void collect(void) {
    for (int i = 0; i < CRYO_SLOTS; i++) {
        if (!g_pending[i]) continue;
        unsigned int available = 0;
        p_glGetQueryObjectuivEXT(g_end[i], CRYO_GL_QUERY_RESULT_AVAILABLE_EXT, &available);
        if (!available) continue;
        uint64_t t0 = 0, t1 = 0;
        p_glGetQueryObjectui64vEXT(g_start[i], CRYO_GL_QUERY_RESULT_EXT, &t0);
        p_glGetQueryObjectui64vEXT(g_end[i], CRYO_GL_QUERY_RESULT_EXT, &t1);
        g_pending[i] = false;
        int disjoint = 0;
        p_glGetIntegerv(CRYO_GL_GPU_DISJOINT_EXT, &disjoint);
        if (!disjoint && t1 > t0 && t1 - t0 < (uint64_t) CRYO_WINDOW_NS) g_busy_ns += (int64_t) (t1 - t0);
    }
    int64_t now = now_ns();
    if (g_reset_window) {
        g_reset_window = 0;
        g_window_start = 0;
    }
    if (g_window_start == 0) {
        g_window_start = now;
        g_busy_ns = 0;
        return;
    }
    int64_t elapsed = now - g_window_start;
    if (elapsed >= CRYO_WINDOW_NS) {
        int64_t pct = g_busy_ns * 100 / elapsed;
        if (pct < 0) pct = 0;
        if (pct > 100) pct = 100;
        g_percent = (int) pct;
        g_busy_ns = 0;
        g_window_start = now;
    }
}

void cryo_gpu_timer_frame_end(void) {
    if (!ensure_ready()) return;
    if (g_open) {
        p_glQueryCounterEXT(g_end[g_slot], CRYO_GL_TIMESTAMP_EXT);
        g_pending[g_slot] = true;
        g_open = false;
        g_slot = (g_slot + 1) % CRYO_SLOTS;
    }
    collect();
}

void cryo_gpu_timer_frame_begin(void) {
    if (!ensure_ready()) return;
    if (g_pending[g_slot]) return;
    p_glQueryCounterEXT(g_start[g_slot], CRYO_GL_TIMESTAMP_EXT);
    g_open = true;
}

JNIEXPORT void JNICALL
Java_org_lwjgl_glfw_CallbackBridge_setGpuTimerEnabled(JNIEnv* env, jclass clazz, jboolean enabled) {
    if (enabled && !g_enabled) g_reset_window = 1;
    g_enabled = enabled ? 1 : 0;
    g_percent = -1;
}

JNIEXPORT jint JNICALL
Java_org_lwjgl_glfw_CallbackBridge_getGpuBusyPercent(JNIEnv* env, jclass clazz) {
    return g_supported == 0 ? -2 : g_percent;
}
