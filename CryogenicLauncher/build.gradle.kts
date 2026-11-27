import com.android.build.api.variant.impl.VariantOutputImpl
import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    id("com.google.devtools.ksp")
    id("kotlinx-serialization")
    id("kotlin-parcelize")
    id("com.movtery.buildkeys")
}

val cryogenicPackageName = "com.russoviller.cryogenic"
val cryogenicApplicationId = "com.russoviller.cryogenic"
val launcherAPPName = project.findProperty("launcher_app_name") as? String ?: error("The \"launcher_app_name\" property is not set in gradle.properties.")
val launcherName = project.findProperty("launcher_name") as? String ?: error("The \"launcher_name\" property is not set in gradle.properties.")
val launcherShortName = project.findProperty("launcher_short_name") as? String ?: error("The \"launcher_short_name\" property is not set in gradle.properties.")
val launcherUrl = project.findProperty("url_home") as? String ?: error("The \"url_home\" property is not set in gradle.properties.")

val launcherVersionCode = (project.findProperty("launcher_version_code") as? String)?.toIntOrNull() ?: error("The \"launcher_version_code\" property is not set as an integer in gradle.properties.")
val launcherVersionName = project.findProperty("launcher_version_name") as? String ?: error("The \"launcher_version_name\" property is not set in gradle.properties.")

val defaultOAuthClientID = project.findProperty("oauth_client_id") as? String
val defaultStorePassword = System.getenv("STORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: project.findProperty("default_store_password") as? String ?: error("The \"default_store_password\" property is not set in gradle.properties.")
val defaultKeyPassword = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: project.findProperty("default_key_password") as? String ?: error("The \"default_key_password\" property is not set in gradle.properties.")
val cryogenicKeystore = file("cryogenic_launcher.jks")
val hasCryogenicKeystore = cryogenicKeystore.isFile

val projectArch: String = System.getProperty("arch", "arm64")
//-Dstorage=is: versão "IS" (arquivos do jogo no armazenamento interno, pasta CryogenicLauncher)
val storageIS: Boolean = System.getProperty("storage", "") == "is"
val projectAbi: String = when (projectArch) {
    "arm" -> "armeabi-v7a"
    "arm64" -> "arm64-v8a"
    else -> error("Unsupported project architecture: $projectArch (use -Darch=arm64 or -Darch=arm)")
}

fun getKeyFromLocal(envKey: String, fileName: String? = null, default: String? = null): String {
    val key = System.getenv(envKey)
    return key ?: fileName?.let {
        val file = File(rootDir, fileName)
        if (file.canRead() && file.isFile) file.readText() else null
    } ?: default ?: run {
        logger.warn("BUILD: $envKey not set; related features may throw exceptions.")
        ""
    }
}

android {
    namespace = cryogenicPackageName
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    signingConfigs {
        create("cryogenic") {
            storeFile = cryogenicKeystore
            storePassword = defaultStorePassword
            keyAlias = "cryogenic"
            keyPassword = defaultKeyPassword
        }
    }

    defaultConfig {
        applicationId = cryogenicApplicationId
        ndk {
            abiFilters += projectAbi
        }
        minSdk = 26
        targetSdk = 34
        versionCode = launcherVersionCode
        versionName = launcherVersionName
        manifestPlaceholders["launcher_name"] = if (storageIS) "$launcherAPPName IS" else launcherAPPName
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            //IS instala ao lado da versão normal (pacote próprio)
            if (storageIS) applicationIdSuffix = ".is"
            if (hasCryogenicKeystore) signingConfig = signingConfigs.getByName("cryogenic")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            if (hasCryogenicKeystore) signingConfig = signingConfigs.getByName("cryogenic")
        }
    }

    androidResources {
        //.tar.xz já é compactado: guardado sem compressão no APK, o Android lê os bytes direto
        //(sem descompactar o deflate do APK em tempo real ao extrair o Java)
        noCompress.add("xz")
        localeFilters += listOf("en", "pt", "pt-rBR", "es", "ru", "ja", "vi", "zh-rCN", "zh-rTW", "tr", "in", "th", "de", "fr", "it", "nl", "pl", "sv", "nb", "fi", "is", "ko", "ar", "hi")
    }

    lint {
        checkReleaseBuilds = false
    }

    ndkVersion = "25.2.9519653"

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf("**/libbytehook.so")
        }
    }

    compileOptions {
        // sora-editor language-textmate
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
        prefab = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            //让 android.util.Log 等框架方法在本地单测中返回默认值而非抛出异常
            isReturnDefaultValues = true
            all {
                //Repassa a pasta de saída da prévia (só usada quando informada)
                System.getProperty("cryo.previewDir")?.let { dir -> it.systemProperty("cryo.previewDir", dir) }
                System.getProperty("cryo.jreDir")?.let { dir -> it.systemProperty("cryo.jreDir", dir) }
                //biblioteca do ChunkBase compilada para o PC (testes)
                System.getProperty("cryoseed.path")?.let { lib -> it.systemProperty("cryoseed.path", lib) }
                System.getProperty("robolectric.dependency.repo.url")?.let { url -> it.systemProperty("robolectric.dependency.repo.url", url) }
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is VariantOutputImpl) {
                val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }
                afterEvaluate {
                    val task = tasks.named("merge${variantName}Assets").get() as MergeSourceSetFolders
                    task.inputs.property("lwjglArch", projectArch)
                    task.doLast {
                        val assetsDir = task.outputDir.get().asFile
                        val tag = "JREAssetsCleanup"
                        logger.lifecycle("[$tag] arch: $projectArch")
                        val jreList = listOf("jre-8", "jre-17", "jre-21", "jre-25")
                        jreList.forEach { jreVersion ->
                            val runtimeDir = File("$assetsDir/runtimes/$jreVersion")
                            logger.lifecycle("[$tag] runtimeDir: ${runtimeDir.absolutePath}")
                            runtimeDir.listFiles()?.forEach {
                                if (projectArch != "all" && it.name != "version" && !it.name.contains("universal") && it.name != "bin-$projectArch.tar.xz") {
                                    logger.lifecycle("[$tag] delete: $it : ${it.delete()}")
                                }
                            }
                        }

                        if (projectArch == "all") return@doLast
                        val abi = when (projectArch) {
                            "arm" -> "armeabi-v7a"
                            "arm64" -> "arm64-v8a"
                            "x86" -> "x86"
                            "x86_64" -> "x86_64"
                            else -> return@doLast
                        }
                        val lwjglVersions = file("libs").listFiles { f ->
                            f.name.matches(Regex("lwjgl-\\d+\\.\\d+\\.\\d+-natives-release\\.aar"))
                        }
                            ?.map { Regex("lwjgl-(\\d+\\.\\d+\\.\\d+)-natives-release\\.aar").find(it.name)!!.groupValues[1] }
                            ?: emptyList()
                        lwjglVersions.forEach { version ->
                            val nativesDir = File(assetsDir, "app_runtime/lwjgl/$version/natives")
                            if (nativesDir.isDirectory) {
                                nativesDir.listFiles()?.forEach { dir ->
                                    if (dir.isDirectory && dir.name != abi) {
                                        logger.lifecycle("Removing non-target-arch natives: $dir")
                                        dir.deleteRecursively()
                                    }
                                }
                            }
                        }
                        File(assetsDir, "runtimes/jna").listFiles()?.forEach { versionDir ->
                            val nativesDir = File(versionDir, "natives")
                            if (nativesDir.isDirectory) {
                                nativesDir.listFiles()?.forEach { dir ->
                                    if (dir.isDirectory && dir.name != abi) {
                                        logger.lifecycle("Removing non-target-arch JNA natives: $dir")
                                        dir.deleteRecursively()
                                    }
                                }
                            }
                        }
                    }
                }

                val versionTag = launcherVersionName.replace(" ", "-")
                val baseName = "$launcherName-${if (variant.buildType == "release") versionTag else "Debug-$versionTag"}"
                output.outputFileName = "$baseName-$projectAbi${if (storageIS) "-IS" else ""}.apk"
            }
        }
    }
}


kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
        )
    }
}

buildKeys {
    string("OAUTH_CLIENT_ID", getKeyFromLocal("OAUTH_CLIENT_ID", ".oauth_client_id.txt", defaultOAuthClientID), true)
    string("LAUNCHER_NAME", launcherAPPName, true)
    string("LAUNCHER_IDENTIFIER", launcherName, true)
    string("LAUNCHER_SHORT_NAME", launcherShortName, true)
    string("URL_HOME", launcherUrl, true)
    string("CURSEFORGE_API", "", true)
    string("BUILD_ARCH", projectArch)
    string("STORAGE_MODE", if (storageIS) "is" else "")
}

val gameJarBytecodeLimits = mapOf(
    "app_runtime/lwjgl/3.3.3" to 52,
    "app_runtime/lwjgl/3.4.1" to 61,
    "components/auth_libs" to 52,
    "components/caciocavallo" to 52,
    "components/caciocavallo17" to 61,
    "components/launcher" to 52
)

val verifyGameJarBytecode by tasks.registering {
    group = "verification"
    val assetsRoot = file("src/main/assets")
    val limits = gameJarBytecodeLimits
    inputs.files(limits.keys.map { File(assetsRoot, it) })
    doLast {
        val offenders = mutableListOf<String>()
        limits.forEach { (dir, maxMajor) ->
            File(assetsRoot, dir).listFiles { f -> f.isFile && f.name.endsWith(".jar") }?.sorted()?.forEach { jar ->
                ZipFile(jar).use { zip ->
                    zip.entries().asSequence()
                        .filter { it.name.endsWith(".class") && !it.name.startsWith("META-INF/versions/") }
                        .forEach { entry ->
                            val header = zip.getInputStream(entry).use { it.readNBytes(8) }
                            if (header.size == 8) {
                                val major = ((header[6].toInt() and 0xFF) shl 8) or (header[7].toInt() and 0xFF)
                                if (major > maxMajor) offenders += "$dir/${jar.name}!/${entry.name}: class file $major > $maxMajor"
                            }
                        }
                }
            }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Game-side jars contain bytecode too new for their minimum Java runtime:\n" +
                    offenders.take(50).joinToString("\n")
            )
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(verifyGameJarBytecode)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.nav3)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.webkit)
    implementation(libs.documentfile)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.svg)
    implementation(libs.coil.network.ktor3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material)
    implementation(libs.material.color.utilities)
    implementation(libs.materialKolor)
    implementation(libs.reorderable)
    implementation(libs.richtext.commonmark)
    implementation(libs.richtext.ui)
    implementation(libs.richtext.ui.material3)
    implementation(platform(libs.editor.bom))
    implementation(libs.editor)
    implementation(libs.editor.language.textmate)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    //Project
    implementation(project(":LayerController"))
    implementation(project(":ColorPicker"))
    implementation(project(":CardGrid"))
    implementation(project(":InputMap"))
    implementation(project(":Guide"))
    implementation(project(":PetLib"))
    //Utils
    implementation(libs.bytehook)
    implementation(libs.gson)
    implementation(libs.jetbrains.annotations)
    implementation(libs.commons.io)
    implementation(libs.commons.codec)
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.zip4j)
    implementation(libs.okio)
    implementation(libs.okhttp)
    //Live (Painel da Live -> TikTok): captura de tela + RTMP. Apache-2.0
    implementation("com.github.pedroSG94.RootEncoder:library:2.6.6")
    //mesma versão do ktor do resto do app (o RootEncoder pede 3.3.x)
    implementation("io.ktor:ktor-network:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-network-tls:${libs.versions.ktor.get()}")
    implementation(libs.ktor.http)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.minidns.hla)
    implementation(libs.toml4j)
    implementation(libs.maven.artifact)
    implementation(libs.mmkv)
    implementation(libs.fishnet)
    implementation(libs.process.phoenix)
    implementation(libs.lunarcalendar)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    //Safe
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.sqlcipher.android)
    ksp(libs.androidx.room.compiler)
    //Support
    implementation(libs.proxy.client.android)
    //Hilt
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    //Test
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver3)
    //Prévia da tela de boas-vindas renderizada no computador (WelcomePreviewRender)
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
