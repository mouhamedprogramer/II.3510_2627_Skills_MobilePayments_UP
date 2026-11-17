package fr.isep.campuspass

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import fr.isep.campuspass.ui.CampusPassApp
import fr.isep.campuspass.ui.theme.CampusPassTheme

/** FragmentActivity (not ComponentActivity) because BiometricPrompt needs a FragmentActivity host. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent { CampusPassTheme { CampusPassApp() } }
    }
}
