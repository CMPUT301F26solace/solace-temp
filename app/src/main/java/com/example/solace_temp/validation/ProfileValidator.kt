/**
 * ProfileValidator.kt
 *
 * Checks personal information before a profile is saved.
 * These checks validate formatting, not contact ownership.
 */
package com.example.solace_temp.validation

/**
 * Provides profile validation without depending on Android or Firestore.
 */
object ProfileValidator {

    private val emailLocalPattern =
        Regex("[A-Za-z0-9.!#\$%&'*+/=?^_`{|}~-]+")

    private val domainLabelPattern =
        Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?")

    private val phonePattern =
        Regex("\\+?[0-9() .-]+")

    /**
     * Checks that the entrant has entered a name.
     *
     * @param name The entered name.
     * @return An error message, or null when valid.
     */
    fun nameError(name: String): String? {
        return if (name.isBlank()) {
            "Enter your name."
        } else {
            null
        }
    }

    /**
     * Checks common email address formatting.
     *
     * @param email The entered email address.
     * @return An error message, or null when valid.
     */
    fun emailError(email: String): String? {
        val value = email.trim()

        if (value.isEmpty()) {
            return "Enter your email address."
        }

        val parts = value.split('@')

        if (parts.size != 2 || value.length > 254) {
            return "Enter a valid email address."
        }

        val localPart = parts[0]
        val domainLabels = parts[1].split('.')

        val validLocalPart =
            localPart.length in 1..64 &&
                    emailLocalPattern.matches(localPart) &&
                    !localPart.startsWith('.') &&
                    !localPart.endsWith('.') &&
                    !localPart.contains("..")

        val validDomain =
            domainLabels.size >= 2 &&
                    domainLabels.all { domainLabelPattern.matches(it) }

        return if (validLocalPart && validDomain) {
            null
        } else {
            "Enter a valid email address."
        }
    }

    /**
     * Accepts an empty phone number or a formatted number with 7–15 digits.
     *
     * @param phone The entered optional phone number.
     * @return An error message, or null when valid.
     */
    fun phoneError(phone: String): String? {
        val value = phone.trim()

        if (value.isEmpty()) {
            return null
        }

        val error = "Enter a valid phone number, or leave it empty."
        val digitCount = value.count { it in '0'..'9' }

        if (!phonePattern.matches(value) || digitCount !in 7..15) {
            return error
        }

        var parenthesesBalance = 0

        for (character in value) {
            when (character) {
                '(' -> parenthesesBalance++
                ')' -> parenthesesBalance--
            }

            if (parenthesesBalance !in 0..1) {
                return error
            }
        }

        return if (parenthesesBalance == 0) null else error
    }
}