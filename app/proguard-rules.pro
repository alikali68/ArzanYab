# ArzanYab ProGuard rules
-keep class androidx.room.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.sqlcipher.**
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ir.arzanyab.app.data.adapter.** { *; }
-keepclassmembers class ir.arzanyab.app.domain.engine.** { *; }
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
