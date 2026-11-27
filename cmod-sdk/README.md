# CryoMod SDK — Launcher Mods (.cmod)

Tools to make Launcher Mods (.cmod) for Cryogenic Launcher.
The full guide is [`CryoMod-Tutorial.txt`](../CryoMod-Tutorial.txt), in the root of the repository.

- `build.sh` — builds a mod folder into a `.cmod` (uses the API in
  `CryogenicLauncher/src/main/java/com/russoviller/cryogenic/cmod/api`).
- `example/` — a complete example mod (one feature, launcher-style UI).
  Its ready-made `.cmod` is [`CryoMod-Example.cmod`](../CryoMod-Example.cmod).

```bash
cd cmod-sdk
ANDROID_HOME=/path/to/android-sdk ./build.sh example
# OK: out/russoviller.example.cmod
```

Requires JDK 17+ and the Android SDK (platform + build-tools).

> ⚠ A .cmod runs code inside the launcher. Only install mods from people you trust.
