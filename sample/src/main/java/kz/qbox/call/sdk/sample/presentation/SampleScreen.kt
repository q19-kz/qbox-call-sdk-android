package kz.qbox.call.sdk.sample.presentation

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import kz.qbox.call.sdk.webrtc.Options
import kz.qbox.call.sdk.webrtc.PeerConnectionClient

const val TAG = "SampleScreen"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SampleScreen(
    context: Context,
    viewModel: SampleViewModel = viewModel {
        SampleViewModel(
            audioManager = context.getSystemService<AudioManager>(),
            audioSwitch = AudioSwitch(
                context = context,
                loggingEnabled = true,
                audioFocusChangeListener = {
                    Log.d(TAG, "AudioSwitch#audioFocusChangeListener() -> $it")
                },
                preferredDeviceList = listOf(
                    AudioDevice.Speakerphone::class.java,
                    AudioDevice.BluetoothHeadset::class.java,
                    AudioDevice.WiredHeadset::class.java,
                    AudioDevice.Earpiece::class.java
                )
            ),
            peerConnectionClient = PeerConnectionClient(
                context = context,
                options = Options(
                    isLocalAudioEnabled = true,
                    isRemoteAudioEnabled = true
                )
            )
        )
    }
) {
    val accessNetworkStatePermissionsState = rememberPermissionState(
        android.Manifest.permission.ACCESS_NETWORK_STATE
    )

    val modifyAudioSettingsPermissionState = rememberPermissionState(
        android.Manifest.permission.MODIFY_AUDIO_SETTINGS
    )

    val recordAudioPermissionState = rememberPermissionState(
        android.Manifest.permission.RECORD_AUDIO
    )

    val uiState by viewModel.uiState.collectAsState()

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Text("WebSocket: ${uiState.webSocketState}")
            }
            Row {
                Text("WebRTC: ${uiState.webRTCState}")
            }
            Row {
                Text("Call: ${uiState.callEvent}")
            }
            Box(modifier = Modifier.height(20.dp))
            LazyColumn {
                items(DTMF_KEYPAD.size) { columnIndex ->
                    LazyRow {
                        items(DTMF_KEYPAD[columnIndex].size) { rowIndex ->
                            val symbol = DTMF_KEYPAD[columnIndex][rowIndex]["symbol"].orEmpty()
                            val label = DTMF_KEYPAD[columnIndex][rowIndex]["label"]
                            DTMFButton(
                                symbol = symbol,
                                label = label
                            ) {
                                viewModel.onDTMFButtonPressed(symbol)
                            }
                        }
                    }
                }
            }
            Box(modifier = Modifier.height(20.dp))
            Row {
                TextButton(
                    onClick = {
                        viewModel.onHangup()
                    }
                ) {
                    Text("Hangup")
                }
                TextButton(
                    onClick = {
                        if (!accessNetworkStatePermissionsState.status.isGranted) {
                            accessNetworkStatePermissionsState.launchPermissionRequest()
                        }

                        if (!modifyAudioSettingsPermissionState.status.isGranted) {
                            modifyAudioSettingsPermissionState.launchPermissionRequest()
                        }

                        if (!recordAudioPermissionState.status.isGranted) {
                            recordAudioPermissionState.launchPermissionRequest()
                        }

                        viewModel.onCall()
                    }
                ) {
                    Text("Call")
                }
            }
            Row {
                TextButton(
                    onClick = {
                        viewModel.onMute()
                    }
                ) {
                    Text("Mute")
                }
                TextButton(
                    onClick = {
                        viewModel.onUnmute()
                    }
                ) {
                    Text("Unmute")
                }
            }
        }
    }
}
