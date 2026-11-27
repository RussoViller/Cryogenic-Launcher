-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn com.github.luben.zstd.**
-dontwarn org.brotli.dec.**
-dontwarn java.lang.management.**
-dontwarn io.ktor.util.debug.**

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}

# SDL
-keep class org.libsdl.app.** { *; }

# Launcher
-keep class org.lwjgl.glfw.CallbackBridge {
    *;
}
-keep class com.oracle.dalvik.VMLauncher {
    *;
}

#
## Hilt
#-keep class dagger.hilt.** { *; }
#-keep class javax.inject.** { *; }
#-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
#-keepclasseswithmembers class * {
#    @dagger.hilt.* <methods>;
#}

# Prevent R8 from over-optimizing constructors (causes StackOverflow with Hilt + proguard-android-optimize.txt)
-keepclassmembers,allowobfuscation class * {
    @dagger.hilt.internal.GeneratedEntryPoint <init>(...);
}
-keep,allowobfuscation @dagger.hilt.android.AndroidEntryPoint class *


-keep class com.russoviller.cryogenic.bridge.** { *; }
-keep class com.russoviller.cryogenic.utils.device.VulkanChecker {
    *;
}
-keep class com.russoviller.cryogenic.utils.device.VulkanCapabilities {
    *;
}
-keep interface com.russoviller.cryogenic.utils.device.VulkanLogCallback {
    *;
}
-keep class com.russoviller.cryogenic.game.input.CriticalNativeTest {
    *;
}

# Libraries
-keep class com.github.steveice10.opennbt.** { *; }

# SoraEditor language-textmate
-keep class org.jcodings.** { *; }
# Ponte JS do RV WebPage (CryoBridge)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Mods do Launcher (.cmod): a API usada pelos mods não pode ser renomeada nem removida
-keep class com.russoviller.cryogenic.cmod.api.** { *; }
-keep interface com.russoviller.cryogenic.cmod.api.** { *; }
