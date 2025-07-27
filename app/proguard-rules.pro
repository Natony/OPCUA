# Eclipse Milo OPC UA
-keep class org.eclipse.milo.** { *; }
-keepclassmembers class org.eclipse.milo.** { *; }
-dontwarn org.eclipse.milo.**

# Netty (used by Milo)
-keep class io.netty.** { *; }
-keepclassmembers class io.netty.** { *; }
-dontwarn io.netty.**

# Bouncy Castle (if used)
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Fix for method size limit
-dontoptimize
-dontobfuscate

# Keep all annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions