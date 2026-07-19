package com.lcs.member.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lcs.member.domain.model.MemberRole;
import com.lcs.member.domain.model.entity.AdminMember;
import com.lcs.member.domain.model.entity.InstructorMember;
import com.lcs.member.domain.model.entity.Member;
import com.lcs.member.domain.model.entity.RegularMember;
import com.lcs.member.domain.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * MEMBER-14+15: 헥사고날 전환 2단계 - {@code domain.repository.MemberRepository}가
 * {@code JpaRepository} 상속 없는 순수 포트 인터페이스로 재정의되고, 실제 영속성은
 * {@link MemberRepositoryAdapter}가 MEMBER-13의 {@link MemberJpaRepository}/JPA 엔티티
 * 4종을 사용해 담당하도록 배선이 교체된다. 이 테스트는 그 배선(포트 → 어댑터 → 실제 H2)이
 * 왕복(round-trip)으로 정상 동작하는지 검증한다.
 *
 * <p>순수 POJO가 된 도메인 {@code Member}는 더 이상 Hibernate가 {@code id} 필드를 채워주지
 * 않으므로, 아직 저장되지 않은 도메인 객체에 대해 {@code getId()}를 호출하면 {@code MemberId}
 * record의 {@code Objects.requireNonNull} 때문에 {@link NullPointerException}이 발생한다.
 * 어댑터는 이 대신 {@code isPersisted()}(id != null)로 신규/기존 여부를 판별해
 * insert/update를 분기한다.</p>
 *
 * <p>Mockito 협력자가 없는 {@code @DataJpaTest} 기반 통합 테스트이므로
 * {@code verify()}/{@code verifyNoInteractions()} 대신 JUnit assert로 검증한다
 * (MEMBER-13 {@code MemberJpaRepositoryTest}, MEMBER-11 {@code SeedDataTest}와 동일한 선례).</p>
 *
 * <p><b>전제한 API(구현 에이전트가 그대로 맞춰야 함):</b></p>
 * <pre>
 * // domain.model.entity.Member (추가분)
 * public boolean isPersisted() { return id != null; }
 * protected Member(Long id, String email, String password, boolean deleted,
 *         OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
 *
 * // domain.model.entity.RegularMember
 * public static RegularMember reconstitute(Long id, String email, String password, boolean deleted,
 *         OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
 *         OffsetDateTime withdrawnAt) {}
 *
 * // domain.model.entity.InstructorMember
 * public static InstructorMember reconstitute(Long id, String email, String password, boolean deleted,
 *         OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
 *         String profileName, String profileImageUrl, String profileIntroduction) {}
 *
 * // domain.model.entity.AdminMember
 * public static AdminMember reconstitute(Long id, String email, String password, boolean deleted,
 *         OffsetDateTime suspendedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
 *
 * // domain.repository.MemberRepository (더 이상 JpaRepository를 상속하지 않는 순수 포트)
 * public interface MemberRepository {
 *     &lt;T extends Member&gt; T save(T member);
 *     Optional&lt;Member&gt; findById(Long id);
 *     boolean existsByEmail(String email);
 *     Optional&lt;Member&gt; findByEmail(String email);
 * }
 * // save가 제네릭(&lt;T extends Member&gt; T save(T member))이어야 하는 이유:
 * // MemberServiceTest/SeedDataTest가 "RegularMember saved = memberRepository.save(regularMember);"처럼
 * // 서브타입을 그대로 대입받는 호출부를 그대로 유지한 채(수정 금지) 통과해야 하기 때문
 * // (기존 JpaRepository.save의 시그니처와 동일한 형태를 포트 인터페이스에 직접 선언).
 *
 * // infrastructure.persistence.MemberRepositoryAdapter
 * &#64;Repository
 * public class MemberRepositoryAdapter implements MemberRepository {
 *     public MemberRepositoryAdapter(MemberJpaRepository memberJpaRepository) {}
 *     // save: member.isPersisted()==false -> id 없는 JPA 엔티티 생성 후 save (insert)
 *     //       member.isPersisted()==true  -> JPA 엔티티 생성 후 setId(member.getId().value()) 호출, save (update)
 *     //       두 경우 모두 저장된 JPA 엔티티를 reconstitute(...)로 변환해 반환
 * }
 *
 * // infrastructure.persistence.MemberJpaEntity: package-private setId(Long id) 추가
 * </pre>
 */
@DataJpaTest
@Import(MemberRepositoryAdapter.class)
class MemberRepositoryAdapterTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberJpaRepository memberJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    // -------------------------------------------------------------------------
    // 불변식: 저장 전 도메인 객체는 isPersisted()==false이고 getId() 호출 시 예외가 발생한다
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("given a newly created RegularMember that has not been saved, when isPersisted and getId are checked, then isPersisted is false and getId throws NullPointerException")
    void givenNewRegularMember_whenCheckedBeforeSave_thenIsPersistedIsFalseAndGetIdThrows() {
        RegularMember newMember = RegularMember.create("not-yet-saved@lxp.local", "encoded-password");

        assertFalse(newMember.isPersisted());
        assertThrows(NullPointerException.class, newMember::getId);
    }

    // -------------------------------------------------------------------------
    // 완료 기준 3: 신규 RegularMember/InstructorMember/AdminMember 각각 save 후
    // 반환된 도메인 객체의 id가 채워져 있고, findById 재조회 시 동일 서브타입/필드 값을 갖는다
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("given a new RegularMember, when saved, then the returned member is persisted with an assigned id and round-trips via findById")
    void givenNewRegularMember_whenSaved_thenIdIsAssignedAndRoundTripsViaFindById() {
        RegularMember newMember = RegularMember.create("regular-adapter@lxp.local", "encoded-password");

        RegularMember saved = memberRepository.save(newMember);
        entityManager.flush();
        entityManager.clear();

        assertTrue(saved.isPersisted());
        assertNotNull(saved.getId().value());

        Optional<Member> found = memberRepository.findById(saved.getId().value());

        assertTrue(found.isPresent());
        RegularMember reloaded = assertInstanceOf(RegularMember.class, found.get());
        assertEquals("regular-adapter@lxp.local", reloaded.getEmail());
        assertEquals(MemberRole.MEMBER, reloaded.getRole());
        assertFalse(reloaded.isDeleted());
        assertNull(reloaded.getSuspendedAt());
        assertNotNull(reloaded.getCreatedAt());
        assertNull(reloaded.getUpdatedAt());
        assertNull(reloaded.getWithdrawnAt());
    }

    @Test
    @DisplayName("given a new InstructorMember, when saved, then the returned member is persisted with an assigned id and round-trips via findById with the profile preserved")
    void givenNewInstructorMember_whenSaved_thenIdIsAssignedAndRoundTripsWithProfilePreserved() {
        InstructorMember newInstructor = InstructorMember.create(
                "instructor-adapter@lxp.local", "encoded-password", "홍길동",
                "https://cdn.example.com/profile.png", "안녕하세요, 강사입니다.");

        InstructorMember saved = memberRepository.save(newInstructor);
        entityManager.flush();
        entityManager.clear();

        assertTrue(saved.isPersisted());
        assertNotNull(saved.getId().value());

        Optional<Member> found = memberRepository.findById(saved.getId().value());

        assertTrue(found.isPresent());
        InstructorMember reloaded = assertInstanceOf(InstructorMember.class, found.get());
        assertEquals("instructor-adapter@lxp.local", reloaded.getEmail());
        assertEquals(MemberRole.INSTRUCTOR, reloaded.getRole());
        assertNotNull(reloaded.getProfile());
        assertEquals("홍길동", reloaded.getProfile().getName());
        assertEquals("https://cdn.example.com/profile.png", reloaded.getProfile().getProfileImageUrl());
        assertEquals("안녕하세요, 강사입니다.", reloaded.getProfile().getIntroduction());
        assertFalse(reloaded.isDeleted());
        assertNull(reloaded.getSuspendedAt());
    }

    @Test
    @DisplayName("given a new AdminMember, when saved, then the returned member is persisted with an assigned id and round-trips via findById")
    void givenNewAdminMember_whenSaved_thenIdIsAssignedAndRoundTripsViaFindById() {
        AdminMember newAdmin = AdminMember.create("admin-adapter@lxp.local", "encoded-password");

        AdminMember saved = memberRepository.save(newAdmin);
        entityManager.flush();
        entityManager.clear();

        assertTrue(saved.isPersisted());
        assertNotNull(saved.getId().value());

        Optional<Member> found = memberRepository.findById(saved.getId().value());

        assertTrue(found.isPresent());
        AdminMember reloaded = assertInstanceOf(AdminMember.class, found.get());
        assertEquals("admin-adapter@lxp.local", reloaded.getEmail());
        assertEquals(MemberRole.ADMIN, reloaded.getRole());
        assertFalse(reloaded.isDeleted());
        assertNull(reloaded.getSuspendedAt());
    }

    // -------------------------------------------------------------------------
    // 완료 기준 3: 기존(이미 저장된) 도메인 객체를 필드 변경 후 다시 save()하면
    // 새 행이 추가되지 않고 기존 행이 갱신된다
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("given an already-persisted RegularMember, when suspended and saved again, then no new row is inserted and the existing row is updated instead")
    void givenPersistedRegularMember_whenSuspendedAndSavedAgain_thenExistingRowIsUpdatedNotInserted() {
        RegularMember newMember = RegularMember.create("update-adapter@lxp.local", "encoded-password");
        RegularMember saved = memberRepository.save(newMember);
        entityManager.flush();
        entityManager.clear();
        long memberId = saved.getId().value();
        long countAfterInsert = memberJpaRepository.count();

        Member toUpdate = memberRepository.findById(memberId).orElseThrow();
        RegularMember regularMemberToUpdate = assertInstanceOf(RegularMember.class, toUpdate);
        regularMemberToUpdate.suspend();

        RegularMember updated = memberRepository.save(regularMemberToUpdate);
        entityManager.flush();
        entityManager.clear();

        assertEquals(memberId, updated.getId().value());
        assertEquals(countAfterInsert, memberJpaRepository.count());

        Member reloaded = memberRepository.findById(memberId).orElseThrow();
        RegularMember reloadedRegularMember = assertInstanceOf(RegularMember.class, reloaded);
        assertTrue(reloadedRegularMember.isDeleted());
        assertNotNull(reloadedRegularMember.getSuspendedAt());
        assertNotNull(reloadedRegularMember.getUpdatedAt());
    }

    // -------------------------------------------------------------------------
    // 완료 기준 3: findByEmail/existsByEmail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("given a saved member, when existsByEmail is called, then it returns true only for the saved email")
    void givenSavedMember_whenExistsByEmail_thenReturnsTrueOnlyForSavedEmail() {
        memberRepository.save(RegularMember.create("exists-check-adapter@lxp.local", "encoded-password"));
        entityManager.flush();

        assertTrue(memberRepository.existsByEmail("exists-check-adapter@lxp.local"));
        assertFalse(memberRepository.existsByEmail("not-exists-adapter@lxp.local"));
    }

    @Test
    @DisplayName("given a saved member, when findByEmail is called, then it returns the matching member and empty for an unknown email")
    void givenSavedMember_whenFindByEmail_thenReturnsMatchingMemberAndEmptyForUnknownEmail() {
        RegularMember saved = memberRepository.save(
                RegularMember.create("find-by-email-adapter@lxp.local", "encoded-password"));
        entityManager.flush();
        entityManager.clear();

        Optional<Member> found = memberRepository.findByEmail("find-by-email-adapter@lxp.local");
        Optional<Member> notFound = memberRepository.findByEmail("nobody-adapter@lxp.local");

        assertTrue(found.isPresent());
        assertEquals(saved.getId().value(), found.get().getId().value());
        assertTrue(notFound.isEmpty());
    }
}
