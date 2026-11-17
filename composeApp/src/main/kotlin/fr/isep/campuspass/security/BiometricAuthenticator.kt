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
        // TODO(Exercise 2): show the system biometric prompt.
        //  1. Create a BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
        //     where callback is an object : BiometricPrompt.AuthenticationCallback() that calls
        //     onSuccess() in onAuthenticationSucceeded and onError(errString.toString()) in onAuthenticationError.
        //  2. Build a BiometricPrompt.PromptInfo with setTitle(title), setSubtitle(subtitle) and
        //     setAllowedAuthenticators(authenticators). Below Android 11 (API 30) a negative button is mandatory:
        //     setNegativeButtonText("Cancel").
        //  3. prompt.authenticate(info)
        onError("TODO Exercise 2: biometric confirmation is not implemented yet")
    }
}
