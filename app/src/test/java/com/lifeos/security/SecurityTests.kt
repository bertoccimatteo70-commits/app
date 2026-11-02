package com.lifeos.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityTests {

    @Test
    fun `pin hash verifies the right pin and rejects others`() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("4821", salt)

        assertTrue(PinHasher.verify("4821", salt, hash))
        assertFalse(PinHasher.verify("4822", salt, hash))
        assertFalse(PinHasher.verify("", salt, hash))
        assertFalse(PinHasher.verify("4821", salt, ""))
        assertFalse(PinHasher.verify("4821", "", hash))
    }

    @Test
    fun `each pin hash uses a different salt`() {
        val first = PinHasher.newSalt()
        val second = PinHasher.newSalt()

        assertNotEquals(first, second)
        assertNotEquals(PinHasher.hash("1234", first), PinHasher.hash("1234", second))
    }

    @Test
    fun `encrypted backup round trips`() {
        val payload = """{"format":"lifeos-backup","version":1,"state":{}}"""

        val encrypted = EncryptedBackup.encrypt(payload, "segreta")

        assertTrue(EncryptedBackup.isEncrypted(encrypted))
        assertFalse(encrypted.contains(payload))
        assertTrue(EncryptedBackup.decrypt(encrypted, "segreta").isSuccess)
        assertTrue(
            EncryptedBackup.decrypt(encrypted, "sbagliata").exceptionOrNull()?.message
                ?.contains("Password errata") == true
        )
    }

    @Test
    fun `tampered encrypted backup is rejected`() {
        val encrypted = EncryptedBackup.encrypt("contenuto", "password")
        val tampered = encrypted.dropLast(12) + "AAAAAAAAAAAA"

        assertTrue(EncryptedBackup.decrypt(tampered, "password").isFailure)
    }

    @Test
    fun `encrypted backup requires a longer password`() {
        assertTrue(runCatching { EncryptedBackup.encrypt("x", "abc") }.isFailure)
    }

    @Test
    fun `plain backup is not reported as encrypted`() {
        assertFalse(EncryptedBackup.isEncrypted("""{"format":"lifeos-backup","version":1}"""))
    }
}
