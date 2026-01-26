package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime

/**
 * Test PG 연동 클라이언트: 실제 Test PG 서버와 통신합니다.
 * - Test PG API 문서에 따라 AES-256-GCM 암호화/복호화 및 HTTP 호출을 수행합니다.
 */
@Component
class TestPgClient(
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate = RestTemplate(),
) : PgClientOutPort {

    private val API_KEY = "11111111-1111-4111-8111-111111111111"
    private val BASE_URL = "https://api-test-pg.bigs.im"

    override fun supports(partnerId: Long): Boolean {
        // 모든 파트너를 지원하거나, 특정 파트너만 지원하도록 설정 가능
        // 과제 수행을 위해 임시로 partnerId가 1인 경우만 지원하도록 설정 (MockPgClient와 충돌 방지)
        return partnerId == 1L
    }

    override fun approve(request: PgApproveRequest): PgApproveResult {
        // 1. PgApproveRequest 객체를 JSON 문자열로 변환
        // (API 문서의 스키마에 맞게 필드 구성)
        val plainPayload = mapOf(
            "cardNumber" to (request.cardBin + "000000" + request.cardLast4),
            "birthDate" to "19900101",
            "expiry" to "1227",
            "password" to "12",
            "amount" to request.amount.toLong()
        )
        val jsonString = objectMapper.writeValueAsString(plainPayload)

        // 2. EncryptionUtil을 사용하여 JSON 문자열을 AES-256-GCM으로 암호화
        val encryptedData = EncryptionUtil.encrypt(jsonString)

        // 3. RestTemplate을 사용하여 POST 요청 전송
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("API-KEY", API_KEY)

        val requestBody = mapOf("enc" to encryptedData)
        val httpEntity = HttpEntity(requestBody, headers)

        val responseEntity = restTemplate.postForEntity(
            "$BASE_URL/api/v1/pay/credit-card",
            httpEntity,
            Map::class.java
        )

        // 4. 응답 처리 및 복호화
        if (responseEntity.statusCode.is2xxSuccessful) {
            val responseBody = responseEntity.body as Map<*, *>

            // 성공 응답은 평문으로 오므로 바로 매핑 (문서에 응답은 암호화된다는 언급이 없으면 평문 처리)
            return PgApproveResult(
                approvalCode = responseBody["approvalCode"].toString(),
                approvedAt = LocalDateTime.parse(responseBody["approvedAt"].toString()),
                status = PaymentStatus.valueOf(responseBody["status"].toString())
            )
        } else {
            throw RuntimeException("PG approval failed: ${responseEntity.statusCode}")
        }
    }
}
