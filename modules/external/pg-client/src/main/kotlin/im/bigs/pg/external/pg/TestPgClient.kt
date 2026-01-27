package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.slf4j.LoggerFactory
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.LocalDateTime

/**
 * Test PG 연동 클라이언트: 실제 Test PG 서버와 통신합니다.
 * - Test PG API 문서에 따라 AES-256-GCM 암호화/복호화 및 HTTP 호출을 수행합니다.
 */
@Component
class TestPgClient(
    private val objectMapper: ObjectMapper,
    restTemplateBuilder: RestTemplateBuilder // Builder를 주입받아 타임아웃 설정
) : PgClientOutPort {

    private val log = LoggerFactory.getLogger(javaClass)
    private val API_KEY = "11111111-1111-4111-8111-111111111111"
    private val BASE_URL = "https://api-test-pg.bigs.im"

    // 타임아웃이 설정된 RestTemplate 생성
    private val restTemplate: RestTemplate = restTemplateBuilder
        .connectTimeout(Duration.ofSeconds(3)) // 연결 3초 제한
        .readTimeout(Duration.ofSeconds(10))   // 응답 대기 10초 제한
        .build()

    override fun supports(partnerId: Long): Boolean {
        // 파트너 ID가 3번(BananaPay)이 아닌 모든 요청은 기본 Test PG 연동을 사용함
        return partnerId != 3L
    }

    override fun approve(request: PgApproveRequest): PgApproveResult {
        try {
            // 1. PgApproveRequest 객체를 JSON 문자열로 변환
            // (API 문서의 스키마에 맞게 필드 구성, 부족한 데이터는 더미 데이터 사용)
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

            // 4. 성공 응답 데이터 매핑
            val responseBody = responseEntity.body!!
            
            return PgApproveResult(
                approvalCode = responseBody["approvalCode"].toString(),
                approvedAt = LocalDateTime.parse(responseBody["approvedAt"].toString()),
                status = PaymentStatus.valueOf(responseBody["status"].toString())
            )

        } catch (e: HttpClientErrorException) {
            val errorBody = e.responseBodyAsString
            log.error("PG Approval Failed (4xx): {}", errorBody)
            throw IllegalArgumentException("PG Approval Failed (4xx): $errorBody", e)

        } catch (e: HttpServerErrorException) {
            log.error("PG Server Error (5xx): status={}, body={}", e.statusCode, e.responseBodyAsString)
            throw IllegalStateException("PG Server Error (5xx): ${e.statusCode}", e)

        } catch (e: ResourceAccessException) {
            log.error("PG Network Timeout or Connection Error: {}", e.message)
            throw IllegalStateException("PG Network Timeout or Connection Error", e)

        } catch (e: Exception) {
            log.error("Unknown PG Error: {}", e.message, e)
            throw RuntimeException("Unknown PG Error: ${e.message}", e)
        }
    }
}
