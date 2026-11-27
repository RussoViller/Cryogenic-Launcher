# CryoMod SDK — Mods do Launcher (.cmod)

Ferramentas para criar Mods do Launcher (.cmod) para o Cryogenic Launcher.
O guia completo está em [`CryoMod-Tutorial.txt`](../CryoMod-Tutorial.txt), na raiz do repositório.

- `build.sh` — compila uma pasta de mod em `.cmod` (usa a API em
  `CryogenicLauncher/src/main/java/com/russoviller/cryogenic/cmod/api`).
- `example/` — mod de exemplo completo (uma função, interface no estilo do launcher).
  O `.cmod` pronto dele é o [`CryoMod-Example.cmod`](../CryoMod-Example.cmod).

```bash
cd cmod-sdk
ANDROID_HOME=/caminho/do/android-sdk ./build.sh example
# OK: out/russoviller.example.cmod
```

Precisa de JDK 17+ e do Android SDK (platform + build-tools).

> ⚠ Um .cmod roda código dentro do launcher. Só instale mods de quem você confia.
