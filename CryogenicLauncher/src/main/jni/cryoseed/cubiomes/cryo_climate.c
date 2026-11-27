/*
 * Cryogenic Launcher: escolha do bioma do Overworld da 1.21.5 em diante.
 *
 * Comparando com mundos gerados pelo jogo (1.21.11, 26.1 e 26.3), o bioma é o ponto da lista
 * mais perto do clima (busca exata), não o resultado da árvore aproximada que a 1.21.4 usava.
 * Aqui a lista é guardada numa árvore k-d com caixas e a busca poda pelo limite inferior da
 * distância, então o resultado é o mesmo de olhar a lista inteira (empate: menor índice).
 */
#include <stdlib.h>
#include <string.h>
#include <pthread.h>

#include "biomes.h"
#include "tables/climate_cryo.h"

#define LEAF_SIZE 8

typedef struct {
    int32_t lo[6], hi[6];   /* caixa que cobre tudo embaixo */
    int32_t left, right;    /* filhos (-1 = folha) */
    int32_t start, count;   /* folha: faixa em g_order */
} KdNode;

static KdNode *g_nodes;
static int32_t g_order[cryo_climate_len];
static int g_node_count;
static pthread_once_t g_once = PTHREAD_ONCE_INIT;

static int g_sort_dim;
static int cmp_center(const void *a, const void *b) {
    int ia = *(const int32_t *) a, ib = *(const int32_t *) b;
    int d = g_sort_dim;
    int32_t ca = cryo_climate_param[ia][2 * d] + cryo_climate_param[ia][2 * d + 1];
    int32_t cb = cryo_climate_param[ib][2 * d] + cryo_climate_param[ib][2 * d + 1];
    if (ca != cb) return ca < cb ? -1 : 1;
    return ia - ib;
}

static int build(int start, int count) {
    int id = g_node_count++;
    KdNode *n = &g_nodes[id];
    int i, d;
    for (d = 0; d < 6; d++) { n->lo[d] = 1 << 30; n->hi[d] = -(1 << 30); }
    for (i = start; i < start + count; i++) {
        const int16_t *p = cryo_climate_param[g_order[i]];
        for (d = 0; d < 6; d++) {
            if (p[2 * d] < n->lo[d]) n->lo[d] = p[2 * d];
            if (p[2 * d + 1] > n->hi[d]) n->hi[d] = p[2 * d + 1];
        }
    }
    n->start = start;
    n->count = count;
    n->left = n->right = -1;
    if (count <= LEAF_SIZE) return id;
    /* divide na dimensão de maior espalhamento dos centros */
    int best = 0;
    int64_t bestSpread = -1;
    for (d = 0; d < 6; d++) {
        int32_t mn = 1 << 30, mx = -(1 << 30);
        for (i = start; i < start + count; i++) {
            const int16_t *p = cryo_climate_param[g_order[i]];
            int32_t c = p[2 * d] + p[2 * d + 1];
            if (c < mn) mn = c;
            if (c > mx) mx = c;
        }
        if ((int64_t) mx - mn > bestSpread) { bestSpread = (int64_t) mx - mn; best = d; }
    }
    g_sort_dim = best;
    qsort(g_order + start, count, sizeof(int32_t), cmp_center);
    int half = count / 2;
    int l = build(start, half);
    int r = build(start + half, count - half);
    g_nodes[id].left = l;
    g_nodes[id].right = r;
    return id;
}

static void init_tree(void) {
    int i;
    for (i = 0; i < cryo_climate_len; i++) g_order[i] = i;
    g_nodes = (KdNode *) calloc(2 * cryo_climate_len, sizeof(KdNode));
    g_node_count = 0;
    if (g_nodes) build(0, cryo_climate_len);
}

static inline int64_t axis_dist(int64_t v, int64_t lo, int64_t hi) {
    int64_t d = v > hi ? v - hi : (v < lo ? lo - v : 0);
    return d * d;
}

static inline int64_t entry_dist(const int64_t np[6], int idx) {
    const int16_t *p = cryo_climate_param[idx];
    int64_t s = 0;
    int d;
    for (d = 0; d < 6; d++) s += axis_dist(np[d], p[2 * d], p[2 * d + 1]);
    return s;
}

static inline int64_t box_dist(const int64_t np[6], const KdNode *n) {
    int64_t s = 0;
    int d;
    for (d = 0; d < 6; d++) s += axis_dist(np[d], n->lo[d], n->hi[d]);
    return s;
}

static void search(const int64_t np[6], const int16_t *biome, int node, int64_t *best, int *bestIdx) {
    const KdNode *n = &g_nodes[node];
    if (n->left < 0) {
        int i;
        for (i = n->start; i < n->start + n->count; i++) {
            int idx = g_order[i];
            if (biome[idx] < 0) continue;
            int64_t d = entry_dist(np, idx);
            if (d < *best || (d == *best && idx < *bestIdx)) { *best = d; *bestIdx = idx; }
        }
        return;
    }
    int64_t dl = box_dist(np, &g_nodes[n->left]);
    int64_t dr = box_dist(np, &g_nodes[n->right]);
    int first = dl <= dr ? n->left : n->right;
    int second = dl <= dr ? n->right : n->left;
    int64_t d1 = dl <= dr ? dl : dr;
    int64_t d2 = dl <= dr ? dr : dl;
    if (d1 <= *best) search(np, biome, first, best, bestIdx);
    if (d2 <= *best) search(np, biome, second, best, bestIdx);
}

int cryoClimateToBiome(int mc, const uint64_t np_u[6], uint64_t *dat) {
    pthread_once(&g_once, init_tree);
    const int16_t *biome = mc >= MC_26_3 ? cryo_climate_biome_26_3
        : mc >= MC_26_2 ? cryo_climate_biome_26_2 : cryo_climate_biome_1_21_5;
    const int64_t *np = (const int64_t *) np_u;
    int bestIdx = -1;
    int64_t best = INT64_MAX;
    /* último resultado como limite inicial (vizinhos quase sempre caem no mesmo ponto) */
    if (dat && *dat < (uint64_t) cryo_climate_len && biome[*dat] >= 0) {
        bestIdx = (int) *dat;
        best = entry_dist(np, bestIdx);
    }
    if (!g_nodes) {
        int i;
        for (i = 0; i < cryo_climate_len; i++) {
            if (biome[i] < 0) continue;
            int64_t d = entry_dist(np, i);
            if (d < best || (d == best && i < bestIdx)) { best = d; bestIdx = i; }
        }
    } else {
        search(np, biome, 0, &best, &bestIdx);
    }
    if (bestIdx < 0) return none;
    if (dat) *dat = (uint64_t) bestIdx;
    return biome[bestIdx];
}
