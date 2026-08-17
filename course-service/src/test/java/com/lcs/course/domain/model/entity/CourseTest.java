package com.lcs.course.domain.model.entity;

import com.lcs.course.domain.exception.CourseException;
import com.lcs.course.domain.model.vo.Category;
import com.lcs.course.domain.model.vo.Difficulty;
import com.lcs.course.domain.model.vo.InstructorId;
import com.lcs.course.domain.model.vo.Title;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CourseTest {

    private static final InstructorId INSTRUCTOR_ID = new InstructorId(10L);
    private static final Title TITLE = new Title("실무 SQL과 대시보드");
    private static final String DESCRIPTION = "SQL로 데이터를 추출하고 핵심 지표 대시보드를 만듭니다.";
    private static final String THUMBNAIL_URL = "https://example.com/thumb.png";

    private static Course createCourse() {
        return Course.create(INSTRUCTOR_ID, TITLE, DESCRIPTION, THUMBNAIL_URL,
                Category.DATA_ANALYSIS, Difficulty.PRACTICAL, 360);
    }

    // -------------------------------------------------------------------------
    // Course.create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("유효한 값으로 강좌를 생성하면 카테고리, 난이도, 학습 시간이 보관된다")
    void givenValidValues_whenCreateCourse_thenRecommendationFieldsAreStored() {
        Course course = createCourse();

        assertEquals(Category.DATA_ANALYSIS, course.getCategory());
        assertEquals(Difficulty.PRACTICAL, course.getDifficulty());
        assertEquals(360, course.getDurationMinutes());
    }

    @Test
    @DisplayName("카테고리가 null이면 강좌 생성 시 예외가 발생한다")
    void givenNullCategory_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> Course.create(INSTRUCTOR_ID, TITLE, DESCRIPTION, THUMBNAIL_URL,
                null, Difficulty.PRACTICAL, 360));
    }

    @Test
    @DisplayName("난이도가 null이면 강좌 생성 시 예외가 발생한다")
    void givenNullDifficulty_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> Course.create(INSTRUCTOR_ID, TITLE, DESCRIPTION, THUMBNAIL_URL,
                Category.DATA_ANALYSIS, null, 360));
    }

    @Test
    @DisplayName("학습 시간이 0이면 강좌 생성 시 예외가 발생한다")
    void givenZeroDurationMinutes_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> Course.create(INSTRUCTOR_ID, TITLE, DESCRIPTION, THUMBNAIL_URL,
                Category.DATA_ANALYSIS, Difficulty.PRACTICAL, 0));
    }

    @Test
    @DisplayName("학습 시간이 음수이면 강좌 생성 시 예외가 발생한다")
    void givenNegativeDurationMinutes_whenCreateCourse_thenThrowsException() {
        assertThrows(CourseException.class, () -> Course.create(INSTRUCTOR_ID, TITLE, DESCRIPTION, THUMBNAIL_URL,
                Category.DATA_ANALYSIS, Difficulty.PRACTICAL, -1));
    }

    // -------------------------------------------------------------------------
    // Course.update
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("강좌를 수정하면 카테고리, 난이도, 학습 시간이 갱신된다")
    void givenValidValues_whenUpdateCourse_thenRecommendationFieldsAreUpdated() {
        Course course = createCourse();

        course.update(TITLE, DESCRIPTION, THUMBNAIL_URL, Category.AI_ML, Difficulty.ADVANCED, 600);

        assertEquals(Category.AI_ML, course.getCategory());
        assertEquals(Difficulty.ADVANCED, course.getDifficulty());
        assertEquals(600, course.getDurationMinutes());
    }

    @Test
    @DisplayName("카테고리가 null이면 강좌 수정 시 예외가 발생한다")
    void givenNullCategory_whenUpdateCourse_thenThrowsException() {
        Course course = createCourse();

        assertThrows(CourseException.class,
                () -> course.update(TITLE, DESCRIPTION, THUMBNAIL_URL, null, Difficulty.ADVANCED, 600));
    }

    @Test
    @DisplayName("난이도가 null이면 강좌 수정 시 예외가 발생한다")
    void givenNullDifficulty_whenUpdateCourse_thenThrowsException() {
        Course course = createCourse();

        assertThrows(CourseException.class,
                () -> course.update(TITLE, DESCRIPTION, THUMBNAIL_URL, Category.AI_ML, null, 600));
    }

    @Test
    @DisplayName("학습 시간이 0이면 강좌 수정 시 예외가 발생한다")
    void givenZeroDurationMinutes_whenUpdateCourse_thenThrowsException() {
        Course course = createCourse();

        assertThrows(CourseException.class,
                () -> course.update(TITLE, DESCRIPTION, THUMBNAIL_URL, Category.AI_ML, Difficulty.ADVANCED, 0));
    }
}
