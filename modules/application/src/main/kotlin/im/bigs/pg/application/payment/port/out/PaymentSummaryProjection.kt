package im.bigs.pg.application.payment.port.out

/** 저장 계층에서 계산된 집계 결과. */
data class PaymentSummaryProjection(
    val count: Long,
    val totalAmount: java.math.BigDecimal,
    val totalNetAmount: java.math.BigDecimal,
)
