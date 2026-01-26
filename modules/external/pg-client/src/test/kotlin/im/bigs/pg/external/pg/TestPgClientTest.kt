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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDateTime

class TestPgClientTest {

    private val objectMapper = jacksonObjectMapper()
    private val restTemplate = mockk<RestTemplate>()
    private val testPgClient = TestPgClient(objectMapper, restTemplate)

    @Suppress("UNCHECKED_CAST")
    @Test
    @DisplayName("외부 PG 승인 요청 시 암호화된 요청을 보내고 응답을 정상적으로 처리해야 한다")
    fun `외부_PG_승인_성공_테스트`() {
        // Given
        val request = PgApproveRequest(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "1234",
            productName = "테스트상품"
        )

        // 가짜 응답 데이터 (평문)
        val responseMap = mapOf(
            "approvalCode" to "12345678",
            "approvedAt" to LocalDateTime.now().toString(),
            "status" to "APPROVED"
        )
        
        val responseEntity = ResponseEntity(responseMap, HttpStatus.OK)

        // RestTemplate이 호출되면 가짜 응답을 반환하도록 설정
        every { 
            restTemplate.postForEntity(any<String>(), any(), Map::class.java) 
        } returns responseEntity as ResponseEntity<Map<*, *>>

        // When
        val result = testPgClient.approve(request)

        // Then
        assertEquals("12345678", result.approvalCode)
        assertEquals(PaymentStatus.APPROVED, result.status)

        // RestTemplate이 실제로 호출되었는지 검증
        verify(exactly = 1) { 
            restTemplate.postForEntity(any<String>(), any(), Map::class.java) 
        }
    }
}
