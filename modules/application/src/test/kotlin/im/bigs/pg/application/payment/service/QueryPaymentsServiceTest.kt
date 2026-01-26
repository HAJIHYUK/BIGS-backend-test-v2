package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
                approvalCode = "APP2", approvedAt = now, status = PaymentStatus.APPROVED, createdAt = now, updatedAt = now
            ),
            Payment(
                id = 1, partnerId = 1L, amount = BigDecimal("1000"),
                appliedFeeRate = BigDecimal("0.03"), feeAmount = BigDecimal("30"), netAmount = BigDecimal("970"),
                approvalCode = "APP1", approvedAt = now, status = PaymentStatus.APPROVED, createdAt = now, updatedAt = now
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

    @Test
    @DisplayName("마지막 페이지 조회 시 hasNext는 false, nextCursor는 null이어야 한다")
    fun `마지막_페이지_조회`() {
        // Given
        val filter = QueryFilter(limit = 10)
        val mockPage = PaymentPage(
            items = emptyList(),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )
        val mockSummary = PaymentSummaryProjection(0, BigDecimal.ZERO, BigDecimal.ZERO)

        every { paymentOutPort.findBy(any()) } returns mockPage
        every { paymentOutPort.summary(any()) } returns mockSummary

        // When
        val result = queryPaymentsService.query(filter)

        // Then
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    @DisplayName("조회 결과가 없을 때 빈 목록과 0 통계를 반환해야 한다")
    fun `빈_결과_조회`() {
        // Given
        val filter = QueryFilter(limit = 10)
        val mockPage = PaymentPage(emptyList(), false, null, null)
        val mockSummary = PaymentSummaryProjection(0, BigDecimal.ZERO, BigDecimal.ZERO)

        every { paymentOutPort.findBy(any()) } returns mockPage
        every { paymentOutPort.summary(any()) } returns mockSummary

        // When
        val result = queryPaymentsService.query(filter)

        // Then
        assertTrue(result.items.isEmpty())
        assertEquals(0, result.summary.count)
        assertEquals(BigDecimal.ZERO, result.summary.totalAmount)
        assertEquals(BigDecimal.ZERO, result.summary.totalNetAmount)
        assertFalse(result.hasNext)
        assertNull(result.nextCursor)
    }

    @Test
    @DisplayName("다양한 필터 조건이 OutPort에 올바르게 전달되어야 한다")
    fun `필터_조건_전달_검증`() {
        // Given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 1, 31, 23, 59, 59)
        val filter = QueryFilter(
            partnerId = 123L,
            status = "APPROVED",
            from = from,
            to = to,
            limit = 50
        )
        val querySlot = slot<PaymentQuery>()
        val summarySlot = slot<PaymentSummaryFilter>()

        every { paymentOutPort.findBy(capture(querySlot)) } returns PaymentPage(emptyList(), false, null, null)
        every { paymentOutPort.summary(capture(summarySlot)) } returns PaymentSummaryProjection(0, BigDecimal.ZERO, BigDecimal.ZERO)

        // When
        queryPaymentsService.query(filter)

        // Then: 캡처된 인자 검증
        val capturedQuery = querySlot.captured
        assertEquals(123L, capturedQuery.partnerId)
        assertEquals(PaymentStatus.APPROVED, capturedQuery.status)
        assertEquals(from, capturedQuery.from)
        assertEquals(to, capturedQuery.to)
        assertEquals(50, capturedQuery.limit)

        val capturedSummaryFilter = summarySlot.captured
        assertEquals(123L, capturedSummaryFilter.partnerId)
        assertEquals(PaymentStatus.APPROVED, capturedSummaryFilter.status)
        assertEquals(from, capturedSummaryFilter.from)
        assertEquals(to, capturedSummaryFilter.to)
    }
}
