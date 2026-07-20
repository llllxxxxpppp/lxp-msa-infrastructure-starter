package com.lcs.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.FileSystemResource;

class GatewayRouteConfigurationTest {

    @Test
    void memberServiceRouteIncludesAdminMemberPath() throws IOException {
        Path gatewayConfig = Path.of(System.getProperty("user.dir"), "..", "config-repo", "gateway.yml")
                .normalize();
        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader()
                .load("gateway", new FileSystemResource(gatewayConfig));

        assertThat(propertySources)
                .anySatisfy(source -> assertThat(source.getProperty(
                        "spring.cloud.gateway.server.webflux.routes[1].predicates[0]"))
                        .isEqualTo("Path=/api/members/**,/api/admin/members/**"));
    }
}
