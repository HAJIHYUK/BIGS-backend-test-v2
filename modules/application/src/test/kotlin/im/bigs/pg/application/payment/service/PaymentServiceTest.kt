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
import io.mockk.verify

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
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
        // Given: 제휴사 1번 (수수료 3% + 100원) 정책 설정
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
            id = 10L, partnerId = 1L, effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0300"), fixedFee = BigDecimal("100")
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")

        // When: 10,000원 결제 요청
        val res = service.pay(cmd)

        // Then: 수수료 400원(300+100) 및 승인 상태 확인
        assertEquals(99L, res.id)
        assertEquals(BigDecimal("400"), res.feeAmount)
        assertEquals(BigDecimal("9600"), res.netAmount)
        assertEquals(PaymentStatus.APPROVED, res.status)
    }

    @Test
    @DisplayName("제휴사별로 서로 다른 수수료 정책이 동적으로 적용되어야 한다 리팩토링 테스트 코드")
    fun `제휴사별로 서로 다른 수수료 정책이 동적으로 적용되어야 한다`() {
        // Given: 제휴사 2번 (수수료 5% + 200원) 정책 설정
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

        val cmd = PaymentCommand(partnerId = 2L, amount = BigDecimal("10000"), cardLast4 = "1111")

        // When: 10,000원 결제 요청
        val res = service.pay(cmd)

        // Then: 수수료 700원(500+200) 확인
        assertEquals(100L, res.id)
        assertEquals(BigDecimal("700"), res.feeAmount)
        assertEquals(BigDecimal("9300"), res.netAmount)
        assertEquals(BigDecimal("0.0500"), res.appliedFeeRate)
    }

    @Test
    @DisplayName("수수료 정책이 없는 제휴사는 결제 시도 시 예외가 발생해야 한다")
    fun `수수료 정책이 없는 제휴사는 결제 시도 시 예외가 발생해야 한다`() {
        // Given: 수수료 정책이 없는 제휴사(3번) 설정
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))

        every { partnerRepo.findById(3L) } returns Partner(3L, "NO_FEE_POLICY", "NoFeePolicy", true)
        every { feeRepo.findEffectivePolicy(3L, any()) } returns null // 수수료 정책이 없음을 Mocking

        val cmd = PaymentCommand(partnerId = 3L, amount = BigDecimal("10000"), cardLast4 = "5555")

        // When & Then: IllegalStateException 발생 확인
        assertThrows<IllegalStateException> {
            service.pay(cmd)
        }
    }

    @Test
    @DisplayName("존재하지 않는 제휴사 ID로 결제 시도 시 예외가 발생해야 한다")
    fun `존재하지 않는 제휴사 ID로 결제 시도 시 예외가 발생해야 한다`() {
        // Given: 존재하지 않는 제휴사(4번) 설정
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))

        every { partnerRepo.findById(4L) } returns null // 파트너를 찾을 수 없음을 Mocking

        val cmd = PaymentCommand(partnerId = 4L, amount = BigDecimal("10000"), cardLast4 = "6666")

        // When & Then: IllegalArgumentException 발생 확인
        assertThrows<IllegalArgumentException> {
            service.pay(cmd)
        }
    }

    @Test
    @DisplayName("비활성화된 제휴사는 결제 시도 시 예외가 발생해야 한다")
    fun `비활성화된 제휴사는 결제 시도 시 예외가 발생해야 한다`() {
        // Given: 비활성화된 제휴사(5번) 설정
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(pgClient))

        every { partnerRepo.findById(5L) } returns Partner(5L, "INACTIVE", "InactivePartner", false) // 비활성화된 파트너 Mocking
        every { feeRepo.findEffectivePolicy(5L, any()) } returns FeePolicy( // 정책은 있지만 비활성화
            id = 50L, partnerId = 5L, effectiveFrom = LocalDateTime.of(2020, 1, 1, 0, 0),
            percentage = BigDecimal("0.0100"), fixedFee = BigDecimal("0")
        )

        val cmd = PaymentCommand(partnerId = 5L, amount = BigDecimal("10000"), cardLast4 = "7777")

        // When & Then: IllegalArgumentException 발생 확인
        assertThrows<IllegalArgumentException> {
            service.pay(cmd)
        }
    }

    @Test
    @DisplayName("PG 승인 실패 시 결제 정보가 저장되지 않아야 한다")
    fun `PG 승인 실패 시 저장 안 됨`() {
        // Given: PG 클라이언트가 예외를 던지도록 설정
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)

        val failingPgClient = mockk<PgClientOutPort>()
        every { failingPgClient.supports(any()) } returns true
        every { failingPgClient.approve(any()) } throws RuntimeException("PG_ERROR")

        val serviceWithFailingPg = PaymentService(partnerRepo, feeRepo, paymentRepo, listOf(failingPgClient))
        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")

        // When & Then: 예외가 발생하는지 확인
        assertThrows<RuntimeException> {
            serviceWithFailingPg.pay(cmd)
        }

        // Then: DB 저장 함수(save)가 한 번도 호출되지 않았음을 검증
        verify(exactly = 0) { paymentRepo.save(any()) }
    }
}
