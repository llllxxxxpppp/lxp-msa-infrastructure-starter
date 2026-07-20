package com.lcs.auth.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "auth.member-client.mode", havingValue = "grpc")
    public ManagedChannel memberServiceChannel(
            @Value("${auth.member-client.grpc.host}") String host,
            @Value("${auth.member-client.grpc.port}") int port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }
}
