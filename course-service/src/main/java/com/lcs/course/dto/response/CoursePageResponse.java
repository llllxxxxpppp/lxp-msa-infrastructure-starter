package com.lcs.course.dto.response;

import com.lcs.course.model.entity.Course;
import java.util.List;
import org.springframework.data.domain.Page;

public record CoursePageResponse(
        List<CourseSummaryResponse> courses,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last) {

    public static CoursePageResponse from(Page<Course> coursePage) {
        return new CoursePageResponse(
                coursePage.getContent().stream().map(CourseSummaryResponse::from).toList(),
                coursePage.getNumber(),
                coursePage.getSize(),
                coursePage.getTotalElements(),
                coursePage.getTotalPages(),
                coursePage.isLast());
    }
}
