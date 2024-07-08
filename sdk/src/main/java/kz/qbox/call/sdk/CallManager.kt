package kz.qbox.call.sdk

import android.media.AudioManager
import android.util.Log
import kz.qbox.call.sdk.socket.WebSocketClient
import kz.qbox.call.sdk.socket.WebSocketState
import kz.qbox.call.sdk.webrtc.PeerConnectionClient
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CallManager(
    private val audioManager: AudioManager?,
    private val peerConnectionClient: PeerConnectionClient,
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor(),
    var listener: Listener? = null
) : WebSocketClient.Listener, PeerConnectionClient.Listener {

    companion object {
        private const val TAG = "CallManager"
    }

    fun init() {
        QBoxSDK.setLoggingEnabled(true)

        executorService.execute {
            val isConnectedToWebSocket = connectToWebSocket()
            Log.d(TAG, "init() -> isConnectedToWebSocket: $isConnectedToWebSocket")
        }
    }

    fun onDestroy() {
        peerConnectionClient.dispose()
        peerConnectionClient.shutdown()
        peerConnectionClient.removeListeners()

        WebSocketClient.disconnect()
        WebSocketClient.shutdown()
        WebSocketClient.removeListeners()
    }

    fun onCall(): Boolean {
        executorService.execute {
            Log.d(TAG, "onCall() -> webSocketState: ${WebSocketClient.webSocketState}")
            val isConnectedToWebSocket = connectToWebSocket()
            Log.d(TAG, "onCall() -> isConnectedToWebSocket: $isConnectedToWebSocket")
        }

        return true
    }

    fun onMute(): Boolean =
        peerConnectionClient.setLocalAudioEnabled(false)

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

    fun onHangup(): Boolean =
        WebSocketClient.sendMessage(
            JSONObject(
                mapOf(
                    "event" to "hangup"
                )
            )
        )

    private fun connectToWebSocket(): Boolean =
        WebSocketClient.connect(
            url = "wss://dial.vlx.kz/websocket",
            listener = this
        )

    private fun sendLocalSessionDescription(sessionDescription: SessionDescription): Boolean =
        WebSocketClient.sendMessage(
            JSONObject(
                mapOf(
                    "event" to "call",
                    "call" to mapOf(
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

    override fun onWebSocketStateChange(state: WebSocketState) {
        listener?.onWebSocketStateChange(state)

        if (state == WebSocketState.Open) {
            peerConnectionClient.createPeerConnection(
                iceServers = listOf(
                    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                        .createIceServer(),
                    PeerConnection.IceServer.builder("turn:195.12.123.27:3478?transport=udp")
                        .setUsername("test")
                        .setPassword("test")
                        .createIceServer()
                ),
                listener = this
            )

            val isLocalMediaStreamCreated = peerConnectionClient.createLocalMediaStream()
            Log.d(TAG, "isLocalMediaStreamCreated: $isLocalMediaStreamCreated")

            peerConnectionClient.addTransceiver(
                MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
                RtpTransceiver.RtpTransceiverInit(
                    RtpTransceiver.RtpTransceiverDirection.RECV_ONLY
                )
            )

            peerConnectionClient.createOffer()

            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isBluetoothScoOn = false
            audioManager?.isSpeakerphoneOn = true
        }
    }

    override fun onWebSocketMessage(message: JSONObject) {
        Log.d(TAG, "onMessage() -> message: $message")

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
                listener?.onCallEvent(CallEvent.Hangup)

                peerConnectionClient.close()

                WebSocketClient.disconnect()
            }
        }
    }

    /**
     * [PeerConnectionClient.Listener]
     */

    override fun onLocalSessionDescription(sessionDescription: SessionDescription) {
        sendLocalSessionDescription(sessionDescription)
    }

    override fun onLocalIceCandidate(iceCandidate: IceCandidate) {
        sendLocalICECandidate(iceCandidate)
    }

    override fun onPeerConnectionStateChange(peerConnectionState: PeerConnection.PeerConnectionState?) {
        listener?.onWebRTCPeerConnectionChange(peerConnectionState)
    }

    interface Listener {
        fun onCallEvent(event: CallEvent) {}
        fun onWebSocketStateChange(state: WebSocketState) {}
        fun onWebRTCPeerConnectionChange(peerConnectionState: PeerConnection.PeerConnectionState?) {}
    }

}