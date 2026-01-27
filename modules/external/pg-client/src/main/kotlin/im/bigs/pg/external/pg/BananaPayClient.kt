package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * 바나나페이 자체 결제 연동 어댑터 (시뮬레이션).
 * - 새로운 PG사가 추가되어도 기존 비즈니스 로직(PaymentService)을 수정하지 않고 
 *   확장 가능한 구조(OCP)를 증명하기 위해 추가되었습니다.
 */
@Component
class BananaPayClient : PgClientOutPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun supports(partnerId: Long): Boolean {
        // 파트너 ID가 3번인 경우 바나나페이(자체 결제)를 사용하도록 설정
        return partnerId == 3L
    }

    override fun approve(request: PgApproveRequest): PgApproveResult {
        log.info("BananaPay 승인 요청 시작: partnerId={}, amount={}", request.partnerId, request.amount)

        // 바나나페이 고유의 승인 번호 체계 (시뮬레이션)
        val randomNum = (1000..9999).random()
        val bananaApprovalCode = "BANANA-$randomNum"
        
        log.info("BananaPay 승인 완료: approvalCode={}", bananaApprovalCode)

        return PgApproveResult(
            approvalCode = bananaApprovalCode,
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            status = PaymentStatus.APPROVED
        )
    }
}
