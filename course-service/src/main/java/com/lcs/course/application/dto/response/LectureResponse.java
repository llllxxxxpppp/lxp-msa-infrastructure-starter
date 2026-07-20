package com.lcs.course.application.dto.response;

import com.lcs.course.domain.model.entity.Lecture;

public record LectureResponse(Long lectureId, String title, String status, String contentType, int sortOrder) {

    public static LectureResponse from(Lecture lecture) {
        return new LectureResponse(
                lecture.getId().value(),
                lecture.getTitle().getValue(),
                lecture.getStatus().name(),
                lecture.getContentType(),
                lecture.getSortOrder());
    }
}
