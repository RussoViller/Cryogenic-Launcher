//
// Cryogenic Launcher: "Núcleos de alto desempenho".
//
// CRYO_AFFINITY_GAME   = núcleos para todas as threads do jogo (ex.: "2,3,4,5,6,7")
// CRYO_AFFINITY_RENDER = núcleos exclusivos da thread de renderização (opcional, modo Máximo)
//
// A afinidade é aplicada na thread que cria a JVM, então toda thread da JVM herda.
// No modo Máximo um monitor reaplica a cada 2 s: thread de renderização -> RENDER,
// demais threads criadas depois do launch -> GAME sem os núcleos de RENDER. Isso evita o problema do
// "big core" antigo (fixar a thread de renderização num núcleo fazia toda thread criada por ela herdar
// aquele único núcleo). Threads do Android que já existiam antes do launch não são tocadas.
//
#define _GNU_SOURCE
#include <sched.h>
#include <pthread.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <sys/syscall.h>

#define CRYO_MAX_PREEXISTING 1024

static cpu_set_t g_game_set, g_render_set, g_worker_set;
static int g_game_count = 0, g_render_count = 0;
static volatile pid_t g_render_tid = 0;
static pid_t g_preexisting[CRYO_MAX_PREEXISTING];
static int g_preexisting_count = 0;
static pid_t g_monitor_tid = 0;

static int parse_cores(const char* list, cpu_set_t* set) {
    CPU_ZERO(set);
    if (list == NULL || *list == 0) return 0;
    int count = 0;
    const char* p = list;
    while (*p) {
        char* end;
        long core = strtol(p, &end, 10);
        if (end == p) break;
        if (core >= 0 && core < CPU_SETSIZE && !CPU_ISSET((int) core, set)) {
            CPU_SET((int) core, set);
            count++;
        }
        p = (*end == ',') ? end + 1 : end;
    }
    return count;
}

static int is_preexisting(pid_t tid) {
    for (int i = 0; i < g_preexisting_count; i++) if (g_preexisting[i] == tid) return 1;
    return 0;
}

static void snapshot_threads(void) {
    DIR* dir = opendir("/proc/self/task");
    if (dir == NULL) return;
    struct dirent* e;
    while ((e = readdir(dir)) != NULL && g_preexisting_count < CRYO_MAX_PREEXISTING) {
        if (e->d_name[0] < '0' || e->d_name[0] > '9') continue;
        g_preexisting[g_preexisting_count++] = (pid_t) atoi(e->d_name);
    }
    closedir(dir);
}

static void apply_monitor_pass(void) {
    DIR* dir = opendir("/proc/self/task");
    if (dir == NULL) return;
    pid_t render = g_render_tid;
    struct dirent* e;
    while ((e = readdir(dir)) != NULL) {
        if (e->d_name[0] < '0' || e->d_name[0] > '9') continue;
        pid_t tid = (pid_t) atoi(e->d_name);
        if (tid == g_monitor_tid || is_preexisting(tid)) continue;
        cpu_set_t current;
        if (sched_getaffinity(tid, sizeof(current), &current) != 0) continue;
        const cpu_set_t* want = (tid == render) ? &g_render_set : &g_worker_set;
        if (!CPU_EQUAL(&current, want)) sched_setaffinity(tid, sizeof(cpu_set_t), want);
    }
    closedir(dir);
}

static void* monitor_main(void* arg) {
    (void) arg;
    g_monitor_tid = (pid_t) syscall(SYS_gettid);
    // o monitor em si roda nos núcleos de trabalho
    sched_setaffinity(0, sizeof(cpu_set_t), &g_worker_set);
    while (1) {
        if (g_render_tid != 0) apply_monitor_pass();
        sleep(2);
    }
    return NULL;
}

/** Chamado na thread que cria a JVM (VMLauncher.launchJVM), antes do JLI_Launch. */
void cryo_affinity_apply_launch(void) {
    g_game_count = parse_cores(getenv("CRYO_AFFINITY_GAME"), &g_game_set);
    if (g_game_count == 0) return;
    g_render_count = parse_cores(getenv("CRYO_AFFINITY_RENDER"), &g_render_set);

    snapshot_threads();
    if (sched_setaffinity(0, sizeof(cpu_set_t), &g_game_set) != 0) {
        printf("[CPU] Failed to set game core affinity: %s\n", strerror(errno));
        g_game_count = 0;
        return;
    }
    printf("[CPU] Game threads limited to %d high-performance core(s): %s\n", g_game_count, getenv("CRYO_AFFINITY_GAME"));

    if (g_render_count > 0) {
        CPU_ZERO(&g_worker_set);
        int workers = 0;
        for (int i = 0; i < CPU_SETSIZE; i++) {
            if (CPU_ISSET(i, &g_game_set) && !CPU_ISSET(i, &g_render_set)) { CPU_SET(i, &g_worker_set); workers++; }
        }
        if (workers == 0) {
            g_worker_set = g_game_set;
        }
        pthread_t thread;
        if (pthread_create(&thread, NULL, monitor_main, NULL) == 0) {
            pthread_detach(thread);
        } else {
            g_render_count = 0;
        }
    }
}

/** Chamado na thread de renderização do jogo (pojavInit). */
void cryo_affinity_mark_render_thread(void) {
    if (g_game_count == 0 || g_render_count == 0) return;
    g_render_tid = (pid_t) syscall(SYS_gettid);
    if (sched_setaffinity(0, sizeof(cpu_set_t), &g_render_set) == 0) {
        printf("[CPU] Render thread dedicated to core(s): %s\n", getenv("CRYO_AFFINITY_RENDER"));
    } else {
        printf("[CPU] Failed to dedicate the render thread: %s\n", strerror(errno));
    }
}
