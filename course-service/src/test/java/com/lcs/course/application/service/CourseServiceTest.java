package com.lcs.course.application.service;

import com.lcs.course.application.dto.response.CourseRagResponse;
import com.lcs.course.application.port.InstructorStatusClient;
import com.lcs.course.domain.model.entity.Course;
import com.lcs.course.domain.model.vo.Category;
import com.lcs.course.domain.model.vo.ContentStatus;
import com.lcs.course.domain.model.vo.Difficulty;
import com.lcs.course.domain.model.vo.InstructorId;
import com.lcs.course.domain.model.vo.Title;
import com.lcs.course.domain.repository.CourseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final Long COURSE_ID = 30017L;
    private static final InstructorId INSTRUCTOR_ID = new InstructorId(12L);
    private static final Title TITLE = new Title("실무 SQL과 대시보드 만들기");
    private static final String DESCRIPTION = "SQL로 데이터를 추출하고 핵심 지표를 보여주는 대시보드를 설계합니다.";
    private static final String THUMBNAIL_URL = "https://example.com/thumb.png";

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private InstructorStatusClient instructorStatusClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CourseService courseService;

    private static Course privateCourse() {
        Course course = Course.create(INSTRUCTOR_ID, TITLE, DESCRIPTION, THUMBNAIL_URL,
                Category.DATA_ANALYSIS, Difficulty.PRACTICAL, 420);
        course.addLecture(new Title("SQL 기초"), "https://example.com/lecture/1", "video");
        course.addMission(new Title("지표 쿼리 작성"), "핵심 지표를 뽑는 쿼리를 작성해 보세요.");
        ReflectionTestUtils.setField(course, "id", COURSE_ID);
        return course;
    }

    private static Course publicCourse() {
        Course course = privateCourse();
        course.publish();
        return course;
    }

    @Test
    @DisplayName("공개 강좌를 조회하면 enum 이름과 한글 표시명을 함께 담은 응답을 반환한다")
    void findCourseForRag_publicCourse_returnsResponseWithLabels() {
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(publicCourse()));

        Optional<CourseRagResponse> result = courseService.findCourseForRag(COURSE_ID);

        assertTrue(result.isPresent());
        CourseRagResponse response = result.get();
        assertEquals(COURSE_ID, response.courseId());
        assertEquals(12L, response.instructorId());
        assertEquals("실무 SQL과 대시보드 만들기", response.title());
        assertEquals(DESCRIPTION, response.description());
        assertEquals("DATA_ANALYSIS", response.category());
        assertEquals("데이터 분석", response.categoryLabel());
        assertEquals("PRACTICAL", response.difficulty());
        assertEquals("실전", response.difficultyLabel());
        assertEquals(420, response.durationMinutes());
    }

    @Test
    @DisplayName("비공개 강좌를 조회하면 빈 값을 반환한다")
    void findCourseForRag_privateCourse_returnsEmpty() {
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(privateCourse()));

        assertTrue(courseService.findCourseForRag(COURSE_ID).isEmpty());
    }

    @Test
    @DisplayName("삭제된 강좌를 조회하면 빈 값을 반환한다")
    void findCourseForRag_deletedCourse_returnsEmpty() {
        Course course = publicCourse();
        course.delete();
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.of(course));

        assertTrue(courseService.findCourseForRag(COURSE_ID).isEmpty());
    }

    @Test
    @DisplayName("없는 강좌를 조회하면 빈 값을 반환한다")
    void findCourseForRag_missingCourse_returnsEmpty() {
        given(courseRepository.findById(COURSE_ID)).willReturn(Optional.empty());

        assertTrue(courseService.findCourseForRag(COURSE_ID).isEmpty());
    }

    @Test
    @DisplayName("초기 적재 조회는 공개 상태이고 삭제되지 않은 강좌만 조회한다")
    void getCoursesForRag_queriesPublicAndNotDeletedOnly() {
        given(courseRepository.findAllByStatusAndDeletedAtIsNull(ContentStatus.PUBLIC))
                .willReturn(List.of(publicCourse()));

        List<CourseRagResponse> result = courseService.getCoursesForRag();

        assertEquals(1, result.size());
        assertEquals(COURSE_ID, result.get(0).courseId());
        assertEquals("데이터 분석", result.get(0).categoryLabel());
    }
}
