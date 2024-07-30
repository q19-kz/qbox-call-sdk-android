package kz.qbox.call.sdk.sample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this)
        button.text = "Launch"
        button.setOnClickListener {
            startActivity(Intent(this, SampleActivity::class.java))
        }

        addContentView(
            button,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        )
    }

}
