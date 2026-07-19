package com.lcs.member.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * MEMBER-13: 헥사고날 전환 1단계로 신설되는 {@code infrastructure.persistence} 패키지의
 * JPA 엔티티 4종({@code MemberJpaEntity}(추상)/{@code RegularMemberJpaEntity}/
 * {@code InstructorMemberJpaEntity}/{@code AdminMemberJpaEntity})과
 * {@code MemberJpaRepository}가 임시 테이블 {@code members_staging}에 정상적으로
 * 저장/조회되는지 검증한다.
 *
 * <p>기존 도메인 {@code Member} 계층({@code members} 테이블)과는 완전히 독립된 별도 테이블을
 * 사용하므로 기존 테스트 스위트({@code SeedDataTest} 등)에 영향을 주지 않는다(완료 기준 4).</p>
 *
 * <p>Mockito 협력자가 없는 {@code @DataJpaTest} 기반 리포지토리 테스트이므로
 * {@code verify()}/{@code verifyNoInteractions()} 대신 JUnit assert로 검증한다
 * (MEMBER-09 {@code HttpMemberStateChangeNotifierTest}, MEMBER-11 {@code SeedDataTest}와 동일한 선례).</p>
 *
 * <p><b>전제한 API(구현 에이전트가 그대로 맞춰야 함):</b></p>
 * <pre>
 * abstract class MemberJpaEntity {
 *     protected MemberJpaEntity() {}
 *     protected MemberJpaEntity(String email, String password, boolean deleted,
 *             OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
 *     Long getId();
 *     String getEmail();
 *     String getPassword();
 *     boolean isDeleted();
 *     OffsetDateTime getSuspendedAt();
 *     OffsetDateTime getCreatedAt();
 *     OffsetDateTime getUpdatedAt();
 * }
 *
 * class RegularMemberJpaEntity extends MemberJpaEntity {
 *     public RegularMemberJpaEntity(String email, String password, boolean deleted,
 *             OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
 *             OffsetDateTime withdrawnAt) {}
 *     OffsetDateTime getWithdrawnAt();
 * }
 *
 * class InstructorMemberJpaEntity extends MemberJpaEntity {
 *     public InstructorMemberJpaEntity(String email, String password, boolean deleted,
 *             OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
 *             String profileName, String profileImageUrl, String profileIntroduction) {}
 *     String getProfileName();
 *     String getProfileImageUrl();
 *     String getProfileIntroduction();
 * }
 *
 * class AdminMemberJpaEntity extends MemberJpaEntity {
 *     public AdminMemberJpaEntity(String email, String password, boolean deleted,
 *             OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
 * }
 *
 * interface MemberJpaRepository extends JpaRepository&lt;MemberJpaEntity, Long&gt; {
 *     boolean existsByEmail(String email);
 *     Optional&lt;MemberJpaEntity&gt; findByEmail(String email);
 * }
 * </pre>
 * <p>InstructorMember의 프로필은 도메인 {@code InstructorProfile} VO를 {@code @Embedded}로 재사용하지 않고
 * 평범한 String 필드 3개(profileName/profileImageUrl/profileIntroduction)로 구현했다고 전제했다.
 * 구현 에이전트가 {@code @Embedded InstructorProfile}을 택할 경우 {@code getProfile().getName()} 등으로
 * 접근 방식이 달라지므로, 이 파일의 InstructorMember 관련 getter 호출부만 맞춰 조정이 필요하다.</p>
 */
@DataJpaTest
class MemberJpaRepositoryTest {

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    // -------------------------------------------------------------------------
    // 완료 기준 1, 2: 각 서브타입 저장/조회 + discriminator(role) 정확성
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("given a RegularMemberJpaEntity, when saved and reloaded, then it round-trips with the MEMBER discriminator")
    void givenRegularMemberJpaEntity_whenSavedAndReloaded_thenRoundTripsWithMemberDiscriminator() {
        RegularMemberJpaEntity newMember = newRegularMember("regular-round-trip@lxp.local");

        RegularMemberJpaEntity saved = memberJpaRepository.save(newMember);
        entityManager.flush();
        entityManager.clear();

        Optional<MemberJpaEntity> found = memberJpaRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        RegularMemberJpaEntity reloaded = assertInstanceOf(RegularMemberJpaEntity.class, found.get());
        assertEquals("regular-round-trip@lxp.local", reloaded.getEmail());
        assertEquals("encoded-password", reloaded.getPassword());
        assertFalse(reloaded.isDeleted());
        assertNull(reloaded.getSuspendedAt());
        assertNotNull(reloaded.getCreatedAt());
        assertNull(reloaded.getUpdatedAt());
        assertNull(reloaded.getWithdrawnAt());
        assertEquals("MEMBER", readRoleColumn(reloaded.getId()));
    }

    @Test
    @DisplayName("given an InstructorMemberJpaEntity, when saved and reloaded, then it round-trips with the INSTRUCTOR discriminator and profile fields preserved")
    void givenInstructorMemberJpaEntity_whenSavedAndReloaded_thenRoundTripsWithInstructorDiscriminatorAndProfilePreserved() {
        InstructorMemberJpaEntity newInstructor = newInstructorMember(
                "instructor-round-trip@lxp.local", "홍길동", "https://cdn.example.com/profile.png",
                "안녕하세요, 강사입니다.");

        InstructorMemberJpaEntity saved = memberJpaRepository.save(newInstructor);
        entityManager.flush();
        entityManager.clear();

        Optional<MemberJpaEntity> found = memberJpaRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        InstructorMemberJpaEntity reloaded = assertInstanceOf(InstructorMemberJpaEntity.class, found.get());
        assertEquals("instructor-round-trip@lxp.local", reloaded.getEmail());
        assertEquals("홍길동", reloaded.getProfileName());
        assertEquals("https://cdn.example.com/profile.png", reloaded.getProfileImageUrl());
        assertEquals("안녕하세요, 강사입니다.", reloaded.getProfileIntroduction());
        assertEquals("INSTRUCTOR", readRoleColumn(reloaded.getId()));
    }

    @Test
    @DisplayName("given an AdminMemberJpaEntity, when saved and reloaded, then it round-trips with the ADMIN discriminator")
    void givenAdminMemberJpaEntity_whenSavedAndReloaded_thenRoundTripsWithAdminDiscriminator() {
        AdminMemberJpaEntity newAdmin = newAdminMember("admin-round-trip@lxp.local");

        AdminMemberJpaEntity saved = memberJpaRepository.save(newAdmin);
        entityManager.flush();
        entityManager.clear();

        Optional<MemberJpaEntity> found = memberJpaRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        AdminMemberJpaEntity reloaded = assertInstanceOf(AdminMemberJpaEntity.class, found.get());
        assertEquals("admin-round-trip@lxp.local", reloaded.getEmail());
        assertEquals("ADMIN", readRoleColumn(reloaded.getId()));
    }

    // -------------------------------------------------------------------------
    // 완료 기준 3 보강: RegularMember 고유 컬럼(withdrawnAt)도 저장/조회 시 보존되는지 확인
    // (InstructorMember 프로필 보존 검증과 대칭)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("given a RegularMemberJpaEntity with a withdrawnAt timestamp, when saved and reloaded, then withdrawnAt is preserved")
    void givenRegularMemberJpaEntityWithWithdrawnAt_whenSavedAndReloaded_thenWithdrawnAtIsPreserved() {
        OffsetDateTime withdrawnAt = OffsetDateTime.now();
        RegularMemberJpaEntity newMember = new RegularMemberJpaEntity(
                "withdrawn-round-trip@lxp.local", "encoded-password", true, null,
                OffsetDateTime.now(), OffsetDateTime.now(), withdrawnAt);

        RegularMemberJpaEntity saved = memberJpaRepository.save(newMember);
        entityManager.flush();
        entityManager.clear();

        Optional<MemberJpaEntity> found = memberJpaRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        RegularMemberJpaEntity reloaded = assertInstanceOf(RegularMemberJpaEntity.class, found.get());
        assertTrue(reloaded.isDeleted());
        assertNotNull(reloaded.getWithdrawnAt());
    }

    // -------------------------------------------------------------------------
    // 불변식 위반 시나리오: email unique/not-null, password not-null 제약
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("given an already-used email, when saving another member with the same email, then a data integrity violation is thrown")
    void givenExistingEmail_whenSavingAnotherMemberWithSameEmail_thenThrowsDataIntegrityViolation() {
        String duplicateEmail = "duplicate@lxp.local";
        memberJpaRepository.saveAndFlush(newRegularMember(duplicateEmail));

        RegularMemberJpaEntity another = newRegularMember(duplicateEmail);

        assertThrows(DataIntegrityViolationException.class,
                () -> memberJpaRepository.saveAndFlush(another));
    }

    @Test
    @DisplayName("given a member with a null email, when saved, then a data integrity violation is thrown due to the not-null constraint")
    void givenNullEmail_whenSaved_thenThrowsDataIntegrityViolation() {
        RegularMemberJpaEntity invalidMember = new RegularMemberJpaEntity(
                null, "encoded-password", false, null, OffsetDateTime.now(), null, null);

        assertThrows(DataIntegrityViolationException.class,
                () -> memberJpaRepository.saveAndFlush(invalidMember));
    }

    @Test
    @DisplayName("given a member with a null password, when saved, then a data integrity violation is thrown due to the not-null constraint")
    void givenNullPassword_whenSaved_thenThrowsDataIntegrityViolation() {
        RegularMemberJpaEntity invalidMember = new RegularMemberJpaEntity(
                "null-password@lxp.local", null, false, null, OffsetDateTime.now(), null, null);

        assertThrows(DataIntegrityViolationException.class,
                () -> memberJpaRepository.saveAndFlush(invalidMember));
    }

    // -------------------------------------------------------------------------
    // 리포지토리 부가 메서드(existsByEmail/findByEmail) 검증 - 완료 기준 외 부가 커버리지
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("given a saved member, when existsByEmail is called, then it returns true only for the saved email")
    void givenSavedMember_whenExistsByEmail_thenReturnsTrueOnlyForSavedEmail() {
        memberJpaRepository.saveAndFlush(newRegularMember("exists-check@lxp.local"));

        assertTrue(memberJpaRepository.existsByEmail("exists-check@lxp.local"));
        assertFalse(memberJpaRepository.existsByEmail("not-exists@lxp.local"));
    }

    @Test
    @DisplayName("given a saved member, when findByEmail is called, then it returns the matching entity and empty for an unknown email")
    void givenSavedMember_whenFindByEmail_thenReturnsMatchingEntity() {
        RegularMemberJpaEntity saved =
                memberJpaRepository.saveAndFlush(newRegularMember("find-by-email@lxp.local"));
        entityManager.clear();

        Optional<MemberJpaEntity> found = memberJpaRepository.findByEmail("find-by-email@lxp.local");
        Optional<MemberJpaEntity> notFound = memberJpaRepository.findByEmail("nobody@lxp.local");

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertTrue(notFound.isEmpty());
    }

    // -------------------------------------------------------------------------
    // 헬퍼
    // -------------------------------------------------------------------------

    private RegularMemberJpaEntity newRegularMember(String email) {
        return new RegularMemberJpaEntity(
                email, "encoded-password", false, null, OffsetDateTime.now(), null, null);
    }

    private InstructorMemberJpaEntity newInstructorMember(
            String email, String profileName, String profileImageUrl, String profileIntroduction) {
        return new InstructorMemberJpaEntity(
                email, "encoded-password", false, null, OffsetDateTime.now(), null,
                profileName, profileImageUrl, profileIntroduction);
    }

    private AdminMemberJpaEntity newAdminMember(String email) {
        return new AdminMemberJpaEntity(
                email, "encoded-password", false, null, OffsetDateTime.now(), null);
    }

    private String readRoleColumn(Long memberId) {
        Object role = entityManager.getEntityManager()
                .createNativeQuery("SELECT role FROM members_staging WHERE id = :id")
                .setParameter("id", memberId)
                .getSingleResult();
        return String.valueOf(role);
    }
}
