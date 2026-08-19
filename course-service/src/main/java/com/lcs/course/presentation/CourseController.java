package com.lcs.course.presentation;

import com.lcs.course.application.dto.request.AddLectureRequest;
import com.lcs.course.application.dto.request.AddMissionRequest;
import com.lcs.course.application.dto.request.CreateCourseRequest;
import com.lcs.course.application.dto.request.ReorderRequest;
import com.lcs.course.application.dto.request.UpdateCourseRequest;
import com.lcs.course.application.dto.response.CourseDetailResponse;
import com.lcs.course.application.dto.response.CoursePageResponse;
import com.lcs.course.application.dto.response.CourseSummaryResponse;
import com.lcs.course.application.service.CourseService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
// [추가] 담당 강좌 목록 응답
import com.lcs.course.application.dto.response.InstructorCourseStatusResponse;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<CoursePageResponse> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(courseService.getCourses(keyword, page, size));
    }

    // [추가] 로그인한 강사의 담당 강좌 목록을 조회한다.
    @GetMapping("/instructor/me")
    public ResponseEntity<List<InstructorCourseStatusResponse>> getInstructorCourses(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(courseService.getCoursesByInstructor(userId));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseSummaryResponse> getCourseSummary(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseSummary(courseId));
    }

    @GetMapping("/{courseId}/detail")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    @PostMapping
    public ResponseEntity<Void> createCourse(
            @RequestBody @Valid CreateCourseRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        courseService.createCourse(userId, request.title(), request.description(), request.thumbnailUrl(),
                request.category(), request.difficulty(), request.durationMinutes());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{courseId}")
    public ResponseEntity<Void> updateCourse(
            @PathVariable Long courseId,
            @RequestBody @Valid UpdateCourseRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.updateCourse(
                courseId, request.title(), request.description(), request.thumbnailUrl(),
                request.category(), request.difficulty(), request.durationMinutes(),
                userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/publish")
    public ResponseEntity<Void> publishCourse(
            @PathVariable Long courseId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.publishCourse(courseId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/unpublish")
    public ResponseEntity<Void> unpublishCourse(
            @PathVariable Long courseId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.unpublishCourse(courseId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long courseId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.deleteCourse(courseId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/lectures")
    public ResponseEntity<Void> addLecture(
            @PathVariable Long courseId,
            @RequestBody @Valid AddLectureRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.addLecture(courseId, userId, isAdmin(role),
                request.title(), request.contentUrl(), request.contentType());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{courseId}/lectures/{lectureId}/publish")
    public ResponseEntity<Void> publishLecture(
            @PathVariable Long courseId,
            @PathVariable Long lectureId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.publishLecture(courseId, lectureId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/lectures/{lectureId}/unpublish")
    public ResponseEntity<Void> unpublishLecture(
            @PathVariable Long courseId,
            @PathVariable Long lectureId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.unpublishLecture(courseId, lectureId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}/lectures/{lectureId}")
    public ResponseEntity<Void> deleteLecture(
            @PathVariable Long courseId,
            @PathVariable Long lectureId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.deleteLecture(courseId, lectureId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/missions")
    public ResponseEntity<Void> addMission(
            @PathVariable Long courseId,
            @RequestBody @Valid AddMissionRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.addMission(courseId, userId, isAdmin(role), request.title(), request.content());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{courseId}/missions/{missionId}/publish")
    public ResponseEntity<Void> publishMission(
            @PathVariable Long courseId,
            @PathVariable Long missionId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.publishMission(courseId, missionId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{courseId}/missions/{missionId}/unpublish")
    public ResponseEntity<Void> unpublishMission(
            @PathVariable Long courseId,
            @PathVariable Long missionId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.unpublishMission(courseId, missionId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{courseId}/missions/{missionId}")
    public ResponseEntity<Void> deleteMission(
            @PathVariable Long courseId,
            @PathVariable Long missionId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        courseService.deleteMission(courseId, missionId, userId, isAdmin(role));
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{courseId}/reorder")
    public ResponseEntity<Void> reorderItems(
            @PathVariable Long courseId,
            @RequestBody @Valid ReorderRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-Role", required = false) String role) {
        List<String> itemTypes = request.items().stream()
                .map(item -> item.type().name())
                .toList();
        List<Long> itemIds = request.items().stream()
                .map(ReorderRequest.Item::id)
                .toList();
        courseService.reorderItems(courseId, userId, isAdmin(role), itemTypes, itemIds);
        return ResponseEntity.ok().build();
    }

    private boolean isAdmin(String role) {
        return role != null && Arrays.stream(role.split(","))
                .map(String::trim)
                .anyMatch(ROLE_ADMIN::equals);
    }
}
