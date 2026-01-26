package im.bigs.pg.domain.calculation

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class 수수료계산기Test {
    @Test
    @DisplayName("퍼센트 수수료만 적용 시 반올림 및 정산금이 정확해야 한다")
    fun `퍼센트 수수료만 적용 시 반올림 및 정산금이 정확해야 한다`() {
        val amount = BigDecimal("10000")
        val rate = BigDecimal("0.0235")
        val (fee, net) = FeeCalculator.calculateFee(amount, rate, null)
        assertEquals(BigDecimal("235"), fee)
        assertEquals(BigDecimal("9765"), net)
    }

    @Test
    @DisplayName("퍼센트+정액 수수료가 함께 적용되어야 한다")
    fun `퍼센트와 정액 수수료가 함께 적용되어야 한다`() {
        val amount = BigDecimal("10000")
        val rate = BigDecimal("0.0300")
        val fixed = BigDecimal("100")
        val (fee, net) = FeeCalculator.calculateFee(amount, rate, fixed)
        assertEquals(BigDecimal("400"), fee)
        assertEquals(BigDecimal("9600"), net)
    }

    @Test
    @DisplayName("HALF_UP 규칙에 따라 소수점 첫째 자리가 5 이상이면 올림 처리되어야 한다")
    fun `HALF_UP 규칙에 따라 소수점 첫째 자리가 5 이상이면 올림 처리되어야 한다`() {
        val amount = BigDecimal("1000")
        val rate = BigDecimal("0.025") // 1000 * 0.025 = 25.0 -> 25
        val (fee1, _) = FeeCalculator.calculateFee(amount, rate, null)
        assertEquals(BigDecimal("25"), fee1)

        val amount2 = BigDecimal("1005")
        val rate2 = BigDecimal("0.025") // 1005 * 0.025 = 25.125 -> 25 (HALF_UP은 0.5에서 올림)
        val (fee2, _) = FeeCalculator.calculateFee(amount2, rate2, null)
        assertEquals(BigDecimal("25"), fee2) // 25.125 -> 25 (0.125는 버림)

        val amount3 = BigDecimal("1000")
        val rate3 = BigDecimal("0.0255") // 1000 * 0.0255 = 25.5 -> 26
        val (fee3, _) = FeeCalculator.calculateFee(amount3, rate3, null)
        assertEquals(BigDecimal("26"), fee3) // 25.5 -> 26 (0.5는 올림)
    }

    @Test
    @DisplayName("HALF_UP 규칙에 따라 소수점 첫째 자리가 5 미만이면 버림 처리되어야 한다")
    fun `HALF_UP 규칙에 따라 소수점 첫째 자리가 5 미만이면 버림 처리되어야 한다`() {
        val amount = BigDecimal("1000")
        val rate = BigDecimal("0.024") // 1000 * 0.024 = 24.0 -> 24
        val (fee1, _) = FeeCalculator.calculateFee(amount, rate, null)
        assertEquals(BigDecimal("24"), fee1)

        val amount2 = BigDecimal("1004")
        val rate2 = BigDecimal("0.024") // 1004 * 0.024 = 24.096 -> 24
        val (fee2, _) = FeeCalculator.calculateFee(amount2, rate2, null)
        assertEquals(BigDecimal("24"), fee2)
    }

    @Test
    @DisplayName("비율 수수료가 0일 때 고정 수수료만 적용되어야 한다")
    fun `비율 수수료가 0일 때 고정 수수료만 적용되어야 한다`() {
        val amount = BigDecimal("10000")
        val rate = BigDecimal("0.0000") // 0%
        val fixed = BigDecimal("500")
        val (fee, net) = FeeCalculator.calculateFee(amount, rate, fixed)
        assertEquals(BigDecimal("500"), fee) // (10000 * 0) + 500 = 500
        assertEquals(BigDecimal("9500"), net)
    }

    @Test
    @DisplayName("고정 수수료가 없을 때 비율 수수료만 적용되어야 한다")
    fun `고정 수수료가 없을 때 비율 수수료만 적용되어야 한다`() {
        val amount = BigDecimal("10000")
        val rate = BigDecimal("0.0150") // 1.5%
        val (fee, net) = FeeCalculator.calculateFee(amount, rate, null)
        assertEquals(BigDecimal("150"), fee) // 10000 * 0.015 = 150
        assertEquals(BigDecimal("9850"), net)
    }

    @Test
    @DisplayName("결제 금액이 0일 때 수수료와 정산금은 모두 0이어야 한다")
    fun `결제 금액이 0일 때 수수료와 정산금은 모두 0이어야 한다`() {
        val amount = BigDecimal("0")
        val rate = BigDecimal("0.0300")
        val fixed = BigDecimal("100")
        val (fee, net) = FeeCalculator.calculateFee(amount, rate, fixed)
        assertEquals(BigDecimal("100"), fee) // 0 * 0.03 + 100 = 100
        assertEquals(BigDecimal("-100"), net) // 0 - 100 = -100
    }

    @Test
    @DisplayName("금액이 음수일 경우 IllegalArgumentException이 발생해야 한다")
    fun `금액이 음수일 경우 IllegalArgumentException이 발생해야 한다`() {
        val amount = BigDecimal("-100")
        val rate = BigDecimal("0.0300")
        assertThrows<IllegalArgumentException> {
            FeeCalculator.calculateFee(amount, rate, null)
        }
    }

    @Test
    @DisplayName("수수료율이 음수일 경우 IllegalArgumentException이 발생해야 한다")
    fun `수수료율이 음수일 경우 IllegalArgumentException이 발생해야 한다`() {
        val amount = BigDecimal("10000")
        val rate = BigDecimal("-0.0100")
        assertThrows<IllegalArgumentException> {
            FeeCalculator.calculateFee(amount, rate, null)
        }
    }
}
