package com.lcs.course.application.service;

import com.lcs.course.application.port.InstructorStatusClient;
import com.lcs.course.application.dto.response.CourseDetailResponse;
import com.lcs.course.application.dto.response.CoursePageResponse;
import com.lcs.course.application.dto.response.CourseSummaryResponse;
import com.lcs.course.application.dto.response.InstructorCourseStatusResponse;
import com.lcs.course.domain.exception.CourseAccessDeniedException;
import com.lcs.course.domain.exception.CourseException;
import com.lcs.course.domain.model.entity.Course;
import com.lcs.course.domain.model.vo.Category;
import com.lcs.course.domain.model.vo.ContentStatus;
import com.lcs.course.domain.model.vo.Difficulty;
import com.lcs.course.domain.model.vo.InstructorId;
import com.lcs.course.domain.model.vo.LectureId;
import com.lcs.course.domain.model.vo.MissionId;
import com.lcs.course.domain.model.vo.ReorderItem;
import com.lcs.course.domain.model.vo.SortableType;
import com.lcs.course.domain.model.vo.Title;
import com.lcs.course.domain.repository.CourseRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final InstructorStatusClient instructorStatusClient;

    public CourseService(CourseRepository courseRepository, InstructorStatusClient instructorStatusClient) {
        this.courseRepository = courseRepository;
        this.instructorStatusClient = instructorStatusClient;
    }

    @Transactional(readOnly = true)
    public CoursePageResponse getCourses(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Course> result;
        if (keyword == null || keyword.isBlank()) {
            result = courseRepository.findAllByStatusAndDeletedAtIsNull(ContentStatus.PUBLIC, pageable);
        } else {
            result = courseRepository.findByStatusAndTitleKeyword(ContentStatus.PUBLIC, keyword, pageable);
        }
        return CoursePageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public CourseSummaryResponse getCourseSummary(Long courseId) {
        return CourseSummaryResponse.from(getCourse(courseId));
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId) {
        return CourseDetailResponse.from(getCourse(courseId));
    }

    public void createCourse(Long instructorId, String title, String description, String thumbnailUrl,
            Category category, Difficulty difficulty, int durationMinutes) {
        rejectIfSuspended(instructorId);
        Course course = Course.create(new InstructorId(instructorId), new Title(title), description, thumbnailUrl,
                category, difficulty, durationMinutes);
        courseRepository.save(course);
    }

    public void updateCourse(
            Long courseId, String newTitle, String description, String thumbnailUrl,
            Category category, Difficulty difficulty, int durationMinutes,
            Long requesterId, boolean isAdmin) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.update(new Title(newTitle), description, thumbnailUrl, category, difficulty, durationMinutes);
    }

    public void publishCourse(Long courseId, Long requesterId, boolean isAdmin) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.publish();
    }

    public void unpublishCourse(Long courseId, Long requesterId, boolean isAdmin) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.unpublish();
    }

    public void addLecture(Long courseId, Long requesterId, boolean isAdmin,
            String title, String contentUrl, String contentType) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.addLecture(new Title(title), contentUrl, contentType);
    }

    public void publishLecture(Long courseId, Long lectureId, Long requesterId, boolean isAdmin) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.publishLecture(new LectureId(lectureId));
    }

    public void unpublishLecture(Long courseId, Long lectureId, Long requesterId, boolean isAdmin) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.unpublishLecture(new LectureId(lectureId));
    }

    public void addMission(Long courseId, Long requesterId, boolean isAdmin, String title, String content) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.addMission(new Title(title), content);
    }

    public void publishMission(Long courseId, Long missionId, Long requesterId, boolean isAdmin) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.publishMission(new MissionId(missionId));
    }

    public void unpublishMission(Long courseId, Long missionId, Long requesterId, boolean isAdmin) {
        rejectIfSuspended(requesterId);
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.unpublishMission(new MissionId(missionId));
    }

    public void deleteCourse(Long courseId, Long requesterId, boolean isAdmin) {
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.delete();
    }

    public void deleteLecture(Long courseId, Long lectureId, Long requesterId, boolean isAdmin) {
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.deleteLecture(new LectureId(lectureId));
    }

    public void deleteMission(Long courseId, Long missionId, Long requesterId, boolean isAdmin) {
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.deleteMission(new MissionId(missionId));
    }

    public void reorderItems(Long courseId, Long requesterId, boolean isAdmin,
            List<String> itemTypes, List<Long> itemIds) {
        rejectIfSuspended(requesterId);
        if (itemTypes == null || itemIds == null || itemTypes.size() != itemIds.size()) {
            throw new CourseException("순서 변경 대상 항목 목록이 올바르지 않습니다.");
        }
        List<ReorderItem> orderedItems = new ArrayList<>();
        for (int i = 0; i < itemTypes.size(); i++) {
            orderedItems.add(new ReorderItem(SortableType.valueOf(itemTypes.get(i)), itemIds.get(i)));
        }
        Course course = getCourse(courseId);
        checkOwnership(course, requesterId, isAdmin);
        course.reorder(orderedItems);
    }

    public void unpublishAllByInstructor(Long instructorId) {
        List<Course> publicCourses =
                courseRepository.findAllByInstructorIdAndStatusAndDeletedAtIsNull(instructorId, ContentStatus.PUBLIC);
        for (Course course : publicCourses) {
            course.unpublish();
        }
    }

    @Transactional(readOnly = true)
    public List<InstructorCourseStatusResponse> getCoursesByInstructor(Long instructorId) {
        return courseRepository.findAll().stream()
                .filter(course -> course.getInstructorId().value().equals(instructorId))
                .filter(course -> !course.isDeleted())
                .map(InstructorCourseStatusResponse::from)
                .toList();
    }

    private void rejectIfSuspended(Long requesterId) {
        if (instructorStatusClient.isSuspended(requesterId)) {
            throw new CourseAccessDeniedException("정지된 강사는 해당 작업을 수행할 수 없습니다.");
        }
    }

    private void checkOwnership(Course course, Long requesterId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (!course.getInstructorId().value().equals(requesterId)) {
            throw new CourseAccessDeniedException("작성한 강사만 접근할 수 있습니다.");
        }
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .filter(course -> !course.isDeleted())
                .orElseThrow(() -> new CourseException("강좌를 찾을 수 없습니다."));
    }
}
