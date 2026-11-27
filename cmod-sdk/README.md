# CryoMod SDK — Launcher Mods (.cmod)

Tools to make Launcher Mods (.cmod) for Cryogenic Launcher.
The full guide is [`CryoMod-Tutorial.txt`](../CryoMod-Tutorial.txt), in the root of the repository.

- `build.sh` — builds a mod folder into a `.cmod` (uses the API in
  `CryogenicLauncher/src/main/java/com/russoviller/cryogenic/cmod/api`).
- `example/` — a complete example mod (one feature, launcher-style UI).
  Its ready-made `.cmod` is [`CryoMod-Example.cmod`](../CryoMod-Example.cmod).
- `example-hud/` — API 2 showcase (CryoHUD): settings in the launcher, and in game
  an FPS/clock HUD, a quick command button, auto-walk and chat mention alerts.
  Its ready-made `.cmod` is [`CryoMod-CryoHUD.cmod`](../CryoMod-CryoHUD.cmod).

```bash
cd cmod-sdk
ANDROID_HOME=/path/to/android-sdk ./build.sh example
# OK: out/russoviller.example.cmod
```

Optional folders next to `src/`: `assets/`, `game/` (files for the game side, e.g. a
`-javaagent` jar) and `libs/` (.jar libraries compiled into the mod).

Requires JDK 17+ and the Android SDK (platform + build-tools).

> ⚠ A .cmod runs code inside the launcher (and, with API 2, inside the game). Only install mods from people you trust.
