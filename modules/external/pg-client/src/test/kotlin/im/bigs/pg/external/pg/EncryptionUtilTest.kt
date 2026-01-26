package im.bigs.pg.external.pg

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.crypto.AEADBadTagException

class EncryptionUtilTest {

    @Test
    @DisplayName("문자열을 암호화하고 복호화하면 원래 문자열과 일치해야 한다")
    fun `암호화_복호화_성공_테스트`() {
        // Given: 테스트할 평문 JSON (API 문서 예시 참고함!)
        val plaintext = """{"cardNumber":"1111-1111-1111-1111","amount":10000}"""

        // When: 암호화 후 다시 복호화 실행
        val encrypted = EncryptionUtil.encrypt(plaintext)
        val decrypted = EncryptionUtil.decrypt(encrypted)

        // Then: 결과 검증
        println("원문: $plaintext")
        println("암호문: $encrypted")
        println("복호화 결과: $decrypted")

        // 1. 암호화된 결과는 원문과 달라야 함
        assertNotEquals(plaintext, encrypted)
        
        // 2. 복호화된 결과는 다시 원문과 완벽히 일치해야 함
        assertEquals(plaintext, decrypted)
    }

    @Test
    @DisplayName("빈 문자열을 암호화하고 복호화하면 원래 빈 문자열과 일치해야 한다")
    fun `빈_문자열_암호화_복호화_테스트`() {
        val plaintext = ""
        val encrypted = EncryptionUtil.encrypt(plaintext)
        val decrypted = EncryptionUtil.decrypt(encrypted)

        assertNotEquals(plaintext, encrypted) // 빈 문자열도 암호화되면 달라야 함
        assertEquals(plaintext, decrypted)
    }

    @Test
    @DisplayName("특수 문자를 포함한 문자열을 암호화하고 복호화하면 원래 문자열과 일치해야 한다")
    fun `특수_문자_포함_문자열_암호화_복호화_테스트`() {
        val plaintext = """{"name":"한글이름", "desc":"!@#$%^&*()_+"}"""
        val encrypted = EncryptionUtil.encrypt(plaintext)
        val decrypted = EncryptionUtil.decrypt(encrypted)

        assertNotEquals(plaintext, encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    @DisplayName("유효하지 않은 Base64URL 형식의 암호문 복호화 시 예외가 발생해야 한다")
    fun `유효하지_않은_암호문_복호화_예외_테스트`() {
        val invalidEncryptedText = "this-is-not-a-valid-base64url-string"

        // Base64 디코딩은 성공하더라도, GCM 태그가 일치하지 않아 AEADBadTagException이 발생함
        assertThrows<AEADBadTagException> { // IllegalArgumentException 대신 AEADBadTagException 발생 예상
            EncryptionUtil.decrypt(invalidEncryptedText)
        }
    }

    @Test
    @DisplayName("유효한 Base64이나 암호화 형식이 아닌 경우 복호화 시 예외가 발생해야 한다")
    fun `유효한_Base64이나_형식_아닌_경우_복호화_예외_테스트`() {
        // Base64 형식은 맞지만, AES-GCM 형식이 아닌 임의의 데이터
        val malformedEncryptedText = "SGVsbG8gV29ybGQ=" // "Hello World"

        assertThrows<AEADBadTagException> {
            EncryptionUtil.decrypt(malformedEncryptedText)
        }
    }
}
