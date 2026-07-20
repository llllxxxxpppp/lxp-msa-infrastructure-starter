package com.lcs.course.domain.model.entity;

import com.lcs.course.domain.exception.CourseException;
import com.lcs.course.domain.model.vo.ContentStatus;
import com.lcs.course.domain.model.vo.CourseId;
import com.lcs.course.domain.model.vo.InstructorId;
import com.lcs.course.domain.model.vo.LectureId;
import com.lcs.course.domain.model.vo.MissionId;
import com.lcs.course.domain.model.vo.ReorderItem;
import com.lcs.course.domain.model.vo.Sortable;
import com.lcs.course.domain.model.vo.SortableType;
import com.lcs.course.domain.model.vo.Title;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "instructor_id", nullable = false)
    private Long instructorId;

    @Embedded
    private Title title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentStatus status;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lecture> lectures = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Mission> missions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime updatedAt;

    @Column
    private OffsetDateTime deletedAt;

    private static final int MAX_DESCRIPTION_LENGTH = 4096;

    protected Course() {}

    public static Course create(InstructorId instructorId, Title title, String description, String thumbnailUrl) {
        if (instructorId == null) {
            throw new CourseException("강사 ID는 null일 수 없습니다.");
        }
        if (title == null) {
            throw new CourseException("제목은 null일 수 없습니다.");
        }
        if (description == null || description.isBlank()) {
            throw new CourseException("설명은 비어있을 수 없습니다.");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new CourseException("설명은 4096자를 초과할 수 없습니다.");
        }
        Course course = new Course();
        course.instructorId = instructorId.value();
        course.title = title;
        course.description = description;
        course.thumbnailUrl = thumbnailUrl;
        course.status = ContentStatus.PRIVATE;
        course.createdAt = OffsetDateTime.now();
        return course;
    }

    public CourseId getId() {
        return new CourseId(id);
    }

    public InstructorId getInstructorId() {
        return new InstructorId(instructorId);
    }

    public ContentStatus getStatus() {
        return status;
    }

    public Title getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
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

    public List<Lecture> getLectures() {
        return lectures.stream()
                .filter(lecture -> !lecture.isDeleted())
                .sorted(Comparator.comparingInt(Lecture::getSortOrder))
                .toList();
    }

    public List<Mission> getMissions() {
        return missions.stream()
                .filter(mission -> !mission.isDeleted())
                .sorted(Comparator.comparingInt(Mission::getSortOrder))
                .toList();
    }

    public List<Sortable> getSortableItems() {
        List<Sortable> items = new ArrayList<>();
        items.addAll(getLectures());
        items.addAll(getMissions());
        return items.stream()
                .sorted(Comparator.comparingInt(Sortable::getSortOrder))
                .toList();
    }

    public void update(Title newTitle, String description, String thumbnailUrl) {
        checkNotDeleted();
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 강좌를 수정할 수 없습니다.");
        }
        if (newTitle == null) {
            throw new CourseException("제목은 null일 수 없습니다.");
        }
        if (description == null || description.isBlank()) {
            throw new CourseException("설명은 비어있을 수 없습니다.");
        }
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new CourseException("설명은 4096자를 초과할 수 없습니다.");
        }
        this.title = newTitle;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.updatedAt = OffsetDateTime.now();
    }

    public Lecture addLecture(Title lectureTitle, String contentUrl, String contentType) {
        checkNotDeleted();
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 강의를 추가할 수 없습니다.");
        }
        Lecture lecture = Lecture.create(this, lectureTitle, contentUrl, contentType, nextSortOrder());
        lectures.add(lecture);
        return lecture;
    }

    public Mission addMission(Title missionTitle, String content) {
        checkNotDeleted();
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 미션을 추가할 수 없습니다.");
        }
        Mission mission = Mission.create(this, missionTitle, content, nextSortOrder());
        missions.add(mission);
        return mission;
    }

    private int nextSortOrder() {
        return currentMaxSortOrder() + 1;
    }

    private int currentMaxSortOrder() {
        return IntStream.concat(
                        lectures.stream().mapToInt(Lecture::getSortOrder),
                        missions.stream().mapToInt(Mission::getSortOrder))
                .max()
                .orElse(0);
    }

    private int maxSortOrderAmongDeleted() {
        return IntStream.concat(
                        lectures.stream().filter(Lecture::isDeleted).mapToInt(Lecture::getSortOrder),
                        missions.stream().filter(Mission::isDeleted).mapToInt(Mission::getSortOrder))
                .max()
                .orElse(0);
    }

    public void updateLecture(LectureId lectureId, Title newTitle, String contentUrl, String contentType) {
        checkNotDeleted();
        findLecture(lectureId).update(newTitle, contentUrl, contentType);
    }

    public void publishLecture(LectureId lectureId) {
        checkNotDeleted();
        findLecture(lectureId).publish();
    }

    public void unpublishLecture(LectureId lectureId) {
        checkNotDeleted();
        findLecture(lectureId).unpublish();
    }

    public void deleteLecture(LectureId lectureId) {
        checkNotDeleted();
        findLecture(lectureId).delete();
    }

    public void updateMission(MissionId missionId, Title newTitle, String content) {
        checkNotDeleted();
        findMission(missionId).update(newTitle, content);
    }

    public void publishMission(MissionId missionId) {
        checkNotDeleted();
        findMission(missionId).publish();
    }

    public void unpublishMission(MissionId missionId) {
        checkNotDeleted();
        findMission(missionId).unpublish();
    }

    public void deleteMission(MissionId missionId) {
        checkNotDeleted();
        findMission(missionId).delete();
    }

    public void reorder(List<ReorderItem> orderedItems) {
        checkNotDeleted();
        if (status == ContentStatus.PUBLIC) {
            throw new CourseException("공개 상태에서는 순서를 변경할 수 없습니다.");
        }
        List<Lecture> activeLectures = lectures.stream().filter(l -> !l.isDeleted()).toList();
        List<Mission> activeMissions = missions.stream().filter(m -> !m.isDeleted()).toList();
        int totalActive = activeLectures.size() + activeMissions.size();
        if (orderedItems == null || orderedItems.size() != totalActive) {
            throw new CourseException("순서 변경 대상 항목의 개수가 강좌에 속한 강의/미션 전체 개수와 일치하지 않습니다.");
        }
        Set<ReorderItem> uniqueItems = new HashSet<>(orderedItems);
        if (uniqueItems.size() != orderedItems.size()) {
            throw new CourseException("순서 변경 대상 목록에 중복된 항목이 있습니다.");
        }
        int order = maxSortOrderAmongDeleted();
        for (ReorderItem item : orderedItems) {
            order++;
            if (item.type() == SortableType.LECTURE) {
                findActiveLecture(activeLectures, item.id()).assignSortOrder(order);
            } else {
                findActiveMission(activeMissions, item.id()).assignSortOrder(order);
            }
        }
    }

    private Lecture findActiveLecture(List<Lecture> activeLectures, Long lectureId) {
        return activeLectures.stream()
                .filter(l -> lectureId.equals(l.getRawId()))
                .findFirst()
                .orElseThrow(() -> new CourseException("강의를 찾을 수 없습니다."));
    }

    private Mission findActiveMission(List<Mission> activeMissions, Long missionId) {
        return activeMissions.stream()
                .filter(m -> missionId.equals(m.getRawId()))
                .findFirst()
                .orElseThrow(() -> new CourseException("미션을 찾을 수 없습니다."));
    }

    private Lecture findLecture(LectureId lectureId) {
        return lectures.stream()
                .filter(l -> lectureId.value().equals(l.getRawId()))
                .findFirst()
                .orElseThrow(() -> new CourseException("강의를 찾을 수 없습니다."));
    }

    private Mission findMission(MissionId missionId) {
        return missions.stream()
                .filter(m -> missionId.value().equals(m.getRawId()))
                .findFirst()
                .orElseThrow(() -> new CourseException("미션을 찾을 수 없습니다."));
    }

    public void publish() {
        checkNotDeleted();
        if (getLectures().isEmpty() || getMissions().isEmpty()) {
            throw new CourseException("강의와 미션을 1개 이상 포함해야 공개할 수 있습니다.");
        }
        this.status = ContentStatus.PUBLIC;
        this.updatedAt = OffsetDateTime.now();
    }

    public void unpublish() {
        checkNotDeleted();
        this.status = ContentStatus.PRIVATE;
        this.updatedAt = OffsetDateTime.now();
    }

    public void delete() {
        checkNotDeleted();
        this.deletedAt = OffsetDateTime.now();
    }

    private void checkNotDeleted() {
        if (deletedAt != null) {
            throw new CourseException("삭제된 강좌는 수정할 수 없습니다.");
        }
    }
}
