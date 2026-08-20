package com.lcs.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MEMBER-17: Swagger(OpenAPI) 도입 검증.
 *
 * <p>Mockito 협력자가 없는 실제 Spring 컨텍스트(내장 톰캣 + springdoc) 기반 통합 테스트이므로,
 * {@code verify()}/{@code verifyNoInteractions()} 대신 JUnit assert로 검증한다
 * (MEMBER-09의 {@code HttpMemberStateChangeNotifierTest}, MEMBER-11의 {@code SeedDataTest}와 동일한 선례).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class OpenApiIntegrationTest {

    private static final String ADMIN_TAG_NAME = "관리자 - 회원 관리";
    private static final String ADMIN_TAG_DESCRIPTION = "관리자가 강사 계정을 생성/정지하는 API";
    private static final String SELF_TAG_NAME = "회원 자기 관리";
    private static final String SELF_TAG_DESCRIPTION = "회원 본인이 비밀번호 변경/강사 프로필 수정/탈퇴를 처리하는 API";
    private static final String INTERNAL_TAG_NAME = "내부 API (Auth/Course 전용 - 외부 미노출)";

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // 완료 기준 1: springdoc-openapi-starter-webmvc-ui 의존성 추가
    //
    // springdoc 의존성은 더 이상 member-service/build.gradle에 직접 선언되지 않고,
    // 공용 컨벤션 플러그인(buildlogic.java-spring-boot-swagger-conventions)으로 이동했다.
    // 그래서 이 테스트도 (1) build.gradle이 해당 플러그인을 적용하는지, (2) 그 플러그인이
    // 실제로 springdoc 의존성을 선언하는지 두 단계로 확인한다.
    // -------------------------------------------------------------------------

    private static final String SWAGGER_CONVENTION_PLUGIN_ID = "buildlogic.java-spring-boot-swagger-conventions";
    private static final Path SWAGGER_CONVENTION_PLUGIN_FILE =
            Path.of("../buildLogic/src/main/groovy/" + SWAGGER_CONVENTION_PLUGIN_ID + ".gradle");

    @Test
    @DisplayName("build.gradle이 swagger 컨벤션 플러그인을 적용하고, 그 플러그인이 springdoc-openapi-starter-webmvc-ui 의존성을 선언한다")
    void givenBuildGradleFile_whenReadingDependencies_thenContainsSpringdocOpenApiStarterWebMvcUi() throws IOException {
        String buildGradleContent = Files.readString(Path.of("build.gradle"));

        assertTrue(buildGradleContent.contains(SWAGGER_CONVENTION_PLUGIN_ID),
                "build.gradle이 " + SWAGGER_CONVENTION_PLUGIN_ID + " 플러그인을 적용해야 한다.");

        String conventionPluginContent = Files.readString(SWAGGER_CONVENTION_PLUGIN_FILE);

        assertTrue(conventionPluginContent.contains("springdoc-openapi-starter-webmvc-ui"),
                SWAGGER_CONVENTION_PLUGIN_ID + " 플러그인에 springdoc-openapi-starter-webmvc-ui 의존성이 존재해야 한다.");
    }

    // -------------------------------------------------------------------------
    // 완료 기준 2: GET /v3/api-docs 200 + 3개 태그 이름 모두 포함
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("애플리케이션 기동 후 GET /v3/api-docs 요청은 200을 반환하고 응답에 3개 태그 이름이 모두 포함된다")
    void givenRunningApplication_whenGetApiDocs_thenReturns200AndContainsAllThreeTagNames() throws Exception {
        MockHttpServletResponse response =
                mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        String body = response.getContentAsString();
        assertTrue(body != null && body.contains(ADMIN_TAG_NAME),
                "응답에 관리자 - 회원 관리 태그 이름이 포함되어야 한다.");
        assertTrue(body.contains(SELF_TAG_NAME),
                "응답에 회원 자기 관리 태그 이름이 포함되어야 한다.");
        assertTrue(body.contains(INTERNAL_TAG_NAME),
                "응답에 내부 API 태그 이름이 포함되어야 한다.");
    }

    // -------------------------------------------------------------------------
    // 완료 기준 3: GET /swagger-ui/index.html 200
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("애플리케이션 기동 후 GET /swagger-ui/index.html 요청은 최종적으로 200을 반환한다")
    void givenRunningApplication_whenGetSwaggerUiIndexHtml_thenReturns200() throws Exception {
        MockHttpServletResponse response =
                mockMvc.perform(get("/swagger-ui/index.html")).andReturn().getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertTrue(response.getContentAsString() != null && !response.getContentAsString().isBlank(),
                "swagger-ui/index.html 응답 본문이 비어있지 않아야 한다.");
    }

    // -------------------------------------------------------------------------
    // 완료 기준 4: 3개 컨트롤러의 경로가 /v3/api-docs 응답에 각각 노출
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("애플리케이션 기동 후 GET /v3/api-docs 응답의 paths에 3개 컨트롤러 경로가 각각 노출된다")
    void givenRunningApplication_whenGetApiDocs_thenPathsContainAllThreeControllerBasePaths()
            throws Exception {
        MockHttpServletResponse response =
                mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.getContentAsString());
        JsonNode paths = root.path("paths");

        boolean hasSelfPath = false;
        boolean hasAdminPath = false;
        boolean hasInternalPath = false;

        Iterator<String> pathNames = paths.fieldNames();
        while (pathNames.hasNext()) {
            String path = pathNames.next();
            if (path.startsWith("/api/members/me")) {
                hasSelfPath = true;
            }
            if (path.startsWith("/api/admin/members")) {
                hasAdminPath = true;
            }
            if (path.startsWith("/internal/members")) {
                hasInternalPath = true;
            }
        }

        assertTrue(hasSelfPath, "paths에 /api/members/me로 시작하는 경로가 있어야 한다.");
        assertTrue(hasAdminPath, "paths에 /api/admin/members로 시작하는 경로가 있어야 한다.");
        assertTrue(hasInternalPath, "paths에 /internal/members로 시작하는 경로가 있어야 한다.");
    }

    // -------------------------------------------------------------------------
    // 완료 기준 5: 내부 API 태그 설명에 "외부"/"노출되면 안 된다" 관련 문구 포함
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("내부 API 태그의 description에 외부 노출 금지 문구가 포함되어 문서만 보고도 내부 전용임을 알 수 있다")
    void givenRunningApplication_whenGetApiDocs_thenInternalTagDescriptionWarnsAgainstExternalExposure()
            throws Exception {
        MockHttpServletResponse response =
                mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.getContentAsString());
        JsonNode tags = root.path("tags");

        String internalTagDescription = null;
        for (JsonNode tag : tags) {
            if (INTERNAL_TAG_NAME.equals(tag.path("name").asText())) {
                internalTagDescription = tag.path("description").asText();
            }
        }

        assertTrue(internalTagDescription != null, "내부 API 태그가 tags 목록에 존재해야 한다.");
        assertTrue(internalTagDescription.contains("외부"),
                "내부 API 태그 description에 '외부' 관련 문구가 포함되어야 한다.");
        assertTrue(internalTagDescription.contains("노출되면 안 된다"),
                "내부 API 태그 description에 '노출되면 안 된다' 문구가 포함되어야 한다.");
    }

    // -------------------------------------------------------------------------
    // 부가 검증: 태그 스펙(description) 전체 일치 - 관리자/회원 자기 관리 태그
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("애플리케이션 기동 후 GET /v3/api-docs 응답의 관리자/회원 자기 관리 태그 description이 세션에서 확정한 문구와 일치한다")
    void givenRunningApplication_whenGetApiDocs_thenAdminAndSelfTagDescriptionsMatchSpec() throws Exception {
        MockHttpServletResponse response =
                mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse();

        assertEquals(HttpStatus.OK.value(), response.getStatus());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(response.getContentAsString());
        JsonNode tags = root.path("tags");

        String adminDescription = null;
        String selfDescription = null;
        for (JsonNode tag : tags) {
            String name = tag.path("name").asText();
            if (ADMIN_TAG_NAME.equals(name)) {
                adminDescription = tag.path("description").asText();
            }
            if (SELF_TAG_NAME.equals(name)) {
                selfDescription = tag.path("description").asText();
            }
        }

        assertEquals(ADMIN_TAG_DESCRIPTION, adminDescription);
        assertEquals(SELF_TAG_DESCRIPTION, selfDescription);
    }
}
