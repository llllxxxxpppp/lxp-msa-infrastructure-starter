package com.lcs.course.repository;

import com.lcs.course.model.entity.Course;
import com.lcs.course.model.vo.ContentStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findAllByStatus(ContentStatus status, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.status = :status AND LOWER(c.title.value) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Course> findByStatusAndTitleKeyword(
            @Param("status") ContentStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    List<Course> findAllByInstructorIdAndStatusAndDeletedAtIsNull(Long instructorId, ContentStatus status);
}
