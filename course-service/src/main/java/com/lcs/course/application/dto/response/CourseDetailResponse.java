package com.lcs.course.application.dto.response;

import com.lcs.course.domain.model.entity.Course;
import com.lcs.course.domain.model.entity.Lecture;
import com.lcs.course.domain.model.entity.Mission;
import com.lcs.course.domain.model.vo.Sortable;
import java.util.List;

public record CourseDetailResponse(
        Long courseId,
        Long instructorId,
        String title,
        String status,
        String description,
        String thumbnailUrl,
        List<LectureResponse> lectures,
        List<MissionResponse> missions,
        List<CourseItemResponse> items) {

    public static CourseDetailResponse from(Course course) {
        List<LectureResponse> lectures = course.getLectures().stream()
                .map(LectureResponse::from)
                .toList();
        List<MissionResponse> missions = course.getMissions().stream()
                .map(MissionResponse::from)
                .toList();
        List<CourseItemResponse> items = course.getSortableItems().stream()
                .map(CourseDetailResponse::toItemResponse)
                .toList();
        return new CourseDetailResponse(
                course.getId().value(),
                course.getInstructorId().value(),
                course.getTitle().getValue(),
                course.getStatus().name(),
                course.getDescription(),
                course.getThumbnailUrl(),
                lectures,
                missions,
                items);
    }

    private static CourseItemResponse toItemResponse(Sortable sortable) {
        if (sortable instanceof Lecture lecture) {
            return CourseItemResponse.fromLecture(lecture);
        }
        return CourseItemResponse.fromMission((Mission) sortable);
    }
}
