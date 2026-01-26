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
        // Given: 테스트할 평문 JSON (API 문서 예시와 유사하게 구성)
        val plaintext = """{"cardNumber":"1111-1111-1111-1111","amount":10000}"""

        // When: 암호화 후 다시 복호화 실행
        val encrypted = EncryptionUtil.encrypt(plaintext)
        val decrypted = EncryptionUtil.decrypt(encrypted)

        // Then: 암호문은 원문과 다르고, 복호화 결과는 원문과 같아야 함
        assertNotEquals(plaintext, encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    @DisplayName("빈 문자열을 암호화하고 복호화하면 원래 빈 문자열과 일치해야 한다")
    fun `빈_문자열_암호화_복호화_테스트`() {
        // Given: 빈 문자열 준비
        val plaintext = ""

        // When: 암호화 후 복호화 실행
        val encrypted = EncryptionUtil.encrypt(plaintext)
        val decrypted = EncryptionUtil.decrypt(encrypted)

        // Then: 빈 문자열도 정상적으로 복구되어야 함
        assertNotEquals(plaintext, encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    @DisplayName("특수 문자를 포함한 문자열을 암호화하고 복호화하면 원래 문자열과 일치해야 한다")
    fun `특수_문자_포함_문자열_암호화_복호화_테스트`() {
        // Given: 특수문자가 포함된 문자열 준비
        val plaintext = """{"name":"한글이름", "desc":"! @#$%^&*()_+"}"""

        // When: 암호화 후 복호화 실행
        val encrypted = EncryptionUtil.encrypt(plaintext)
        val decrypted = EncryptionUtil.decrypt(encrypted)

        // Then: 특수문자도 깨지지 않고 복구되어야 함
        assertNotEquals(plaintext, encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    @DisplayName("유효하지 않은 Base64URL 형식의 암호문 복호화 시 예외가 발생해야 한다")
    fun `유효하지_않은_암호문_복호화_예외_테스트`() {
        // Given: 유효하지 않은 형식의 암호문 준비
        val invalidEncryptedText = "this-is-not-a-valid-base64url-string"

        // When & Then: 복호화 시 AEADBadTagException 발생 확인 (Base64 디코딩 성공 후 태그 불일치)
        assertThrows<AEADBadTagException> {
            EncryptionUtil.decrypt(invalidEncryptedText)
        }
    }

    @Test
    @DisplayName("유효한 Base64이나 암호화 형식이 아닌 경우 복호화 시 예외가 발생해야 한다")
    fun `유효한_Base64이나_형식_아닌_경우_복호화_예외_테스트`() {
        // Given: Base64 형식이지만 암호화되지 않은 문자열 준비
        val malformedEncryptedText = "SGVsbG8gV29ybGQ=" // "Hello World"

        // When & Then: 복호화 시 AEADBadTagException 발생 확인
        assertThrows<AEADBadTagException> {
            EncryptionUtil.decrypt(malformedEncryptedText)
        }
    }
}
