package com.lcs.course.infrastructure;

import com.lcs.grpc.member.MemberSuspensionServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "course.instructor-status.mode", havingValue = "grpc")
public class GrpcClientConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel memberChannel(
            @Value("${member.grpc.host:localhost}") String host,
            @Value("${member.grpc.port:9090}") int port) {
        return ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
    }

    @Bean
    public MemberSuspensionServiceGrpc.MemberSuspensionServiceBlockingStub memberSuspensionStub(
            ManagedChannel memberChannel) {
        return MemberSuspensionServiceGrpc.newBlockingStub(memberChannel);
    }
}
