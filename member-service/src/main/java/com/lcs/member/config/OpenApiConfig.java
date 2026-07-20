package com.lcs.member.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI memberServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Member Service API")
                        .description("LXP MSA 회원(Member) 바운디드 컨텍스트 API 문서")
                        .version("v1"));
    }
}
