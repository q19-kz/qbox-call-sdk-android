# CHANGELOG

**Before you upgrade: Breaking changes might happen in major and minor versions of packages.<br/>
See the [Migration Guide](MIGRATION.md) for the complete breaking changes list.**

## 1.1.3

- Separated Peer connection instance create from WebSocket as `CallManager#onCall()` method,
  that's why you should call method, in order to make a call.
  Before a call was made automatically after WebSocket success connect.
- `CallEvent.Hangup` changed from `data object` to `data class` and added variable `errorCode`.
