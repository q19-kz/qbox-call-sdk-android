package kz.qbox.call.sdk

import android.os.CountDownTimer
import kz.qbox.call.sdk.logging.Logger
import kz.qbox.call.sdk.socket.WebSocketClient
import kz.qbox.call.sdk.socket.WebSocketClientState
import kz.qbox.call.sdk.webrtc.PeerConnectionClient
import kz.qbox.call.sdk.webrtc.PeerConnectionClientState
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.SessionDescription
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CallManager(
    private val peerConnectionClient: PeerConnectionClient,
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor(),
    private val iceServers: List<IceServer> = listOf(
        IceServer.builder("stun:stun.l.google.com:19302")
            .createIceServer(),
    ),
    private val isAuthZone: Boolean = false,
    var listener: Listener? = null
) : WebSocketClient.Listener, PeerConnectionClient.Listener {

    companion object {
        private const val TAG = "CallManager"
    }

    private var timer: CountDownTimer? = null

    fun init(token: String) {
        Logger.debug(TAG, "init() -> token: $token")
        executorService.execute {
            val isWebSocketRequested = connectToWebSocket(token)
            Logger.debug(TAG, "init() -> isWebSocketRequested: $isWebSocketRequested")
        }
    }

    fun onDestroy() {
        Logger.debug(TAG, "onDestroy()")

        timer?.cancel()
        timer = null

        peerConnectionClient.dispose()
//        peerConnectionClient.shutdown()
        peerConnectionClient.removeListeners()

        WebSocketClient.disconnect()
//        WebSocketClient.shutdown()
        WebSocketClient.removeListeners()
    }

    fun getPeerConnectionClientState(): PeerConnectionClientState =
        peerConnectionClient.peerConnectionClientState

    fun getWebSocketClientState(): WebSocketClientState =
        WebSocketClient.webSocketClientState

    @Deprecated(
        "Use android.media.AudioManager#setMicrophoneMute(true)",
        replaceWith = ReplaceWith(
            "audioManager.setMicrophoneMute(true)",
            "android.media.AudioManager"
        )
    )
    fun onMute(): Boolean =
        peerConnectionClient.setLocalAudioEnabled(false)

    @Deprecated(
        "Use android.media.AudioManager#setMicrophoneMute(false)",
        replaceWith = ReplaceWith(
            "audioManager.setMicrophoneMute(false)",
            "android.media.AudioManager"
        )
    )
    fun onUnmute(): Boolean =
        peerConnectionClient.setLocalAudioEnabled(true)

    fun onDTMFButtonPressed(symbol: String): Boolean =
        WebSocketClient.sendMessage(
            JSONObject(
                mapOf(
                    "event" to "dtmf",
                    "dtmf" to JSONObject(
                        mapOf(
                            "digit" to symbol
                        )
                    )
                )
            )
        )

    fun onHangup(): Boolean {
        Logger.debug(TAG, "onHangup()")

        val enqueued = WebSocketClient.sendMessage(
            JSONObject(
                mapOf(
                    "event" to "hangup"
                )
            )
        )

        if (enqueued) {
            timer?.cancel()
            timer = null
            timer = object : CountDownTimer(10_000L, 1_000L) {
                override fun onTick(p0: Long) {
                    Logger.debug(TAG, "CountDownTimer#onTick() -> p0: $p0")
                }

                override fun onFinish() {
                    Logger.debug(TAG, "CountDownTimer#onFinish()")

                    listener?.onCallEvent(CallEvent.Hangup)

                    peerConnectionClient.dispose()

                    WebSocketClient.disconnect()
                }
            }
            timer?.start()
        }

        return enqueued
    }

    private fun connectToWebSocket(token: String): Boolean {
        Logger.debug(TAG, "connectToWebSocket() -> token: $token")
        val url = QBoxSDK.getWebSocketUrl()
        if (url.isNullOrBlank()) {
            throw IllegalStateException("WebSocket URL is null or blank!")
        }
        return WebSocketClient.connect(
            url = url,
            token = token,
            listener = this
        )
    }

    private fun sendLocalSessionDescription(sessionDescription: SessionDescription): Boolean =
        WebSocketClient.sendMessage(
            JSONObject(
                mapOf(
                    "event" to "call",
                    "call" to mapOf(
                        "auth_zone" to isAuthZone,
                        "sdp" to JSONObject(
                            mapOf(
                                "type" to sessionDescription.type.canonicalForm(),
                                "sdp" to sessionDescription.description
                            )
                        )
                    )
                )
            )
        )

    private fun sendLocalICECandidate(iceCandidate: IceCandidate): Boolean =
        WebSocketClient.sendMessage(
            JSONObject(
                mapOf(
                    "event" to "candidate",
                    "candidate" to JSONObject(
                        mapOf(
                            "sdpMid" to iceCandidate.sdpMid,
                            "sdpMLineIndex" to iceCandidate.sdpMLineIndex,
                            "candidate" to iceCandidate.sdp
                        )
                    )
                )
            )
        )

    /**
     * [WebSocketClient.Listener]
     */

    override fun onWebSocketClientStateChange(state: WebSocketClientState) {
        Logger.debug(TAG, "onWebSocketStateChange() -> state: $state")

        listener?.onWebSocketStateChange(state)

        if (state == WebSocketClientState.Open) {
            peerConnectionClient.createPeerConnection(
                iceServers = iceServers,
                listener = this
            )

            val isLocalMediaStreamCreated = peerConnectionClient.createLocalMediaStream()
            Logger.debug(TAG, "isLocalMediaStreamCreated: $isLocalMediaStreamCreated")

//            peerConnectionClient.addTransceiver(
//                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
//                RtpTransceiver.RtpTransceiverInit(
//                    RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
//                )
//            )

            peerConnectionClient.createOffer()
        }
    }

    override fun onWebSocketMessage(message: JSONObject) {
        Logger.debug(TAG, "onMessage() -> message: $message")

        when (message.getString("event")) {
            "connect" -> {
                listener?.onCallEvent(CallEvent.Connect)
            }

            "answer" -> {
                val answer = message.getJSONObject("answer")
                val sdp = answer.getJSONObject("sdp")
                peerConnectionClient.setRemoteDescription(
                    SessionDescription(
                        SessionDescription.Type.fromCanonicalForm(sdp.getString("type")),
                        sdp.getString("sdp")
                    )
                )
            }

            "candidate" -> {
                val iceCandidate = message.getJSONObject("candidate")
                peerConnectionClient.addRemoteIceCandidate(
                    IceCandidate(
                        iceCandidate.getString("sdpMid"),
                        iceCandidate.getInt("sdpMLineIndex"),
                        iceCandidate.getString("candidate")
                    )
                )
            }

            "hangup" -> {
                timer?.cancel()
                timer = null

                listener?.onCallEvent(CallEvent.Hangup)

                peerConnectionClient.dispose()

                WebSocketClient.disconnect()
            }
        }
    }

    /**
     * [PeerConnectionClient.Listener]
     */

    override fun onPeerConnectionClientStateChange(state: PeerConnectionClientState) {
        Logger.debug(TAG, "onPeerConnectionClientStateChange() -> state: $state")
    }

    override fun onLocalSessionDescription(sessionDescription: SessionDescription) {
        sendLocalSessionDescription(sessionDescription)
    }

    override fun onLocalIceCandidate(iceCandidate: IceCandidate) {
        sendLocalICECandidate(iceCandidate)
    }

    override fun onPeerConnectionStateChange(state: PeerConnection.PeerConnectionState?) {
        Logger.debug(TAG, "onPeerConnectionStateChange() -> state: $state")
        listener?.onWebRTCPeerConnectionChange(state)
    }

    interface Listener {
        fun onCallEvent(event: CallEvent) {}
        fun onWebSocketStateChange(state: WebSocketClientState) {}
        fun onWebRTCPeerConnectionChange(state: PeerConnection.PeerConnectionState?) {}
    }

}