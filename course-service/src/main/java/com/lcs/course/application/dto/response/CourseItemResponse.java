package com.lcs.course.application.dto.response;

import com.lcs.course.domain.model.entity.Lecture;
import com.lcs.course.domain.model.entity.Mission;
import com.lcs.course.domain.model.vo.SortableType;

public record CourseItemResponse(String type, Long id, String title, String status, int sortOrder) {

    public static CourseItemResponse fromLecture(Lecture lecture) {
        return new CourseItemResponse(
                SortableType.LECTURE.name(),
                lecture.getId().value(),
                lecture.getTitle().getValue(),
                lecture.getStatus().name(),
                lecture.getSortOrder());
    }

    public static CourseItemResponse fromMission(Mission mission) {
        return new CourseItemResponse(
                SortableType.MISSION.name(),
                mission.getId().value(),
                mission.getTitle().getValue(),
                mission.getStatus().name(),
                mission.getSortOrder());
    }
}
