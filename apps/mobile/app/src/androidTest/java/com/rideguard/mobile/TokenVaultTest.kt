package com.rideguard.mobile

import androidx.test.platform.app.InstrumentationRegistry
import com.rideguard.mobile.shared.storage.TokenVault
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class TokenVaultTest {
    @Test fun ciphertextRoundtripRotationAndDeletion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vault = TokenVault(context)
        val token = "synthetic-instrumentation-refresh-not-a-real-credential"
        try {
            vault.write(token)
            assertEquals(token, TokenVault(context).read())
            val file = File(context.noBackupFilesDir, "refresh.v1")
            val first = file.readBytes()
            assertFalse(String(first, Charsets.UTF_8).contains(token))
            vault.write(token + "-rotated")
            assertFalse(first.contentEquals(file.readBytes()))
            assertEquals(token + "-rotated", TokenVault(context).read())
            vault.clear(); assertNull(vault.read()); assertFalse(file.exists())
        } finally { vault.clear() }
    }
}
