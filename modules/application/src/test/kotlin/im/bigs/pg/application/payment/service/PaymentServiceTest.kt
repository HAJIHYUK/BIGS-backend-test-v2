package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.DisplayName
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class 결제서비스Test {
    private val partnerRepo = mockk<PartnerOutPort>()
    private val feeRepo = mockk<FeePolicyOutPort>()
    private val paymentRepo = mockk<PaymentOutPort>()
    private val pgClient = object : PgClientOutPort {
        override fun supports(partnerId: Long) = true
        override fun approve(request: PgApproveRequest) =
            PgApproveResult("APPROVAL-123", LocalDateTime.of(2024,1,1,0,0), PaymentStatus.APPROVED)
    }

    @Test
    @DisplayName("결제 시 수수료 정책을 적용하고 저장해야 한다")
    fun `결제 시 수수료 정책을 적용하고 저장해야 한다`() {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
            id = 10L, partnerId = 1L, effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0300"), fixedFee = BigDecimal("100")
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")
        val res = service.pay(cmd)

        assertEquals(99L, res.id)
        assertEquals(BigDecimal("400"), res.feeAmount)
        assertEquals(BigDecimal("9600"), res.netAmount)
        assertEquals(PaymentStatus.APPROVED, res.status)
    }

    @Test
    @DisplayName("제휴사별로 서로 다른 수수료 정책이 동적으로 적용되어야 한다 리팩토링 테스트 코드")
    fun `제휴사별로 서로 다른 수수료 정책이 동적으로 적용되어야 한다`() {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))

        // Ex :  제휴사 2번은 5% + 200원 정책을 가짐
        every { partnerRepo.findById(2L) } returns Partner(2L, "TEST2", "Test2", true)
        every { feeRepo.findEffectivePolicy(2L, any()) } returns FeePolicy(
            id = 20L, partnerId = 2L,
            effectiveFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
            percentage = BigDecimal("0.0500"), // 5%
            fixedFee = BigDecimal("200")       // 200원
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 100L) }

        // When: 10,000원 결제 요청
        val cmd = PaymentCommand(partnerId = 2L, amount = BigDecimal("10000"), cardLast4 = "1111")
        val res = service.pay(cmd)

        // Then: 수수료는 700원(10000 * 0.05 + 200)이어야 함
        assertEquals(100L, res.id)
        assertEquals(BigDecimal("700"), res.feeAmount)
        assertEquals(BigDecimal("9300"), res.netAmount)
        assertEquals(BigDecimal("0.0500"), res.appliedFeeRate)
    }
}
