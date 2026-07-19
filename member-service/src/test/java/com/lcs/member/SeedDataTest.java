package com.lcs.member;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * MEMBER-11: 로컬 실행용 시드 데이터({@code data.sql})가 애플리케이션 컨텍스트 기동 시 H2 인메모리 DB에
 * 정확히 반영되는지 검증하는 통합 성격의 테스트.
 *
 * <p>Mockito 협력자가 없는 실제 Spring 컨텍스트(내장 H2 + {@code data.sql}) 기반 테스트이므로,
 * {@code verify()}/{@code verifyNoInteractions()} 대신 JUnit assert로 검증한다
 * (MEMBER-09의 {@code HttpMemberStateChangeNotifierTest}와 동일한 선례).</p>
 */
@SpringBootTest
class SeedDataTest {

    private static final long ADMIN_SEED_ID = 1L;
    private static final long INSTRUCTOR_SEED_ID = 2L;
    private static final long MEMBER_SEED_ID = 3L;

    @Autowired
    private MemberRepository memberRepository;

    // -------------------------------------------------------------------------
    // 완료 기준 1: 강사 고정 ID(2) 시드 검증
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("data.sql이 적용되면 id=2로 시드된 회원은 프로필이 채워진 InstructorMember이다")
    void givenSeededData_whenFindInstructorById2_thenReturnsInstructorMemberWithFixedIdEmailRoleAndProfile() {
        Optional<Member> found = memberRepository.findById(INSTRUCTOR_SEED_ID);

        assertTrue(found.isPresent());
        Member member = found.get();

        InstructorMember instructor = assertInstanceOf(InstructorMember.class, member);
        assertEquals(INSTRUCTOR_SEED_ID, instructor.getId().value());
        assertEquals("instructor@lxp.local", instructor.getEmail());
        assertEquals(MemberRole.INSTRUCTOR, instructor.getRole());
        assertNotNull(instructor.getProfile());
        assertNotNull(instructor.getProfile().getName());
        assertFalse(instructor.getProfile().getName().isBlank());
        assertFalse(instructor.isDeleted());
        assertNull(instructor.getSuspendedAt());
    }

    // -------------------------------------------------------------------------
    // 완료 기준 2: 어드민(id=1)/일반 회원(id=3) 고정 ID 시드 검증
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("data.sql이 적용되면 id=1로 시드된 회원은 AdminMember이고 역할은 ADMIN이다")
    void givenSeededData_whenFindAdminById1_thenReturnsAdminMemberWithAdminRole() {
        Optional<Member> found = memberRepository.findById(ADMIN_SEED_ID);

        assertTrue(found.isPresent());
        Member member = found.get();

        AdminMember admin = assertInstanceOf(AdminMember.class, member);
        assertEquals(ADMIN_SEED_ID, admin.getId().value());
        assertEquals("admin@lxp.local", admin.getEmail());
        assertEquals(MemberRole.ADMIN, admin.getRole());
        assertFalse(admin.isDeleted());
        assertNull(admin.getSuspendedAt());
    }

    @Test
    @DisplayName("data.sql이 적용되면 id=3으로 시드된 회원은 RegularMember이고 역할은 MEMBER이다")
    void givenSeededData_whenFindRegularMemberById3_thenReturnsRegularMemberWithMemberRole() {
        Optional<Member> found = memberRepository.findById(MEMBER_SEED_ID);

        assertTrue(found.isPresent());
        Member member = found.get();

        RegularMember regularMember = assertInstanceOf(RegularMember.class, member);
        assertEquals(MEMBER_SEED_ID, regularMember.getId().value());
        assertEquals("member@lxp.local", regularMember.getEmail());
        assertEquals(MemberRole.MEMBER, regularMember.getRole());
        assertFalse(regularMember.isDeleted());
        assertNull(regularMember.getSuspendedAt());
    }

    // -------------------------------------------------------------------------
    // 완료 기준 3: RESTART WITH 1000 반영 - 신규 저장 시 고정 시드 ID와 충돌하지 않음
    // -------------------------------------------------------------------------

    @Test
    @Transactional
    @DisplayName("고정 시드(id 1~3) 이후 새 회원을 저장하면 RESTART WITH 1000이 반영되어 1000 이상의 새 ID가 발급된다")
    void givenRestartWith1000Applied_whenSavingNewRegularMember_thenAssignsIdNotConflictingWithFixedSeedIds() {
        RegularMember newMember = RegularMember.create("new-member@lxp.local", "placeholder-encoded-password");

        RegularMember saved = memberRepository.save(newMember);

        long assignedId = saved.getId().value();
        assertNotNull(assignedId);
        assertTrue(assignedId >= 1000L,
                "새로 발급된 ID는 ALTER TABLE ... RESTART WITH 1000 이 반영되어 1000 이상이어야 한다. 실제: " + assignedId);
        assertTrue(assignedId != ADMIN_SEED_ID && assignedId != INSTRUCTOR_SEED_ID && assignedId != MEMBER_SEED_ID,
                "새로 발급된 ID는 고정 시드 ID(1~3)와 충돌해서는 안 된다.");
    }
}
