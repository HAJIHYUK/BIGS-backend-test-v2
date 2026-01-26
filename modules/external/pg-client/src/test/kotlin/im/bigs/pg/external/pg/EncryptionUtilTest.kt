package im.bigs.pg.external.pg

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
}
