package im.bigs.pg.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("나노바나나 페이먼츠 결제 시스템 API")
                    .description("결제 생성, 이력 조회 및 외부 PG 연동을 위한 백엔드 API 문서입니다.")
                    .version("v1.0.0")
            )
    }
}
