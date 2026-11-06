/**
 * ProfileValidatorTest.kt
 *
 * Tests personal-information validation for US 01.07.03.
 * These tests do not access Android or Firestore.
 */
package com.example.solace_temp.validation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileValidatorTest {

    @Test
    fun validNamesAreAccepted() {
        assertNull(ProfileValidator.nameError("Test Camper"))
        assertNull(ProfileValidator.nameError("Élodie O’Connor"))
    }

    @Test
    fun blankNamesAreRejected() {
        assertNotNull(ProfileValidator.nameError(""))
        assertNotNull(ProfileValidator.nameError("   "))
        assertNotNull(ProfileValidator.nameError("\t\n"))
    }

    @Test
    fun validEmailsAreAccepted() {
        val emails = listOf(
            "camper@example.com",
            "camper+banff@camp.example.com",
            "  camper@example.com  "
        )

        for (email in emails) {
            assertNull(
                "Expected this email to be accepted: $email",
                ProfileValidator.emailError(email)
            )
        }
    }

    @Test
    fun invalidEmailsAreRejected() {
        val emails = listOf(
            "",
            "not-an-email",
            "@example.com",
            "camper@",
            "camper@example",
            "camper@@example.com",
            ".camper@example.com",
            "camper.@example.com",
            "camper..test@example.com",
            "camper@example..com",
            "camper@-example.com",
            "camper@example-.com",
            "camper name@example.com"
        )

        for (email in emails) {
            assertNotNull(
                "Expected this email to be rejected: $email",
                ProfileValidator.emailError(email)
            )
        }
    }

    @Test
    fun optionalPhoneCanBeEmpty() {
        assertNull(ProfileValidator.phoneError(""))
        assertNull(ProfileValidator.phoneError("   "))
    }

    @Test
    fun commonPhoneFormatsAreAccepted() {
        val phones = listOf(
            "7805550142",
            "(780) 555-0142",
            "+1 (780) 555-0142"
        )

        for (phone in phones) {
            assertNull(
                "Expected this phone to be accepted: $phone",
                ProfileValidator.phoneError(phone)
            )
        }
    }

    @Test
    fun phoneDigitBoundariesAreAccepted() {
        assertNull(ProfileValidator.phoneError("1234567"))
        assertNull(ProfileValidator.phoneError("123456789012345"))
    }

    @Test
    fun invalidPhonesAreRejected() {
        val phones = listOf(
            "123456",
            "1234567890123456",
            "780555ABCD",
            "780+5550142",
            "(780 555-0142",
            "780) 555-0142",
            ")780( 555-0142"
        )

        for (phone in phones) {
            assertNotNull(
                "Expected this phone to be rejected: $phone",
                ProfileValidator.phoneError(phone)
            )
        }
    }
}