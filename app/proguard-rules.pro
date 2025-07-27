# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========================================
# Eclipse Milo OPC UA Library Rules
# ========================================

# Keep Eclipse Milo core classes
-keep class org.eclipse.milo.opcua.** { *; }
-keep class org.eclipse.milo.** { *; }
-dontwarn org.eclipse.milo.**

# Keep Netty classes used by Milo
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Keep Bouncy Castle classes (used for security)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep JCTOOLS classes (used by Netty)
-keep class org.jctools.** { *; }
-dontwarn org.jctools.**

# ========================================
# Method Size Optimization
# ========================================

# Enable aggressive optimization
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification

# Reduce method size by splitting large methods
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ========================================
# Android Architecture Components
# ========================================

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-dontwarn dagger.hilt.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ========================================
# Kotlin Coroutines
# ========================================

# Keep coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Flow
-keep class kotlinx.coroutines.flow.** { *; }

# ========================================
# Gson (if used)
# ========================================

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepattributes Signature
-keepattributes *Annotation*

# Keep data classes used with Gson
-keep class com.example.s7opcuaapp.data.model.** { *; }

# ========================================
# App-specific Rules
# ========================================

# Keep all model classes
-keep class com.example.s7opcuaapp.data.model.** { *; }

# Keep ViewModels
-keep class com.example.s7opcuaapp.viewmodel.** { *; }

# Keep Repository interfaces and implementations
-keep class com.example.s7opcuaapp.data.repository.** { *; }

# Keep utility classes
-keep class com.example.s7opcuaapp.util.** { *; }

# ========================================
# Network and Security
# ========================================

# Keep SSL/TLS classes
-keep class javax.net.ssl.** { *; }
-keep class javax.security.** { *; }

# Keep network connectivity classes
-keep class android.net.** { *; }

# ========================================
# Reflection and Serialization
# ========================================

# Keep classes that use reflection
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========================================
# Performance Optimizations
# ========================================

# Remove logging in release builds (optional)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove assertions in release builds
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullExpressionValue(...);
    public static void checkParameterIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkFieldIsNotNull(...);
    public static void checkReturnedValueIsNotNull(...);
}

# ========================================
# Method Size Optimization Specific Rules
# ========================================

# Split large methods automatically
-optimizations !method/inlining/*
-optimizations !class/unboxing/enum

# Keep method signatures but allow optimization
-keepclassmembernames class * {
    public <methods>;
}

# Allow ProGuard to rename classes and methods to reduce size
-printmapping mapping.txt

# ========================================
# Warnings to Ignore
# ========================================

# Ignore warnings about missing classes (they might be optional dependencies)
-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signal

# Ignore warnings about Java 8+ features on older Android versions
-dontwarn java.lang.invoke.**
-dontwarn java.time.**

# ========================================
# Debug Information (for release builds)
# ========================================

# Keep stack traces readable
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile