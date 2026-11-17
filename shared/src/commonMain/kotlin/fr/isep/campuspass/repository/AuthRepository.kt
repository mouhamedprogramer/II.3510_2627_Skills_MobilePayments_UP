package fr.isep.campuspass.repository

import fr.isep.campuspass.domain.Role
import fr.isep.campuspass.domain.UserAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** In-memory authentication with mock accounts (passwords in plain text on purpose: demo only). */
class AuthRepository(val accounts: List<UserAccount> = defaultAccounts) {
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser

    /** Returns the matching account (and signs it in), or null if the credentials are wrong. */
    fun authenticate(email: String, password: String): UserAccount? {
        val account = accounts.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) && it.password == password }
        if (account != null) _currentUser.value = account
        return account
    }

    fun logout() {
        _currentUser.value = null
    }

    companion object {
        val defaultAccounts = listOf(
            UserAccount(1, "Alice Martin", "alice@isep.fr", "alice123", Role.STUDENT),
            UserAccount(2, "Bob Dupont", "bob@isep.fr", "bob123", Role.STUDENT),
            UserAccount(3, "BDE ISEP", "bde@isep.fr", "bde123", Role.ORGANIZER),
        )
    }
}
