# Migration Guide

This document gathered all breaking changes and migrations requirement between versions.

<!--
When new content need to be added to the migration guide, make sure they're following the format:
1. Add a version in the *Breaking versions* section, with a version anchor.
2. Use *Summary* and *Details* to introduce the migration.
-->

## Breaking versions

- [1.1.3](#113)

## 1.1.3

### Summary

- Separated Peer connection instance create from WebSocket as `CallManager#onCall()` method, 
  that's why you should call method, in order to make a call. 
  Before a call was made automatically after WebSocket success connect.
- `CallEvent.Hangup` changed from `data object` to `data class` and added variable `errorCode`.

### Details

#### `CallManager#onCall()`

```diff
  override fun onWebSocketClientStateChange(state: WebSocketClientState) {
    Logger.debug(TAG, "onWebSocketStateChange() -> state: $state")

    listener?.onWebSocketStateChange(state)

-   if (state == WebSocketClientState.Open) {
-     peerConnectionClient.createPeerConnection(
-       iceServers = iceServers,
-       listener = this
-     )
-
-     val isLocalMediaStreamCreated = peerConnectionClient.createLocalMediaStream()
-     Logger.debug(TAG, "isLocalMediaStreamCreated: $isLocalMediaStreamCreated")
-
-     peerConnectionClient.createOffer()
-   }
  }
```

```kotlin
// Make a call (creates PeerConnection instance, then creates local media stream with offer)
callManager.onCall()
```

#### `CallEvent.Hangup`

```diff
  sealed class CallEvent {
    data object Connect : CallEvent()
-   data object Hangup : CallEvent()
  }
```

```diff
  sealed class CallEvent {
    data object Connect : CallEvent()
+   data class Hangup(val errorCode: String? = null) : CallEvent()
  }
```
