import java.security.MessageDigest
import java.util.zip.ZipFile

plugins {
    java
}

fun writeVersion(file: File, inputs: List<File>) {
    val digest = MessageDigest.getInstance("SHA-1")
    inputs.forEach { input ->
        input.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
    }
    file.writeText(digest.digest().joinToString("") { "%02x".format(it) })
}

val lwjglVersion = "3.4.1"

fun assertMaxClassVersion(jars: List<File>, maxMajor: Int) {
    val offenders = mutableListOf<String>()
    jars.forEach { jar ->
        ZipFile(jar).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.endsWith(".class") && !it.name.startsWith("META-INF/versions/") }
                .forEach { entry ->
                    val header = zip.getInputStream(entry).use { it.readNBytes(8) }
                    if (header.size == 8) {
                        val major = ((header[6].toInt() and 0xFF) shl 8) or (header[7].toInt() and 0xFF)
                        if (major > maxMajor) offenders += "${jar.name}!/${entry.name} (class file $major)"
                    }
                }
        }
    }
    if (offenders.isNotEmpty()) {
        throw GradleException("LWJGL $lwjglVersion must not contain classes newer than class file $maxMajor:\n" + offenders.joinToString("\n"))
    }
}
group = "org.lwjgl.glfw"

configurations {
    create("lwjglModules") {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "../compileOnly", "include" to listOf("*.jar"))))
    implementation(fileTree(mapOf("dir" to "libs/$lwjglVersion", "include" to listOf("*.jar"))))
    add(
        "lwjglModules",
        fileTree(mapOf("dir" to "libs/$lwjglVersion", "include" to listOf("*.jar")))
    )
    add("lwjglModules", project(":LWJGL:patches"))
    implementation(project(":LWJGL:patches"))
    implementation(libs.jspecify) // lwjgl3.3.3 has jsr305 included as a jar

}

tasks.jar {
    // 被排除的模块只经 doLast 复制、不参与合并，需显式声明为输入，
    // 否则更新这些 jar 后任务会误判 UP-TO-DATE，导致 copy 与 version 不更新
    inputs.files(configurations["lwjglModules"])

    // Modules to copy over to the components directory instead of patching and merging
    val excludedModules = arrayOf(
        "lwjgl-lwjglx.jar",
        "lwjgl.jar",
        "jsr305.jar",
        "lwjgl-freetype.jar",
        "lwjgl-jemalloc.jar",
        "lwjgl-nanovg.jar",
        // lwjgl-openal.jar: mesclado (ALC11 modificado em src/ manda o microfone do Voice Chat para a live do launcher)
        "lwjgl-stb.jar",
        // lwjgl-tinyfd.jar: mesclado (TinyFileDialogs modificado em src/ abre o seletor de arquivos do Android);
        // como jar separado o pacote org.lwjgl.util.tinyfd ficaria dividido entre dois jars (quebra layers de módulos do Forge)
        "lwjgl-shaderc.jar",
        "lwjgl-spvc.jar",
        "lwjgl-vma.jar",
        "lwjgl-vulkan.jar",
        "lwjgl-spng.jar"
    )

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("lwjgl-${lwjglVersion}-merged-modules")
    destinationDirectory.set(file("$rootDir/CryogenicLauncher/src/main/assets/app_runtime/lwjgl/${lwjglVersion}"))

    from({
        // Ensure that the core lwjgl jar is processed first so duplicates in META-INF from other classes
        // are ignored. This avoids InvalidModuleDescriptorException due to say, using the module-info.class
        // from lwjgl-jemalloc.
        val includedModules = configurations["lwjglModules"].filter { dep ->
            !excludedModules.any { it == dep.name }
        }
        val coreJar = includedModules.find { it.name == "lwjgl.jar" }
        val jarList =
            if (coreJar != null) listOf(coreJar) + (includedModules - coreJar) else includedModules
        println("Merging LWJGL $lwjglVersion modules in the order: ")
        jarList.map {
            println(it.name)
            if (it.isDirectory) it else zipTree(it)
        }
    })

    // Makes the jar reproducible so the version file actually is a version file
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    val versionFile = File(destinationDirectory.get().asFile, "version")
    doLast {
        val excludedModulesFileList = excludedModules.flatMap { fileName ->
            configurations["lwjglModules"].filter { it.name == fileName }
        }
        copy {
            // Copy excluded modules to the lwjgl classes dir
            from(excludedModulesFileList)
            into(archiveFile.get().asFile.parentFile)
        }
        assertMaxClassVersion(listOf(archiveFile.get().asFile) + excludedModulesFileList, 61)
        writeVersion(versionFile, listOf(archiveFile.get().asFile) + excludedModulesFileList)
    }
    // Adds the jank to outputs
    outputs.file(versionFile)
    outputs.files(excludedModules.map { path -> File(destinationDirectory.get().asFile, path) })
    exclude("net/java/openjdk/cacio/ctc/**")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}