package com.lcs.auth.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {
    // member-service 무응답 시 호출 스레드가 무한 대기하지 않도록 전송 계층 timeout을 둔다.
    // 이 timeout으로 끊긴 호출(ResourceAccessException)은 서킷 브레이커가 실패로 집계한다.
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(2))
                .withReadTimeout(Duration.ofSeconds(3));
        return configurer.configure(RestClient.builder())
                .requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings));
    }
}
