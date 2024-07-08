package kz.qbox.call.sdk.webrtc

import android.content.Context
import kz.qbox.call.sdk.logging.Logger
import kz.qbox.call.sdk.safeShutdown
import kz.qbox.call.sdk.webrtc.audio.AudioTrackErrorCallback
import kz.qbox.call.sdk.webrtc.audio.AudioTrackStateCallback
import kz.qbox.call.sdk.webrtc.constraints.AudioBooleanConstraint
import kz.qbox.call.sdk.webrtc.constraints.AudioIntegerConstraint
import kz.qbox.call.sdk.webrtc.constraints.OfferAnswerConstraint
import kz.qbox.call.sdk.webrtc.constraints.RTCConstraints
import kz.qbox.call.sdk.webrtc.constraints.addConstraints
import kz.qbox.call.sdk.webrtc.constraints.toMediaConstraints
import kz.qbox.call.sdk.webrtc.ice.observers.RemoteICEAddObserver
import kz.qbox.call.sdk.webrtc.pc.observers.PeerConnectionObserver
import kz.qbox.call.sdk.webrtc.sdp.observers.LocalSDPObserver
import kz.qbox.call.sdk.webrtc.sdp.observers.OfferObserver
import kz.qbox.call.sdk.webrtc.sdp.observers.RemoteSDPObserver
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SessionDescription
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PeerConnectionClient private constructor(
    private val contextReference: WeakReference<Context>,

    private val options: Options = Options(),

    ioThread: ExecutorService? = null
) {

    companion object {
        private val TAG = PeerConnectionClient::class.java.simpleName
    }

    constructor(
        context: Context,
        options: Options = Options(),
        ioThread: ExecutorService? = null
    ) : this(
        contextReference = WeakReference(context),
        options = options,
        ioThread = ioThread
    )

    private val context: Context?
        get() = contextReference.get()

    private val executor = ioThread ?: Executors.newSingleThreadExecutor()

    private var listener: Listener? = null

    private var iceServers: List<PeerConnection.IceServer>? = null

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    private var audioDeviceModule: AudioDeviceModule? = null

    private var localAudioSource: AudioSource? = null

    private var localMediaStream: MediaStream? = null
    private var remoteMediaStream: MediaStream? = null

    private var localAudioTrack: AudioTrack? = null
    private var remoteAudioTrack: AudioTrack? = null

    private val audioBooleanConstraints by lazy {
        RTCConstraints<AudioBooleanConstraint, Boolean>().apply {
        }
    }

    private val audioIntegerConstraints by lazy {
        RTCConstraints<AudioIntegerConstraint, Int>()
    }

    private val offerAnswerConstraints by lazy {
        RTCConstraints<OfferAnswerConstraint, Boolean>().apply {
            addMandatoryConstraint(OfferAnswerConstraint.OFFER_TO_RECEIVE_AUDIO, true)
            addMandatoryConstraint(OfferAnswerConstraint.OFFER_TO_RECEIVE_VIDEO, false)
        }
    }

    private val peerConnectionObserver = PeerConnectionObserver(
        onPeerConnectionStateChange = { listener?.onPeerConnectionStateChange(it) },
        onIceConnectionStateChange = { listener?.onIceConnectionStateChange(it) },
        onLocalIceCandidate = { listener?.onLocalIceCandidate(it) },
        onAddRemoteStream = { listener?.onAddRemoteStream(it) },
        onRemoveRemoteStream = { listener?.onRemoveRemoteStream(it) }
    )

    private val localSDPObserver = LocalSDPObserver(
        onSetSuccess = {
            val sdp = localSessionDescription
            if (sdp == null) {
                Logger.error(TAG, "localSDPObserver#onSetSuccess() -> [SDP null]")
                listener?.onPeerConnectionError(PeerConnectionError.InvalidLocalSessionDescription)
            } else {
                Logger.debug(TAG, "localSDPObserver#onSetSuccess() -> sdp: ${sdp.type}")
                listener?.onLocalSessionDescription(sdp)
            }
        },
        onSetFailure = { errorMessage ->
            Logger.debug(TAG, "localSDPObserver#onSetSuccess() -> errorMessage: $errorMessage")
            listener?.onPeerConnectionError(PeerConnectionError.LocalSessionDescriptionSetFailure)
        }
    )

    private val remoteSDPObserver = RemoteSDPObserver(
        onSetSuccess = {
            Logger.debug(TAG, "remoteSDPObserver#onSetSuccess()")
        },
        onSetFailure = { errorMessage ->
            Logger.debug(TAG, "remoteSDPObserver#onSetFailure() -> errorMessage: $errorMessage")
            listener?.onPeerConnectionError(PeerConnectionError.RemoteSessionDescriptionSetFailure)
        }
    )

    private val offerObserver = OfferObserver(
        onCreateSuccess = { localSessionDescription ->
            Logger.debug(
                TAG,
                "offerObserver#onCreateSuccess() -> sdp: ${localSessionDescription.type}"
            )
            this.localSessionDescription = localSessionDescription
        },
        onCreateFailure = { errorMessage ->
            Logger.debug(TAG, "offerObserver#onCreateFailure() -> errorMessage: $errorMessage")
            listener?.onPeerConnectionError(PeerConnectionError.LocalSessionDescriptionCreateFailure)
        }
    )

    private var localSessionDescription: SessionDescription? = null
        set(value) {
            field = value

            value?.let {
                Logger.debug(TAG, "localSessionDescription#set() -> sdp: ${it.type}")
                peerConnection?.setLocalDescription(localSDPObserver, it)
            }
        }

    private var remoteSessionDescription: SessionDescription? = null
        set(value) {
            field = value

            value?.let {
                Logger.debug(TAG, "remoteSessionDescription#set() -> sdp: ${it.type}")
                peerConnection?.setRemoteDescription(remoteSDPObserver, it)
            }
        }

    @Throws(IllegalStateException::class)
    fun createPeerConnection(
        iceServers: List<PeerConnection.IceServer> = emptyList(),

        audioBooleanConstraints: RTCConstraints<AudioBooleanConstraint, Boolean>? = null,
        audioIntegerConstraints: RTCConstraints<AudioIntegerConstraint, Int>? = null,
        offerAnswerConstraints: RTCConstraints<OfferAnswerConstraint, Boolean>? = null,

        listener: Listener? = null
    ): PeerConnection? {
        Logger.debug(TAG, "createPeerConnection() -> options: $options, iceServers: $iceServers")

        this.iceServers = iceServers
        this.listener = listener

        localSessionDescription = null
        remoteSessionDescription = null

        audioBooleanConstraints?.let {
            this.audioBooleanConstraints += it
        }
        audioIntegerConstraints?.let {
            this.audioIntegerConstraints += it
        }
        offerAnswerConstraints?.let {
            this.offerAnswerConstraints += it
        }

        val future = executor.submit(Callable {
            if (context == null) return@Callable null

            val initializationOptions = PeerConnectionFactory.InitializationOptions
                .builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()

            PeerConnectionFactory.initialize(initializationOptions)

            val peerConnectionFactoryOptions = PeerConnectionFactory.Options()
            peerConnectionFactoryOptions.disableNetworkMonitor = true

            audioDeviceModule = createJavaAudioDeviceModule()

            audioDeviceModule?.setMicrophoneMute(false)
            audioDeviceModule?.setSpeakerMute(false)

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(peerConnectionFactoryOptions)
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory()

            peerConnection = peerConnectionFactory?.let { createPeerConnectionInternally(it) }

            return@Callable peerConnection
        })

        return future.get()
    }

    private fun createJavaAudioDeviceModule(): JavaAudioDeviceModule =
        JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .setAudioTrackStateCallback(AudioTrackStateCallback())
            .setAudioTrackErrorCallback(AudioTrackErrorCallback())
            .createAudioDeviceModule()

    fun createLocalMediaStream(): Boolean {
        Logger.debug(TAG, "createLocalMediaStream()")

        localMediaStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS")

        val audioTrack = createAudioTrack()
        if (audioTrack != null) {
            val rtpSender = try {
                peerConnection?.addTrack(audioTrack, listOf("AudioTrack"))
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            return rtpSender != null
        }

        return false
    }

    fun addRemoteStreamToPeer(mediaStream: MediaStream): Boolean {
        Logger.debug(TAG, "addRemoteStreamToPeer() -> mediaStream: $mediaStream")

//        try {
//            val id = mediaStream.id
//            Logger.debug(TAG, "addRemoteStreamToPeer() [MediaStream exists] -> id: $id")
//        } catch (e: IllegalStateException) {
//            Logger.debug(TAG, "addRemoteStreamToPeer() [MediaStream does not exist]")
//            return false
//        }

        remoteMediaStream = mediaStream

        if (mediaStream.audioTracks.isNotEmpty()) {
            remoteAudioTrack = mediaStream.audioTracks.first()
            remoteAudioTrack?.setEnabled(options.isRemoteAudioEnabled)
        }

        return true
    }

    private fun createAudioTrack(): AudioTrack? {
        Logger.debug(TAG, "createAudioTrack()")

        localAudioSource = peerConnectionFactory?.createAudioSource(
            MediaConstraints().apply {
                addConstraints(audioBooleanConstraints, audioIntegerConstraints)
            }
        )

        localAudioTrack = peerConnectionFactory?.createAudioTrack(
            "ARDAMSa0",
            localAudioSource
        )
        localAudioTrack?.setEnabled(options.isLocalAudioEnabled)

        return localAudioTrack
    }

    fun addRemoteIceCandidate(iceCandidate: IceCandidate) {
        Logger.debug(TAG, "addRemoteIceCandidate() -> iceCandidate: $iceCandidate")

        executor.execute {
            peerConnection?.addIceCandidate(
                iceCandidate,
                RemoteICEAddObserver(iceCandidate.hashCode())
            )
        }
    }

    fun setRemoteDescription(sessionDescription: SessionDescription) {
        Logger.debug(TAG, "setRemoteDescription() -> sdp: ${sessionDescription.type}")

        this.remoteSessionDescription = sessionDescription
    }

    fun createOffer() {
        Logger.debug(TAG, "createOffer()")

        executor.execute {
            peerConnection?.createOffer(offerObserver, offerAnswerConstraints.toMediaConstraints())
        }
    }

    private fun createPeerConnectionInternally(factory: PeerConnectionFactory): PeerConnection? {
        Logger.debug(TAG, "createPeerConnectionInternally() -> factory: $factory")

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers ?: emptyList())

        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXCOMPAT
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy =
            PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN

        return factory.createPeerConnection(rtcConfig, peerConnectionObserver)
    }

    fun setLocalAudioEnabled(isEnabled: Boolean): Boolean {
        return localAudioTrack?.setEnabled(isEnabled) == true
    }

    fun setRemoteAudioEnabled(isEnabled: Boolean): Boolean {
        return remoteAudioTrack?.setEnabled(isEnabled) == true
    }

    fun addStream(mediaStream: MediaStream): Boolean =
        peerConnection?.addStream(mediaStream) == true

    fun removeStream(mediaStream: MediaStream) {
        peerConnection?.removeStream(mediaStream)
    }

    fun removeMediaStreamTrack(mediaStreamTrack: MediaStreamTrack): Boolean =
        when {
            mediaStreamTrack.kind() == MediaStreamTrack.AUDIO_TRACK_KIND -> {
                remoteMediaStream?.removeTrack(remoteAudioTrack) == true
            }

            else -> {
                false
            }
        }

    fun addTransceiver(
        mediaType: MediaStreamTrack.MediaType,
        transceiverInit: RtpTransceiver.RtpTransceiverInit? = null
    ): RtpTransceiver? =
        peerConnection?.addTransceiver(mediaType, transceiverInit)

    fun setLocalAudioTrackVolume(volume: Double) = localAudioTrack?.setVolume(volume)

    fun setRemoteAudioTrackVolume(volume: Double) = remoteAudioTrack?.setVolume(volume)

    fun removeListeners() {
        listener = null
    }

    fun reset() {
        iceServers = null

        localSessionDescription = null
        remoteSessionDescription = null
    }

    fun close() {
        reset()

        safeShutdown(
            name = "peerConnection",
            action = {
                peerConnection?.close()
            }
        )
    }

    fun dispose(): Boolean {
        reset()

        safeShutdown(
            name = "audioDeviceModule",
            action = {
                audioDeviceModule?.release()
            },
            onComplete = {
                audioDeviceModule = null
            }
        )

        safeShutdown(
            name = "localAudioSource",
            action = {
                localAudioSource?.dispose()
            },
            onComplete = {
                localAudioSource = null
            }
        )

        safeShutdown(
            name = "localAudioTrack",
            action = {
                localAudioTrack?.dispose()
            },
            onComplete = {
                localAudioTrack = null
            }
        )

        safeShutdown(
            name = "remoteAudioTrack",
            action = {
                remoteAudioTrack?.dispose()
            },
            onComplete = {
                remoteAudioTrack = null
            }
        )

        safeShutdown(
            name = "localMediaStream",
            action = {
                localMediaStream?.dispose()
            },
            onComplete = {
                localMediaStream = null
            }
        )

        safeShutdown(
            name = "peerConnection",
            action = {
                peerConnection?.dispose()
            },
            onComplete = {
                peerConnection = null
            }
        )

        safeShutdown(
            name = "peerConnectionFactory",
            action = {
                peerConnectionFactory?.dispose()
            },
            onComplete = {
                peerConnectionFactory = null
            }
        )

        return true
    }

    fun shutdown() {
        executor.shutdown()
    }

    interface Listener {
        fun onPeerConnectionStateChange(peerConnectionState: PeerConnection.PeerConnectionState?) {}
        fun onIceConnectionStateChange(iceConnectionState: PeerConnection.IceConnectionState) {}

        fun onLocalSessionDescription(sessionDescription: SessionDescription) {}
        fun onLocalIceCandidate(iceCandidate: IceCandidate) {}

        fun onAddRemoteStream(mediaStream: MediaStream) {}
        fun onRemoveRemoteStream(mediaStream: MediaStream) {}

        fun onPeerConnectionError(error: PeerConnectionError) {}
    }

}