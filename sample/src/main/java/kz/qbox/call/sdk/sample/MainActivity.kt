package kz.qbox.call.sdk.sample

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api

@ExperimentalMaterial3Api
class MainActivity : ComponentActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this)
        rootLayout.orientation = LinearLayout.VERTICAL
        rootLayout.gravity = Gravity.CENTER

        var isAuthZone = false

        val authZoneLayout = LinearLayout(this)
        authZoneLayout.orientation = LinearLayout.HORIZONTAL
        authZoneLayout.gravity = Gravity.CENTER

        val isAuthZoneTextView = TextView(this)
        isAuthZoneTextView.text = "Auth zone?".uppercase()
        authZoneLayout.addView(
            isAuthZoneTextView,
            ViewGroup.MarginLayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 50
            }
        )

        val isAuthZoneButton = Button(this)
        isAuthZoneButton.text = isAuthZone.toString()
        isAuthZoneButton.setOnClickListener {
            isAuthZone = !isAuthZone
            isAuthZoneButton.text = isAuthZone.toString()
        }
        authZoneLayout.addView(
            isAuthZoneButton,
            ViewGroup.MarginLayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        rootLayout.addView(
            authZoneLayout,
            ViewGroup.MarginLayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val launchButton = Button(this)
        launchButton.text = "Launch"
        launchButton.setOnClickListener {
            startActivity(
                Intent(this, SampleActivity::class.java).apply {
                    putExtra("is_auth_zone", isAuthZone)
                }
            )
        }
        rootLayout.addView(
            launchButton,
            ViewGroup.MarginLayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        addContentView(
            rootLayout,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        )
    }

}
