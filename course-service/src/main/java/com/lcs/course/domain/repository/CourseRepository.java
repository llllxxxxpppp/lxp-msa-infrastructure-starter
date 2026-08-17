package com.lcs.course.domain.repository;

import com.lcs.course.domain.model.entity.Course;
import com.lcs.course.domain.model.vo.ContentStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findAllByStatusAndDeletedAtIsNull(ContentStatus status, Pageable pageable);

    @Query("SELECT c FROM Course c WHERE c.status = :status AND c.deletedAt IS NULL AND LOWER(c.title.value) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Course> findByStatusAndTitleKeyword(
            @Param("status") ContentStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    List<Course> findAllByInstructorIdAndStatusAndDeletedAtIsNull(Long instructorId, ContentStatus status);

    List<Course> findAllByStatusAndDeletedAtIsNull(ContentStatus status);
}
