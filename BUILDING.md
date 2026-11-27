# Cryogenic Launcher — código-fonte e compilação

Launcher de Minecraft: Java Edition para Android. Autor principal: RussoViller.

## Autoria, licença e origem

O **Cryogenic Launcher foi criado por RussoViller**, com nome, identidade visual, telas e funções próprias. Repositório oficial: https://github.com/RussoViller/Cryogenic-Launcher

O Launcher usa o código do [Zalith Launcher 2](https://github.com/ZalithLauncher/ZalithLauncher2) (Copyright © 2025-2026 MovTery e colaboradores, [GNU GPL v3](LICENSE)) como base, além de código e ideias de outros projetos citados nos créditos. Por isso, conforme os termos do Zalith Launcher 2, é identificado como uma versão modificada não oficial dele. Não é afiliado aos desenvolvedores do Zalith Launcher 2 nem endossado por eles.

Termos que valem para qualquer cópia (GPL v3, seção 7; detalhes em `NOTICE` e na licença RVCLapk1.1, em `CryogenicLauncher/src/main/assets/legal/RVCL.md`):

1. Toda cópia, modificada ou não, deve manter visível no app: **"Cryogenic Launcher — created by RussoViller"**, o link do repositório oficial e os créditos do criador.
2. Ninguém pode se dizer criador ou dono do Cryogenic Launcher nem apresentar uma cópia como a oficial. Versões modificadas devem informar: **"Based on Cryogenic Launcher by RussoViller."**
3. Republicar com outro nome apagando esses créditos é proibido.
4. Os termos do Zalith Launcher 2 também continuam valendo: nome diferente do original, aviso de versão modificada não oficial e manutenção dos avisos de copyright.

## Compilação

Requer:
- JDK 21
- Android SDK (platform 37.2, build-tools 37) e NDK 25.2.9519653
- Rust com `cargo-ndk` e os alvos `aarch64-linux-android` e `armv7-linux-androideabi` (módulo PetLib)

```bash
# APK arm64-v8a
./gradlew :CryogenicLauncher:assembleRelease -Darch=arm64
# APK armeabi-v7a
./gradlew :CryogenicLauncher:assembleRelease -Darch=arm
# versão "IS" (arquivos do jogo no armazenamento interno)
./gradlew :CryogenicLauncher:assembleRelease -Darch=arm64 -Dstorage=is
```

- Sem o keystore oficial (`CryogenicLauncher/cryogenic_launcher.jks`, que não está no repositório), o APK sai sem a assinatura oficial: assine com a sua própria chave.
- Login com conta Microsoft: defina a variável de ambiente `OAUTH_CLIENT_ID` (ou o arquivo `.oauth_client_id.txt` na raiz).

## Mods do Launcher (.cmod)

Para criar mods para o próprio launcher, veja [`CryoMod-Tutorial.txt`](CryoMod-Tutorial.txt), a pasta [`cmod-sdk`](cmod-sdk) e o exemplo pronto [`CryoMod-Example.cmod`](CryoMod-Example.cmod).
