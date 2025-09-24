package kz.qbox.call.sdk.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.qbox.call.sdk.sample.presentation.SampleScreen
import kz.qbox.call.sdk.sample.presentation.theme.QBoxCallSDKTheme

@ExperimentalMaterial3Api
class SampleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isAuthZone = intent.getBooleanExtra("is_auth_zone", false)

        setContent {
            QBoxCallSDKTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SampleScreen(applicationContext, isAuthZone)
                }
            }
        }
    }

}