package com.lcs.course.infrastructure.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.lcs.course.application.service.CourseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InstructorSuspendedListenerTest {

    @Test
    @DisplayName("강사 정지 이벤트를 받으면 해당 강사의 강좌를 비공개 처리한다")
    void handle_unpublishesCoursesOfInstructor() {
        // given
        CourseService courseService = mock(CourseService.class);
        InstructorSuspendedListener listener = new InstructorSuspendedListener(courseService);

        // when
        listener.handle(new InstructorSuspendedMessage(1L));

        // then
        verify(courseService).unpublishAllByInstructor(1L);
    }
}
