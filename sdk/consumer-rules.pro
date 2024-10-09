-keep class kz.qbox.** { *; }

-keep class org.webrtc.** { *; }

-keepclassmembers class * {
    native <methods>;
}