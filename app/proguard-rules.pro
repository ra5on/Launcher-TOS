# R8-Regeln für Launcher TOS.

# kotlinx.serialization: generierte Serializer des Modells (Layout, Einstellungen, Export/Import) behalten.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class de.umstieg.launcher.core.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.umstieg.launcher.core.**$$serializer { *; }
-keepclassmembers class de.umstieg.launcher.core.** {
    *** Companion;
}
-keepclasseswithmembers class de.umstieg.launcher.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Enum-Namen sind Teil des gespeicherten JSON.
-keepclassmembers enum de.umstieg.launcher.core.** { *; }
