package fr.isep.campuspass.security

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Confirms a sensitive action (a payment) with the fingerprint / face, or the device PIN as fallback.
 * The app never sees the biometric data: it only receives "success" or "error".
 */
class BiometricAuthenticator(private val activity: FragmentActivity) {

    enum class Availability { AVAILABLE, NOT_ENROLLED, UNAVAILABLE }

    private val authenticators =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK

    fun availability(): Availability = when (BiometricManager.from(activity).canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> Availability.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Availability.NOT_ENROLLED
        else -> Availability.UNAVAILABLE
    }

    fun authenticate(
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString())
                // onAuthenticationFailed = one wrong finger: the system prompt stays open and lets the user retry.
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .apply { if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) setNegativeButtonText("Cancel") }
            .build()
        prompt.authenticate(info)
    }
}
