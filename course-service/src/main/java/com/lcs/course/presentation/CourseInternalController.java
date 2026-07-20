package com.lcs.course.presentation;

import com.lcs.course.application.dto.response.InstructorCourseStatusResponse;
import com.lcs.course.application.service.CourseService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/courses")
public class CourseInternalController {

    private final CourseService courseService;

    public CourseInternalController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/by-instructor/{instructorId}/unpublish-all")
    public ResponseEntity<Void> unpublishAllByInstructor(@PathVariable Long instructorId) {
        courseService.unpublishAllByInstructor(instructorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/by-instructor/{instructorId}")
    public ResponseEntity<List<InstructorCourseStatusResponse>> getCoursesByInstructor(
            @PathVariable Long instructorId) {
        return ResponseEntity.ok(courseService.getCoursesByInstructor(instructorId));
    }
}
