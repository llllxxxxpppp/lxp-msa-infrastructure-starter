package com.lcs.course.model.entity;

import com.lcs.course.exception.CourseException;
import com.lcs.course.model.vo.ContentStatus;
import com.lcs.course.model.vo.LectureId;
import com.lcs.course.model.vo.Sortable;
import com.lcs.course.model.vo.Title;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lectures")
public class Lecture implements Sortable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Embedded
    private Title title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status;

    @Column(nullable = false)
    private String contentUrl;

    @Column(nullable = false)
    private String contentType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime deletedAt;

    protected Lecture() {}

    static Lecture create(Course course, Title title, String contentUrl, String contentType, int sortOrder) {
        if (course == null) {
            throw new CourseException("강좌는 null일 수 없습니다.");
        }
        if (title == null) {
            throw new CourseException("제목은 null일 수 없습니다.");
        }
        if (contentUrl == null || contentUrl.isBlank()) {
            throw new CourseException("강의 자료 URL은 비어있을 수 없습니다.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new CourseException("자료 타입은 비어있을 수 없습니다.");
        }
        Lecture lecture = new Lecture();
        lecture.course = course;
        lecture.title = title;
        lecture.status = ContentStatus.PUBLIC;
        lecture.contentUrl = contentUrl;
        lecture.contentType = contentType;
        lecture.sortOrder = sortOrder;
        lecture.createdAt = OffsetDateTime.now();
        return lecture;
    }

    public LectureId getId() {
        return new LectureId(id);
    }

    Long getRawId() {
        return id;
    }

    public ContentStatus getStatus() {
        return status;
    }

    public Title getTitle() {
        return title;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public int getSortOrder() {
        return sortOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    void update(Title newTitle, String contentUrl, String contentType) {
        checkNotDeleted();
        if (course.getStatus() == ContentStatus.PUBLIC && status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 강의를 수정할 수 없습니다.");
        }
        if (newTitle == null) {
            throw new CourseException("제목은 null일 수 없습니다.");
        }
        if (contentUrl == null || contentUrl.isBlank()) {
            throw new CourseException("강의 자료 URL은 비어있을 수 없습니다.");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new CourseException("자료 타입은 비어있을 수 없습니다.");
        }
        this.title = newTitle;
        this.contentUrl = contentUrl;
        this.contentType = contentType;
        this.updatedAt = OffsetDateTime.now();
    }

    void publish() {
        checkNotDeleted();
        this.status = ContentStatus.PUBLIC;
        this.updatedAt = OffsetDateTime.now();
    }

    void unpublish() {
        checkNotDeleted();
        this.status = ContentStatus.PRIVATE;
        this.updatedAt = OffsetDateTime.now();
    }

    void delete() {
        checkNotDeleted();
        this.deletedAt = OffsetDateTime.now();
    }

    void assignSortOrder(int sortOrder) {
        checkNotDeleted();
        this.sortOrder = sortOrder;
        this.updatedAt = OffsetDateTime.now();
    }

    private void checkNotDeleted() {
        if (deletedAt != null) {
            throw new CourseException("삭제된 강의는 수정할 수 없습니다.");
        }
    }
}
