# Proguard rules for SpamBlocker
-keep class com.antispam.blocker.data.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
