package com.lcs.course.presentation;

import com.lcs.course.application.dto.response.CourseRagResponse;
import com.lcs.course.application.dto.response.InstructorCourseStatusResponse;
import com.lcs.course.application.service.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CourseInternalControllerTest {

    private static final Long COURSE_ID = 30017L;

    private static final CourseRagResponse RESPONSE = new CourseRagResponse(
            COURSE_ID,
            12L,
            "실무 SQL과 대시보드 만들기",
            "SQL로 데이터를 추출하고 핵심 지표를 보여주는 대시보드를 설계합니다.",
            "DATA_ANALYSIS",
            "데이터 분석",
            "PRACTICAL",
            "실전",
            420);

    private CourseService courseService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        courseService = mock(CourseService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CourseInternalController(courseService)).build();
    }

    @Test
    @DisplayName("공개 강좌를 단건 조회하면 200과 표시명이 포함된 응답을 반환한다")
    void getCourseForRag_publicCourse_returns200() throws Exception {
        when(courseService.findCourseForRag(COURSE_ID)).thenReturn(Optional.of(RESPONSE));

        mockMvc.perform(get("/internal/courses/{courseId}/for-rag", COURSE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(COURSE_ID))
                .andExpect(jsonPath("$.description").value(RESPONSE.description()))
                .andExpect(jsonPath("$.category").value("DATA_ANALYSIS"))
                .andExpect(jsonPath("$.categoryLabel").value("데이터 분석"))
                .andExpect(jsonPath("$.difficulty").value("PRACTICAL"))
                .andExpect(jsonPath("$.difficultyLabel").value("실전"))
                .andExpect(jsonPath("$.durationMinutes").value(420));
    }

    @Test
    @DisplayName("조회 결과가 없으면 본문 없이 404를 반환한다")
    void getCourseForRag_absentCourse_returns404() throws Exception {
        when(courseService.findCourseForRag(COURSE_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/internal/courses/{courseId}/for-rag", COURSE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("초기 적재 조회는 공개 강좌 목록을 반환한다")
    void getCoursesForRag_returnsPublicCourses() throws Exception {
        when(courseService.getCoursesForRag()).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get("/internal/courses/for-rag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseId").value(COURSE_ID))
                .andExpect(jsonPath("$[0].categoryLabel").value("데이터 분석"));
    }

    @Test
    @DisplayName("기존 강사별 조회 경로가 단건 조회 경로에 가로채이지 않는다")
    void getCoursesByInstructor_isNotShadowedByForRagPath() throws Exception {
        when(courseService.getCoursesByInstructor(12L))
                .thenReturn(List.of(new InstructorCourseStatusResponse(COURSE_ID, 12L, "실무 SQL", "PUBLIC")));

        mockMvc.perform(get("/internal/courses/by-instructor/{instructorId}", 12L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(COURSE_ID));

        verify(courseService).getCoursesByInstructor(12L);
    }
}
