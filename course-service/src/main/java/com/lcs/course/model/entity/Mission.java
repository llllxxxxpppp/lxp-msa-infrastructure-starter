package com.lcs.course.model.entity;

import com.lcs.course.exception.CourseException;
import com.lcs.course.model.vo.ContentStatus;
import com.lcs.course.model.vo.MissionId;
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
@Table(name = "missions")
public class Mission implements Sortable {

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

    @Column(columnDefinition = "TEXT", length = 4096)
    private String content;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime deletedAt;

    private static final int MAX_CONTENT_LENGTH = 4096;

    protected Mission() {}

    static Mission create(Course course, Title title, String content, int sortOrder) {
        if (course == null) {
            throw new CourseException("강좌는 null일 수 없습니다.");
        }
        if (title == null) {
            throw new CourseException("제목은 null일 수 없습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new CourseException("문제 내용은 비어있을 수 없습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new CourseException("문제 내용은 4096자를 초과할 수 없습니다.");
        }
        Mission mission = new Mission();
        mission.course = course;
        mission.title = title;
        mission.content = content;
        mission.status = ContentStatus.PUBLIC;
        mission.sortOrder = sortOrder;
        mission.createdAt = OffsetDateTime.now();
        return mission;
    }

    public MissionId getId() {
        return new MissionId(id);
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

    public String getContent() {
        return content;
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

    void update(Title newTitle, String content) {
        checkNotDeleted();
        if (course.getStatus() == ContentStatus.PUBLIC && status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 미션을 수정할 수 없습니다.");
        }
        if (newTitle == null) {
            throw new CourseException("제목은 null일 수 없습니다.");
        }
        if (content == null || content.isBlank()) {
            throw new CourseException("문제 내용은 비어있을 수 없습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new CourseException("문제 내용은 4096자를 초과할 수 없습니다.");
        }
        this.title = newTitle;
        this.content = content;
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
            throw new CourseException("삭제된 미션은 수정할 수 없습니다.");
        }
    }
}
