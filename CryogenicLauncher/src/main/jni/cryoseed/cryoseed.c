/*
 * Cryogenic Launcher - ChunkBase
 * Ponte JNI para o cubiomes (biomas e estruturas a partir da seed).
 * Cada handle tem o próprio Generator: use um handle por thread.
 */
#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include "cubiomes/generator.h"
#include "cubiomes/finders.h"
#include "cubiomes/biomes.h"
#include "cubiomes/util.h"

#define JNI_FN(name) Java_com_russoviller_cryogenic_game_seedmap_SeedNative_##name

typedef struct {
    Generator g;
    SurfaceNoise sn;
    int mc;
    int dim;
    uint64_t seed;
} SeedCtx;

static int floor_div(int a, int b) {
    int q = a / b;
    if ((a % b != 0) && ((a < 0) != (b < 0))) q--;
    return q;
}

JNIEXPORT jlong JNICALL JNI_FN(create)(JNIEnv *env, jclass cls, jint mc, jlong seed, jint dim, jboolean large) {
    (void) env; (void) cls;
    SeedCtx *c = (SeedCtx *) calloc(1, sizeof(SeedCtx));
    if (!c) return 0;
    c->mc = mc;
    c->dim = dim;
    c->seed = (uint64_t) seed;
    setupGenerator(&c->g, mc, large ? LARGE_BIOMES : 0);
    applySeed(&c->g, dim, (uint64_t) seed);
    if (dim == DIM_END) initSurfaceNoise(&c->sn, DIM_END, (uint64_t) seed);
    return (jlong) (intptr_t) c;
}

JNIEXPORT void JNICALL JNI_FN(free)(JNIEnv *env, jclass cls, jlong handle) {
    (void) env; (void) cls;
    if (handle) free((void *) (intptr_t) handle);
}

/* Biomas de uma área: scale 1/4/16/64/256, (x,z) canto noroeste já na escala. y na escala 1:4. */
JNIEXPORT jintArray JNICALL JNI_FN(biomes)(JNIEnv *env, jclass cls, jlong handle,
                                           jint scale, jint x, jint z, jint w, jint h, jint y) {
    (void) cls;
    SeedCtx *c = (SeedCtx *) (intptr_t) handle;
    if (!c || w <= 0 || h <= 0) return NULL;
    Range r = { scale, x, z, w, h, y, 1 };
    int *cache = allocCache(&c->g, r);
    if (!cache) return NULL;
    if (genBiomes(&c->g, cache, r) != 0) {
        free(cache);
        return NULL;
    }
    jintArray out = (*env)->NewIntArray(env, w * h);
    if (out) (*env)->SetIntArrayRegion(env, out, 0, w * h, cache);
    free(cache);
    return out;
}

/* 1 = a estrutura existe nessa versão (e em qual dimensão: devolve dim+2, ou 0 se não existe). */
JNIEXPORT jint JNICALL JNI_FN(structureDim)(JNIEnv *env, jclass cls, jint type, jint mc) {
    (void) env; (void) cls;
    StructureConfig sc;
    if (!getStructureConfig(type, mc, &sc)) return 0;
    return sc.dim + 2;
}

/* Posições válidas (x,z em blocos) de uma estrutura dentro do retângulo de blocos dado. */
JNIEXPORT jintArray JNICALL JNI_FN(structures)(JNIEnv *env, jclass cls, jlong handle, jint type,
                                               jint x0, jint z0, jint x1, jint z1, jint max) {
    (void) cls;
    SeedCtx *c = (SeedCtx *) (intptr_t) handle;
    if (!c) return NULL;
    StructureConfig sc;
    if (!getStructureConfig(type, c->mc, &sc) || sc.dim != c->dim) return (*env)->NewIntArray(env, 0);
    int reg = sc.regionSize * 16;
    if (reg <= 0) return (*env)->NewIntArray(env, 0);
    int rx0 = floor_div(x0, reg), rx1 = floor_div(x1, reg);
    int rz0 = floor_div(z0, reg), rz1 = floor_div(z1, reg);
    if (max <= 0) max = 1;
    int *buf = (int *) malloc(sizeof(int) * 2 * max);
    if (!buf) return NULL;
    int n = 0;
    for (int rz = rz0; rz <= rz1 && n < max; rz++) {
        for (int rx = rx0; rx <= rx1 && n < max; rx++) {
            Pos p;
            if (!getStructurePos(type, c->mc, c->seed, rx, rz, &p)) continue;
            if (p.x < x0 || p.x > x1 || p.z < z0 || p.z > z1) continue;
            if (!isViableStructurePos(type, &c->g, p.x, p.z, 0)) continue;
            if (type == End_City && !isViableEndCityTerrain(&c->g, &c->sn, p.x, p.z)) continue;
            if (c->dim == DIM_OVERWORLD && c->mc >= MC_1_18 &&
                !isViableStructureTerrain(type, &c->g, p.x, p.z)) continue;
            buf[n * 2] = p.x;
            buf[n * 2 + 1] = p.z;
            n++;
        }
    }
    jintArray out = (*env)->NewIntArray(env, n * 2);
    if (out && n > 0) (*env)->SetIntArrayRegion(env, out, 0, n * 2, buf);
    free(buf);
    return out;
}

/* Fortalezas (strongholds): precisa de um handle do Overworld. */
JNIEXPORT jintArray JNICALL JNI_FN(strongholds)(JNIEnv *env, jclass cls, jlong handle, jint count) {
    (void) cls;
    SeedCtx *c = (SeedCtx *) (intptr_t) handle;
    if (!c || c->dim != DIM_OVERWORLD || count <= 0) return (*env)->NewIntArray(env, 0);
    int *buf = (int *) malloc(sizeof(int) * 2 * count);
    if (!buf) return NULL;
    StrongholdIter sh;
    initFirstStronghold(&sh, c->mc, c->seed);
    int n = 0;
    while (n < count) {
        int left = nextStronghold(&sh, &c->g);
        buf[n * 2] = sh.pos.x;
        buf[n * 2 + 1] = sh.pos.z;
        n++;
        if (left <= 0) break;
    }
    jintArray out = (*env)->NewIntArray(env, n * 2);
    if (out && n > 0) (*env)->SetIntArrayRegion(env, out, 0, n * 2, buf);
    free(buf);
    return out;
}

/* Biomas que existem na versão e dimensão (ids). */
JNIEXPORT jintArray JNICALL JNI_FN(biomeIds)(JNIEnv *env, jclass cls, jint mc, jint dim) {
    (void) cls;
    int buf[256];
    int n = 0;
    for (int id = 0; id < 256; id++) {
        if (!biomeExists(mc, id)) continue;
        if (getDimension(id) != dim) continue;
        if (dim == DIM_OVERWORLD && !isOverworld(mc, id)) continue;
        if (!biome2str(mc, id)) continue;
        buf[n++] = id;
    }
    jintArray out = (*env)->NewIntArray(env, n);
    if (out && n > 0) (*env)->SetIntArrayRegion(env, out, 0, n, buf);
    return out;
}

JNIEXPORT jstring JNICALL JNI_FN(biomeName)(JNIEnv *env, jclass cls, jint mc, jint id) {
    (void) cls;
    const char *s = biome2str(mc, id);
    return s ? (*env)->NewStringUTF(env, s) : NULL;
}

/* Cores padrão dos biomas (256 x RGB). */
JNIEXPORT jbyteArray JNICALL JNI_FN(biomeColors)(JNIEnv *env, jclass cls) {
    (void) cls;
    unsigned char colors[256][3];
    initBiomeColors(colors);
    jbyteArray out = (*env)->NewByteArray(env, 256 * 3);
    if (out) (*env)->SetByteArrayRegion(env, out, 0, 256 * 3, (const jbyte *) colors);
    return out;
}

/* Versão do cubiomes para um texto como "1.21.1" (0 = não reconhecida). */
JNIEXPORT jint JNICALL JNI_FN(versionOf)(JNIEnv *env, jclass cls, jstring name) {
    (void) cls;
    if (!name) return 0;
    const char *s = (*env)->GetStringUTFChars(env, name, NULL);
    if (!s) return 0;
    int mc = str2mc(s);
    (*env)->ReleaseStringUTFChars(env, name, s);
    return mc < 0 ? 0 : mc;
}

JNIEXPORT jint JNICALL JNI_FN(newestVersion)(JNIEnv *env, jclass cls) {
    (void) env; (void) cls;
    return MC_NEWEST;
}
