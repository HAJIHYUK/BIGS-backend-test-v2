package im.bigs.pg.application.payment.port.out

import com.fasterxml.jackson.annotation.JsonFormat
import im.bigs.pg.domain.payment.PaymentStatus
import java.time.LocalDateTime

/**
 * 결제 페이지 조회용 입력 값.
 * - 정렬 키(createdAt desc, id desc)를 커서로 사용합니다.
 */
data class PaymentQuery(
    val partnerId: Long? = null,
    val status: PaymentStatus? = null,
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val from: LocalDateTime? = null,
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val to: LocalDateTime? = null,
    val limit: Int = 20,
    @get:JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val cursorCreatedAt: LocalDateTime? = null,
    val cursorId: Long? = null,
)
