package com.mato.syai.note.data.local.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CryptoManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private val algorithm = KeyProperties.KEY_ALGORITHM_AES
    private val blockMode = KeyProperties.BLOCK_MODE_GCM
    private val padding = KeyProperties.ENCRYPTION_PADDING_NONE
    private val transformation = "$algorithm/$blockMode/$padding"

    private val mutex = Mutex()

    private fun getSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry("syai_key", null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createKey()
    }

    private fun createKey(): SecretKey {
        return KeyGenerator.getInstance(algorithm, "AndroidKeyStore").apply {
            init(
                KeyGenParameterSpec.Builder(
                    "syai_key",
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(blockMode)
                    .setEncryptionPaddings(padding)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
        }.generateKey()
    }

    suspend fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> = mutex.withLock {
        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                val cipher = Cipher.getInstance(transformation)
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
                return@withLock cipher.iv to cipher.doFinal(data)
            } catch (e: Exception) {
                lastException = e
                kotlinx.coroutines.delay(50L * attempt)
            }
        }
        throw lastException ?: IllegalStateException("Encryption failed")
    }

    suspend fun decrypt(iv: ByteArray, encryptedData: ByteArray): ByteArray = mutex.withLock {
        var lastException: Exception? = null
        for (attempt in 1..3) {
            try {
                val cipher = Cipher.getInstance(transformation)
                cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
                return@withLock cipher.doFinal(encryptedData)
            } catch (e: Exception) {
                lastException = e
                kotlinx.coroutines.delay(50L * attempt)
            }
        }
        throw lastException ?: IllegalStateException("Decryption failed")
    }
}