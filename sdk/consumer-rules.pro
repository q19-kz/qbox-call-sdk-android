-keep class kz.qbox.** { *; }

-keep class org.webrtc.** { *; }

-keep class org.jni_zero.** { *; }

-keepclassmembers class * {
    native <methods>;
}