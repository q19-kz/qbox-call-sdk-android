## Добавление зависимости

### Ссылка

```
https://jitpack.io/#q19-kz/qbox-call-sdk-android/1.1.4
```

### Инструкция

Добавить репозиторий в родительский `build.gradle`

```gradle
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}
```

Добавить зависимость

```gradle
dependencies {
  implementation 'com.github.q19-kz:qbox-call-sdk-android:1.1.4
}
```

Требуемые разрешения

```
Manifest.permission.MODIFY_AUDIO_SETTINGS
Manifest.permission.RECORD_AUDIO
```

> Разрешения добавлены в `AndroidManifest.xml` библиотеки

### Пример

Ссылка

```
https://github.com/q19-kz/qbox-call-sdk-android/tree/master/sample
```

#### Инструкция

Передать конфигурационные настройки

```kotlin
QBoxSDK.init(
  isLoggingEnabled = true,
  webSocketUrl = "<Адрес WebSocket>"
)
```

Сгенерировать token

```
https://<домен>/api/generate
```

Объявить CallManager

```kotlin
CallManager(
  isAuthZone = false,
  peerConnectionClient = PeerConnectionClient(
    context = applicationContext,
    options = Options(
      isLocalAudioEnabled = true,
      isRemoteAudioEnabled = true
    )
  ),
  listener = this
)
```
            
Инициализировать CallManager

```kotlin
callManager.init(token = token)
```

Прочее

Передача DTMF команды

```kotlin
callManager.onDTMFButtonPressed(<symbol>)
```

Включить/выключить микрофон

```kotlin
callManager.onMute(), callManager.onUnmute()
```

Завершение звонка

```kotlin
callManager.onHangup()
```

Управление аудиовыходами

Рекомендация:

```
https://github.com/twilio/audioswitch
```

Зависимости библиотеки

1. https://square.github.io/okhttp/, версия 4.12.0
2. https://github.com/webrtc-sdk/android, версия 137.7151.04
