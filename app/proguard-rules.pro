# Keep entire app package to prevent obfuscation issues with internal logic
-keep class com.kavya.zes.** { *; }

# AdMob Rules
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep public class com.google.ads.** {
   public *;
}

# Keep Play Services and Native methods
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Play Core (App Update)
-keep class com.google.android.play.core.** { *; }
-dontwarn com.google.android.play.core.**

# General AppCompat and Material rules
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep class com.google.android.material.** { *; }

# WebView and JavaScript
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Prevent stripping of R class (sometimes helps with resource issues)
-keep class **.R$* {
    public static <fields>;
}
