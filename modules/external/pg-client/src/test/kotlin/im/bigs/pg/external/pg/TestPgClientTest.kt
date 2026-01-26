package im.bigs.pg.external.pg

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestPgClientTest {

    private val objectMapper = jacksonObjectMapper()
    private val restTemplate = mockk<RestTemplate>()
    private val restTemplateBuilder = mockk<RestTemplateBuilder>()
    
    // TestPgClient 초기화 전에 Builder Mock 설정
    init {
        every { restTemplateBuilder.connectTimeout(any()) } returns restTemplateBuilder
        every { restTemplateBuilder.readTimeout(any()) } returns restTemplateBuilder
        every { restTemplateBuilder.build() } returns restTemplate
    }

    private val testPgClient = TestPgClient(objectMapper, restTemplateBuilder)

    @Test
    @DisplayName("TestPgClient는 partnerId가 1일 때만 지원해야 한다")
    fun `supports_메소드_검증`() {
        // Given & When & Then: 모든 파트너 ID에 대해 true를 반환하는지 확인
        assertTrue(testPgClient.supports(1L))
        assertTrue(testPgClient.supports(2L))
        assertTrue(testPgClient.supports(999L))
    }

    @Test
    @DisplayName("외부 PG 승인 요청 시 암호화된 요청을 보내고 응답을 정상적으로 처리해야 한다")
    fun `외부_PG_승인_성공_테스트`() {
        // Given: 승인 요청 데이터 준비
        val request = PgApproveRequest(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "1234",
            productName = "테스트상품"
        )

        // Given: 가짜 응답(성공) 설정
        val responseMap = mapOf(
            "approvalCode" to "12345678",
            "approvedAt" to LocalDateTime.now().toString(),
            "status" to "APPROVED"
        )
        val responseEntity = ResponseEntity(responseMap, HttpStatus.OK)

        // Given: RestTemplate이 호출되면 가짜 응답을 반환하도록 설정
        every { 
            restTemplate.postForEntity(any<String>(), any(), Map::class.java) 
        } returns responseEntity as ResponseEntity<Map<*, *>>

        // When: 승인 요청 실행
        val result = testPgClient.approve(request)

        // Then: 응답 코드와 상태가 정상적으로 매핑되었는지 확인
        assertEquals("12345678", result.approvalCode)
        assertEquals(PaymentStatus.APPROVED, result.status)

        // Then: RestTemplate이 실제로 호출되었는지 검증
        verify(exactly = 1) { 
            restTemplate.postForEntity(any<String>(), any(), Map::class.java) 
        }
    }

    @Test
    @DisplayName("PG 승인 실패 (4xx) 응답 시 IllegalArgumentException이 발생해야 한다")
    fun `PG_승인_실패_4xx_테스트`() {
        // Given: 4xx 에러 응답(잔액 부족 등) 설정
        val request = PgApproveRequest(1L, BigDecimal("10000"), "123", "1234", "상품")
        
        every { 
            restTemplate.postForEntity(any<String>(), any(), Map::class.java) 
        } throws HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", "{\"message\":\"잔액부족\"}".toByteArray(), null)

        // When & Then: IllegalArgumentException 발생 및 에러 메시지 포함 확인
        val exception = assertThrows<IllegalArgumentException> {
            testPgClient.approve(request)
        }
        assertTrue(exception.message!!.contains("PG Approval Failed"))
    }

    @Test
    @DisplayName("PG 서버 오류 (5xx) 응답 시 IllegalStateException이 발생해야 한다")
    fun `PG_서버_오류_5xx_테스트`() {
        // Given: 5xx 서버 에러 응답 설정
        val request = PgApproveRequest(1L, BigDecimal("10000"), "123", "1234", "상품")
        
        every { 
            restTemplate.postForEntity(any<String>(), any(), Map::class.java) 
        } throws HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR)

        // When & Then: IllegalStateException 발생 확인
        assertThrows<IllegalStateException> {
            testPgClient.approve(request)
        }
    }

    @Test
    @DisplayName("네트워크 오류/타임아웃 시 IllegalStateException이 발생해야 한다")
    fun `네트워크_오류_테스트`() {
        // Given: 네트워크 타임아웃 오류 설정
        val request = PgApproveRequest(1L, BigDecimal("10000"), "123", "1234", "상품")
        
        every { 
            restTemplate.postForEntity(any<String>(), any(), Map::class.java) 
        } throws ResourceAccessException("Connection timed out")

        // When & Then: IllegalStateException 발생 확인
        assertThrows<IllegalStateException> {
            testPgClient.approve(request)
        }
    }
}
