pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content { excludeGroup("com.github.pedroSG94.RootEncoder") }
        }
        mavenCentral {
            content { excludeGroup("com.github.pedroSG94.RootEncoder") }
        }
        //RootEncoder (RTMP da live): só esse grupo vem do JitPack
        maven("https://jitpack.io") {
            content { includeGroup("com.github.pedroSG94.RootEncoder") }
        }
    }
}

rootProject.name = "CryogenicLauncher"
include(":CryogenicLauncher")
include(":LWJGL")
include(":LWJGL:patches")
project(":LWJGL:patches").projectDir = file("LWJGL/patches")
include(":LWJGL:lwjgl-3.3.3")
project(":LWJGL:lwjgl-3.3.3").projectDir = file("LWJGL/3.3.3")
include(":LWJGL:lwjgl-3.4.1")
project(":LWJGL:lwjgl-3.4.1").projectDir = file("LWJGL/3.4.1")
include(":LayerController")
include(":ColorPicker")
include(":CardGrid")
include(":InputMap")
include(":Guide")
include(":PetLib")
