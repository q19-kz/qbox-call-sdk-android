package kz.qbox.call.sdk.sample.presentation

import android.Manifest
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import kz.qbox.call.sdk.webrtc.Options
import kz.qbox.call.sdk.webrtc.PeerConnectionClient

const val TAG = "SampleScreen"

@ExperimentalMaterial3Api
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SampleScreen(
    context: Context,
    viewModel: SampleViewModel = viewModel {
        SampleViewModel(
            audioManager = ContextCompat.getSystemService(context, AudioManager::class.java),
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
    val uiState by viewModel.uiState.collectAsState()

    val modifyAudioSettingsPermissionState = rememberPermissionState(
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )

    val recordAudioPermissionState = rememberPermissionState(
        Manifest.permission.RECORD_AUDIO
    )

    var isAudioOutputSelectDialogVisible by remember { mutableStateOf(false) }

    if (isAudioOutputSelectDialogVisible) {
        val audioDevices = viewModel.getAudioOutputDevices()

        BasicAlertDialog(
            onDismissRequest = {
                isAudioOutputSelectDialogVisible = false
            }
        ) {
            Surface(
                modifier = Modifier.wrapContentWidth().wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    LazyColumn {
                        items(audioDevices.size) {
                            TextButton(
                                onClick = {
                                    viewModel.onAudioOutputDeviceSelected(audioDevices[it])
                                    isAudioOutputSelectDialogVisible = false
                                }
                            ) {
                                Text(text = audioDevices[it].name)
                            }
                        }
                    }
                }
            }
        }
    }

    Scaffold { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Text("Permissions: ${listOf(modifyAudioSettingsPermissionState, recordAudioPermissionState).all { it.status.isGranted }}")
            }
            Row {
                Text("Audio input: ${uiState.isMuted}")
            }
            Row {
                Text("Audio output: ${uiState.audioDevice?.name}")
            }
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
                        if (!modifyAudioSettingsPermissionState.status.isGranted) {
                            modifyAudioSettingsPermissionState.launchPermissionRequest()
                        }

                        if (!recordAudioPermissionState.status.isGranted) {
                            recordAudioPermissionState.launchPermissionRequest()
                        }
                    }
                ) {
                    Text(text = "Request permissions")
                }
            }
            Row {
                TextButton(
                    onClick = {
                        isAudioOutputSelectDialogVisible = !isAudioOutputSelectDialogVisible
                    }
                ) {
                    Text(text = "Audio output")
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
            Row {
                TextButton(
                    onClick = {
                        viewModel.onHangup()
                    }
                ) {
                    Text("Hangup")
                }
            }
        }
    }
}
