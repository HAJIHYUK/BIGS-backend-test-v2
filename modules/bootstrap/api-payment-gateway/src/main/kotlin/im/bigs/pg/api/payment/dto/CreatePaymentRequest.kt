package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.math.BigDecimal

@Schema(description = "결제 생성 요청 정보")
data class CreatePaymentRequest(
    @Schema(description = "제휴사 식별자", example = "1")
    val partnerId: Long,
    @Schema(description = "결제 금액", example = "10000")
    @field:Min(1)
    val amount: BigDecimal,
    @Schema(description = "카드 BIN (앞 6자리)", example = "123456")
    val cardBin: String? = null,
    @Schema(description = "카드 마지막 4자리", example = "1234")
    val cardLast4: String? = null,
    @Schema(description = "상품명", example = "테스트 상품")
    val productName: String? = null,
)