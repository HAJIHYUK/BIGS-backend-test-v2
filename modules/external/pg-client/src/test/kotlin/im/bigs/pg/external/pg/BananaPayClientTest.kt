package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BananaPayClientTest {

    private val bananaPayClient = BananaPayClient()

    @Test
    @DisplayName("BananaPayClient는 partnerId가 3일 때만 지원해야 한다")
    fun `supports_메소드_검증`() {
        // Given & When & Then
        assertTrue(bananaPayClient.supports(3L), "파트너 ID 3번은 지원해야 함")
        assertFalse(bananaPayClient.supports(1L), "파트너 ID 1번은 지원하지 않아야 함")
        assertFalse(bananaPayClient.supports(2L), "파트너 ID 2번은 지원하지 않아야 함")
    }

    @Test
    @DisplayName("승인 요청 시 BANANA-로 시작하는 승인 코드를 반환해야 한다")
    fun `approve_메소드_검증`() {
        // Given : 승인요청 데이터
        val request = PgApproveRequest(
            partnerId = 3L,
            amount = BigDecimal("5000"),
            cardBin = "123456",
            cardLast4 = "1234",
            productName = "바나나맛 우유"
        )

        // When : 승인 요청 실행
        val result = bananaPayClient.approve(request)

        // Then : 승인 코드가 BANANA-로 시작하는지 확인
        assertTrue(result.approvalCode.startsWith("BANANA-"), "승인 코드는 BANANA-로 시작해야 함")
        assertEquals(PaymentStatus.APPROVED, result.status, "결제 상태는 APPROVED여야 함")
    }
}
