package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.domain.payment.Payment
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class 결제_조회_서비스_테스트 {

    private val paymentOutPort = mockk<PaymentOutPort>()
    private val queryPaymentsService = QueryPaymentsService(paymentOutPort)

    @Test
    @DisplayName("결제 내역 조회 시 목록, 통계, 커서 정보가 올바르게 반환되어야 한다")
    fun `결제_내역_조회_성공`() {
        // Given: Mock 데이터 설정
        val filter = QueryFilter(partnerId = 1L, limit = 10)
        
        val now = LocalDateTime.now() // 현재 시간을 기준으로 예시 데이터 생성
        val mockPayments = listOf(
            Payment(
                id = 2, partnerId = 1L, amount = BigDecimal("2000"),
                appliedFeeRate = BigDecimal("0.03"), feeAmount = BigDecimal("60"), netAmount = BigDecimal("1940"),
                approvalCode = "APP2", approvedAt = now, status = im.bigs.pg.domain.payment.PaymentStatus.APPROVED, createdAt = now, updatedAt = now
            ),
            Payment(
                id = 1, partnerId = 1L, amount = BigDecimal("1000"),
                appliedFeeRate = BigDecimal("0.03"), feeAmount = BigDecimal("30"), netAmount = BigDecimal("970"),
                approvalCode = "APP1", approvedAt = now, status = im.bigs.pg.domain.payment.PaymentStatus.APPROVED, createdAt = now, updatedAt = now
            )
        )
        val mockPage = PaymentPage(
            items = mockPayments,
            hasNext = true,
            nextCursorCreatedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
            nextCursorId = 1L
        )
        val mockSummary = PaymentSummaryProjection(
            count = 2,
            totalAmount = BigDecimal("3000"),
            totalNetAmount = BigDecimal("2800")
        )

        every { paymentOutPort.findBy(any()) } returns mockPage
        every { paymentOutPort.summary(any()) } returns mockSummary

        // When: 서비스 호출
        val result = queryPaymentsService.query(filter)

        // Then: 결과 검증
        assertEquals(2, result.items.size)
        assertEquals(2, result.summary.count)
        assertEquals(BigDecimal("3000"), result.summary.totalAmount)
        assertTrue(result.hasNext)
        // 커서가 올바르게 인코딩 되었는지 대략적으로 확인
        assertTrue(result.nextCursor?.isNotBlank() ?: false)
        
        verify(exactly = 1) { paymentOutPort.findBy(any()) }
        verify(exactly = 1) { paymentOutPort.summary(any()) }
    }
}
