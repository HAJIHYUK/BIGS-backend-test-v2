package im.bigs.pg.external.pg

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Test PG API 연동을 위한 AES-256-GCM 암호화/복호화 유틸리티.
 * - Test PG API 문서: 암호화 절차 참고
 */
object EncryptionUtil {
    // TODO: Test PG API 문서에 명시된 API-KEY (Key 생성에 사용)
    private const val API_KEY_FOR_ENCRYPTION = "11111111-1111-4111-8111-111111111111"

    // TODO: Test PG API 문서에 명시된 IV (Base64URL 디코딩)
    private val IV_BYTES = Base64.getUrlDecoder().decode("AAAAAAAAAAAAAAAA") // Base64URL(AAAAAAAAAAAAAAAA) -> 12바이트 배열

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128 // 128비트 = 16바이트

    // API-KEY를 SHA-256 해시하여 32바이트 AES Key 생성
    private val AES_KEY: SecretKeySpec = SecretKeySpec(
        MessageDigest.getInstance("SHA-256").digest(API_KEY_FOR_ENCRYPTION.toByteArray(Charsets.UTF_8)),
        "AES"
    )

    /**
     * 평문 데이터를 AES-256-GCM으로 암호화하여 Base64URL(ciphertext || tag) 문자열로 반환.
     * @param plaintext 암호화할 평문 JSON 문자열
     * @return Base64URL 인코딩된 암호문
     */
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, AES_KEY, GCMParameterSpec(TAG_LENGTH_BIT, IV_BYTES))

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext)
    }

    /**
     * Base64URL(ciphertext || tag) 문자열을 AES-256-GCM으로 복호화하여 평문 문자열로 반환.
     * @param encryptedText Base64URL 인코딩된 암호문
     * @return 복호화된 평문 JSON 문자열
     */
    fun decrypt(encryptedText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, AES_KEY, GCMParameterSpec(TAG_LENGTH_BIT, IV_BYTES))

        val decoded = Base64.getUrlDecoder().decode(encryptedText)
        val plaintext = cipher.doFinal(decoded)
        return String(plaintext, Charsets.UTF_8)
    }
}
