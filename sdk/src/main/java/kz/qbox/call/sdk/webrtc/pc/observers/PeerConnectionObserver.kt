package kz.qbox.call.sdk.webrtc.pc.observers

import kz.qbox.call.sdk.logging.Logger
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.IceCandidateErrorEvent
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver

class PeerConnectionObserver(
    private val onSignalingStateChange: (signalingState: PeerConnection.SignalingState) -> Unit = {},
    private val onPeerConnectionStateChange: (peerConnectionState: PeerConnection.PeerConnectionState?) -> Unit = {},
    private val onIceConnectionStateChange: (iceConnectionState: PeerConnection.IceConnectionState) -> Unit = {},

    private val onLocalIceCandidate: (iceCandidate: IceCandidate) -> Unit = {},

    private val onAddRemoteStream: (mediaStream: MediaStream) -> Unit = {},
    private val onRemoveRemoteStream: (mediaStream: MediaStream) -> Unit = {},

    private val onRenegotiationNeeded: () -> Unit = {},

    private val onAddRemoteTrack: (rtpReceiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) -> Unit = { _, _ -> },
    private val onRemoveRemoteTrack: (rtpReceiver: RtpReceiver?) -> Unit = {},
    private val onRemoteTrack: (transceiver: RtpTransceiver?) -> Unit = {},
) : PeerConnection.Observer {

    companion object {
        private const val TAG = "PeerConnectionObserver"
    }

    override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {
        Logger.debug(TAG, "onSignalingChange() -> signalingState: $signalingState")

        onSignalingStateChange.invoke(signalingState)
    }

    override fun onConnectionChange(peerConnectionState: PeerConnection.PeerConnectionState?) {
        Logger.debug(TAG, "onConnectionChange() -> peerConnectionState: $peerConnectionState")

        onPeerConnectionStateChange.invoke(peerConnectionState)
    }

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
        Logger.debug(TAG, "onIceConnectionChange() -> iceConnectionState: $iceConnectionState")

        onIceConnectionStateChange.invoke(iceConnectionState)
    }

    override fun onIceConnectionReceivingChange(b: Boolean) {
        Logger.debug(TAG, "onIceConnectionReceivingChange() -> b: $b")
    }

    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {
        Logger.debug(TAG, "onIceGatheringChange() -> iceGatheringState: $iceGatheringState")
    }

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        Logger.debug(TAG, "onIceCandidate() -> iceCandidate: $iceCandidate")

        onLocalIceCandidate.invoke(iceCandidate)
    }

    override fun onIceCandidateError(event: IceCandidateErrorEvent?) {
        Logger.debug(
            TAG, "onIceCandidateError() -> " +
                    "address: ${event?.address}, " +
                    "port: ${event?.port}, " +
                    "url: ${event?.url}, " +
                    "errorCode: ${event?.errorCode}, " +
                    "errorText: ${event?.errorText}"
        )
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
        Logger.debug(
            TAG,
            "onIceCandidatesRemoved() -> iceCandidates: ${iceCandidates.contentToString()}"
        )
    }

    override fun onAddStream(mediaStream: MediaStream) {
        Logger.debug(TAG, "onAddStream() -> mediaStream: $mediaStream")

        onAddRemoteStream.invoke(mediaStream)
    }

    override fun onRemoveStream(mediaStream: MediaStream) {
        Logger.debug(TAG, "onRemoveStream() -> mediaStream: $mediaStream")

        onRemoveRemoteStream.invoke(mediaStream)
    }

    override fun onDataChannel(dataChannel: DataChannel) {
        Logger.debug(TAG, "onDataChannel() -> dataChannel: $dataChannel")
    }

    override fun onRenegotiationNeeded() {
        Logger.debug(TAG, "onRenegotiationNeeded()")

        onRenegotiationNeeded.invoke()
    }

    override fun onAddTrack(
        rtpReceiver: RtpReceiver?,
        mediaStreams: Array<out MediaStream>?
    ) {
        Logger.debug(TAG, "onAddTrack() -> $rtpReceiver, ${mediaStreams.contentToString()}")

        onAddRemoteTrack.invoke(rtpReceiver, mediaStreams)
    }

    override fun onRemoveTrack(rtpReceiver: RtpReceiver?) {
        Logger.debug(TAG, "onRemoveTrack() -> $rtpReceiver")

        onRemoveRemoteTrack.invoke(rtpReceiver)
    }

    override fun onTrack(transceiver: RtpTransceiver?) {
        Logger.debug(TAG, "onTrack() -> transceiver: $transceiver")

        onRemoteTrack.invoke(transceiver)
    }

}