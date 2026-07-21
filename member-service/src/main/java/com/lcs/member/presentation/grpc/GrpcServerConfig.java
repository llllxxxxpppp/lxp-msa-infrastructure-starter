package com.lcs.member.presentation.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GrpcServerConfig {

    @Bean(destroyMethod = "shutdown")
    public Server grpcServer(MemberSuspensionGrpcService memberSuspensionGrpcService,
                              MemberLoginInfoGrpcService memberLoginInfoGrpcService,
                              @Value("${member.grpc.port:9090}") int port) throws IOException {
        Server server = ServerBuilder.forPort(port)
                .addService(memberSuspensionGrpcService)
                .addService(memberLoginInfoGrpcService)
                .build();
        server.start();
        return server;
    }
}
