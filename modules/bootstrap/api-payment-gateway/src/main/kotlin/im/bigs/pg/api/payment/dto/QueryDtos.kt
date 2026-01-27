package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "결제 내역 및 통계 정보")
data class QueryResponse(
    @Schema(description = "결제 내역 목록")
    val items: List<PaymentResponse>,
    @Schema(description = "조건별 전체 통계")
    val summary: Summary,
    @Schema(description = "다음 페이지 조회용 커서 (없으면 마지막 페이지)", example = "ey... (base64)")
    val nextCursor: String?,
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
)

@Schema(description = "조회 조건별 전체 통계")
data class Summary(
    @Schema(description = "전체 건수", example = "35")
    val count: Long,
    @Schema(description = "전체 금액", example = "350000")
    val totalAmount: BigDecimal,
    @Schema(description = "전체 정산 금액 (전체 금액 - 전체 수수료)", example = "339500")
    val totalNetAmount: BigDecimal,
)