package com.tbd.service

import com.typesafe.config.ConfigFactory
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.SecureRandom
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.*

class EncryptionService {
    private val config = ConfigFactory.load()
    
    init {
        // Register BouncyCastle provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }
    
    private fun getMasterKey(): SecretKey {
        // Get master key from environment or config
        val masterKeyHex = System.getenv("MASTER_ENCRYPTION_KEY") 
            ?: config.getString("encryption.masterKey")
            ?: throw IllegalStateException("MASTER_ENCRYPTION_KEY must be set in environment or config")
        
        val keyBytes = hexStringToByteArray(masterKeyHex)
        if (keyBytes.size != 32) {
            throw IllegalStateException("Master key must be 32 bytes (64 hex characters)")
        }
        
        return SecretKeySpec(keyBytes, "AES")
    }
    
    fun encrypt(plaintext: String): String {
        val masterKey = getMasterKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // Combine IV + ciphertext and encode as base64
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        
        return Base64.getEncoder().encodeToString(combined)
    }
    
    fun decrypt(encrypted: String): String {
        val masterKey = getMasterKey()
        val combined = Base64.getDecoder().decode(encrypted)
        
        // Extract IV (first 12 bytes for GCM)
        val iv = ByteArray(12)
        System.arraycopy(combined, 0, iv, 0, 12)
        
        // Extract ciphertext (rest)
        val ciphertext = ByteArray(combined.size - 12)
        System.arraycopy(combined, 12, ciphertext, 0, ciphertext.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC")
        val parameterSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec)
        
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
    
    private fun hexStringToByteArray(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("0x", "")
        val len = cleanHex.length
        val data = ByteArray(len / 2)
        for (i in 0 until len step 2) {
            data[i / 2] = ((Character.digit(cleanHex[i], 16) shl 4)
                    + Character.digit(cleanHex[i + 1], 16)).toByte()
        }
        return data
    }
    
    companion object {
        /**
         * Generate a new master encryption key (run once, store securely)
         */
        fun generateMasterKey(): String {
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val key = keyGenerator.generateKey()
            return bytesToHex(key.encoded)
        }
        
        private fun bytesToHex(bytes: ByteArray): String {
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

