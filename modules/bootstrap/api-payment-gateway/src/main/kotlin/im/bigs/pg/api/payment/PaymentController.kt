package im.bigs.pg.api.payment

import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.*
import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.PaymentResponse
import im.bigs.pg.api.payment.dto.QueryResponse
import im.bigs.pg.api.payment.dto.Summary
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * 결제 API 진입점.
 * - POST: 결제 생성
 * - GET: 결제 조회(커서 페이지네이션 + 통계)
 */
@Tag(name = "결제 API", description = "결제 생성 및 이력 조회 기능을 제공합니다.")
@RestController
@RequestMapping("/api/v1/payments")
@Validated
class PaymentController(
    private val paymentUseCase: PaymentUseCase,
    private val queryPaymentsUseCase: QueryPaymentsUseCase,
) {

    /**
     * 결제 생성.
     *
     * @param req 결제 요청 본문
     * @return 생성된 결제 요약 응답
     */
    @Operation(summary = "결제 생성", description = "외부 PG사 승인을 거쳐 결제 데이터를 생성하고 DB에 저장합니다.")
    @PostMapping
    fun create(@RequestBody req: CreatePaymentRequest): ResponseEntity<PaymentResponse> {
        val saved = paymentUseCase.pay(
            PaymentCommand(
                partnerId = req.partnerId,
                amount = req.amount,
                cardBin = req.cardBin,
                cardLast4 = req.cardLast4,
                productName = req.productName,

            ),
        )
        return ResponseEntity.ok(PaymentResponse.from(saved))
    }

    /**
     * 결제 조회(커서 기반 페이지네이션 + 통계).
     *
     * @param partnerId 제휴사 필터
     * @param status 상태 필터
     * @param from 조회 시작 시각(ISO-8601)
     * @param to 조회 종료 시각(ISO-8601)
     * @param cursor 다음 페이지 커서
     * @param limit 페이지 크기(기본 20)
     * @return 목록/통계/커서 정보
     */
    @Operation(summary = "결제 이력 조회", description = "다양한 필터 조건과 커서 기반 페이지네이션을 사용하여 결제 내역과 통계 정보를 조회합니다.")
    @GetMapping
    fun query(
        @Parameter(description = "제휴사 식별자", example = "1")
        @RequestParam(required = false) partnerId: Long?,
        @Parameter(description = "결제 상태 (APPROVED, CANCELED)", example = "APPROVED")
        @RequestParam(required = false) status: String?,
        @Parameter(description = "조회 시작 시각 (yyyy-MM-dd HH:mm:ss)", example = "2024-01-01 00:00:00")
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") from: LocalDateTime?,
        @Parameter(description = "조회 종료 시각 (yyyy-MM-dd HH:mm:ss)", example = "2024-01-31 23:59:59")
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") to: LocalDateTime?,
        @Parameter(description = "다음 페이지 조회를 위한 커서 토큰", example = "ey...")
        @RequestParam(required = false) cursor: String?,
        @Parameter(description = "페이지당 결과 개수", example = "20")
        @RequestParam(defaultValue = "20") limit: Int,
    ): ResponseEntity<QueryResponse> {
        val res = queryPaymentsUseCase.query(
            QueryFilter(partnerId, status, from, to, cursor, limit),
        )
        return ResponseEntity.ok(
            QueryResponse(
                items = res.items.map { PaymentResponse.from(it) },
                summary = Summary(res.summary.count, res.summary.totalAmount, res.summary.totalNetAmount),
                nextCursor = res.nextCursor,
                hasNext = res.hasNext,
            ),
        )
    }
}