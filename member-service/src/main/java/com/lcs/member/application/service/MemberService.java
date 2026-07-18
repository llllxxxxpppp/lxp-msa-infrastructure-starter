package com.lcs.member.application.service;

import com.lcs.member.application.dto.response.CreateMemberResponse;
import com.lcs.member.application.dto.response.MemberAuthStatusResponse;
import com.lcs.member.application.dto.response.MemberCredentialResponse;
import com.lcs.member.application.dto.response.SuspensionStatusResponse;
import com.lcs.member.application.dto.response.UserResponseDTO;
import com.lcs.member.domain.event.InstructorSuspendedEvent;
import com.lcs.member.domain.event.MemberRegisteredEvent;
import com.lcs.member.domain.event.MemberSuspendedEvent;
import com.lcs.member.domain.event.MemberWithdrawnEvent;
import com.lcs.member.domain.exception.MemberException;
import com.lcs.member.domain.model.entity.InstructorMember;
import com.lcs.member.domain.model.entity.Member;
import com.lcs.member.domain.model.entity.RegularMember;
import com.lcs.member.domain.repository.MemberRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {
    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MemberService(
            PasswordEncoder passwordEncoder,
            MemberRepository memberRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.passwordEncoder = passwordEncoder;
        this.memberRepository = memberRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public UserResponseDTO register(String email, String password) {
        ensureEmailNotTaken(email);

        RegularMember member = RegularMember.create(email, passwordEncoder.encode(password));

        Member savedUser = memberRepository.save(member);

        eventPublisher.publishEvent(new MemberRegisteredEvent(savedUser.getId().value()));

        return UserResponseDTO.from(savedUser);
    }

    @Transactional
    public UserResponseDTO registerInstructor(
            String email,
            String password,
            String name,
            String profileImageUrl,
            String introduction
    ) {
        ensureEmailNotTaken(email);

        InstructorMember member = InstructorMember.create(
                email, passwordEncoder.encode(password), name, profileImageUrl, introduction);

        Member savedUser = memberRepository.save(member);

        return UserResponseDTO.from(savedUser);
    }

    @Transactional
    public void suspendMember(Long memberId) {
        RegularMember regularMember = getRegularMemberOrThrow(memberId);
        regularMember.suspend();
        memberRepository.save(regularMember);

        eventPublisher.publishEvent(new MemberSuspendedEvent(memberId));
    }

    @Transactional
    public void withdrawMember(Long memberId) {
        RegularMember regularMember = getRegularMemberOrThrow(memberId);
        regularMember.withdraw();
        memberRepository.save(regularMember);

        eventPublisher.publishEvent(new MemberWithdrawnEvent(memberId));
    }

    @Transactional
    public void suspendInstructor(Long instructorId) {
        InstructorMember instructorMember = getInstructorMemberOrThrow(instructorId);
        instructorMember.suspend();
        memberRepository.save(instructorMember);

        eventPublisher.publishEvent(new InstructorSuspendedEvent(instructorId));
    }

    @Transactional
    public void changePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = getMemberOrThrow(memberId);

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new MemberException("현재 비밀번호가 일치하지 않습니다.");
        }

        member.updatePassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    @Transactional
    public UserResponseDTO updateInstructorProfile(
            Long memberId,
            String name,
            String profileImageUrl,
            String introduction
    ) {
        InstructorMember instructorMember = getInstructorMemberOrThrow(memberId);

        instructorMember.updateProfile(name, profileImageUrl, introduction);
        Member savedMember = memberRepository.save(instructorMember);

        return UserResponseDTO.from(savedMember);
    }

    @Transactional
    public CreateMemberResponse createFromHash(String email, String passwordHash) {
        ensureEmailNotTaken(email);

        RegularMember member = RegularMember.create(email, passwordHash);

        Member savedMember = memberRepository.save(member);

        return CreateMemberResponse.from(savedMember);
    }

    @Transactional(readOnly = true)
    public MemberCredentialResponse findByEmailForAuth(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException("존재하지 않는 회원입니다."));

        return MemberCredentialResponse.from(member);
    }

    @Transactional(readOnly = true)
    public MemberAuthStatusResponse getAuthStatus(Long memberId) {
        Member member = getMemberOrThrow(memberId);

        return MemberAuthStatusResponse.from(member);
    }

    @Transactional(readOnly = true)
    public SuspensionStatusResponse getSuspensionStatus(Long memberId) {
        Member member = getMemberOrThrow(memberId);

        return SuspensionStatusResponse.from(member);
    }

    private void ensureEmailNotTaken(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new MemberException("이미 사용 중인 이메일 입니다.");
        }
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException("존재하지 않는 회원입니다."));
    }

    private RegularMember getRegularMemberOrThrow(Long memberId) {
        if (getMemberOrThrow(memberId) instanceof RegularMember regularMember) {
            return regularMember;
        }
        throw new MemberException("일반 회원이 아닙니다.");
    }

    private InstructorMember getInstructorMemberOrThrow(Long memberId) {
        if (getMemberOrThrow(memberId) instanceof InstructorMember instructorMember) {
            return instructorMember;
        }
        throw new MemberException("강사가 아닙니다.");
    }
}
