"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import * as memberApi from "@/features/member/api";
import { useLogout } from "@/features/auth/hooks";
import { getAccessToken, clearTokens } from "@/lib/token-storage";
import { decodeAccessToken, getRoles } from "@/lib/jwt";
import { ApiError } from "@/types/api";
import { Card } from "@/components/ui/Card";
import { Chip } from "@/components/ui/Chip";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Avatar } from "@/components/ui/Avatar";
import { ProgressBar } from "@/components/ui/ProgressBar";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

// MOCK: 학습 이력/뱃지/통계 API가 없다. 준비되면 features/course 또는 별도 learning-history
// feature에 실제 endpoint를 추가하고 이 배열들을 교체한다.
const MOCK_STATS = { coursesCompleted: 24, learningHours: 148 };
const MOCK_HISTORY = [
  {
    title: "Advanced Compliance Regulations 2024",
    status: "completed" as const,
    detail: "Completed on Oct 12, 2024",
  },
  { title: "Inclusive Leadership Seminars", status: "in-progress" as const, progress: 80 },
];
const MOCK_BADGES = [
  { title: "Compliance Master", icon: "military_tech" },
  { title: "Fast Learner", icon: "bolt" },
  { title: "Team Player", icon: "groups" },
  { title: "Perfect Score", icon: "workspace_premium" },
];

export default function MembersPage() {
  const router = useRouter();
  const logout = useLogout();

  // 마이페이지 조회 API가 없어 accessToken payload에서 email/role만 표시용으로 읽는다.
  const [payload] = useState(() => {
    const token = getAccessToken();
    return token ? decodeAccessToken(token) : null;
  });
  const email = payload?.sub ?? "알 수 없음";
  const roles = getRoles(payload);
  const isInstructor = roles.some((role) => role.includes("INSTRUCTOR"));

  return (
    <div className="gap-stack-lg flex flex-col">
      <ProfileHeaderSection email={email} roles={roles} />

      <div className="gap-gutter grid grid-cols-1">
        <div className="gap-stack-lg flex flex-col">
          {/* <LearningHistorySection /> */}
          <PersonalInfoSection email={email} />
          {isInstructor && <InstructorProfileSection />}
          <DangerZoneSection
            onWithdrawn={async () => {
              await logout().catch(() => {});
              clearTokens();
              router.push("/");
            }}
          />
        </div>
        {/* <div className="lg:col-span-1">
          <BadgesSection />
        </div> */}
      </div>
    </div>
  );
}

function ProfileHeaderSection({ email, roles }: { email: string; roles: string[] }) {
  return (
    <div className="gap-gutter grid grid-cols-1">
      <div className="gap-stack-lg border-outline-variant bg-surface-container-lowest p-stack-lg flex flex-col items-center rounded-xl border shadow-sm md:flex-row md:items-start">
        <Avatar
          label={email}
          className="border-surface text-headline-lg h-32 w-32 shrink-0 border-4 shadow-md"
        />
        <div className="flex-1 text-center md:text-left">
          <h1 className="mb-stack-sm text-headline-lg-mobile text-on-surface md:text-headline-lg">
            {email}
          </h1>
          <div className="gap-stack-sm flex flex-wrap justify-center md:justify-start">
            {roles.length === 0 && <Chip>MEMBER</Chip>}
            {roles.map((role) => (
              <Chip key={role} tone="primary">
                {role.replace("ROLE_", "")}
              </Chip>
            ))}
          </div>
        </div>
      </div>

      {/* <div className="gap-gutter grid grid-rows-2 md:col-span-4">
        <StatCard label="Courses Completed" value={MOCK_STATS.coursesCompleted} icon="school" />
        <StatCard label="Learning Hours" value={MOCK_STATS.learningHours} icon="timer" />
      </div> */}
    </div>
  );
}

function StatCard({ label, value, icon }: { label: string; value: number; icon: string }) {
  return (
    <Card className="p-stack-md relative flex items-center justify-between overflow-hidden">
      <div className="relative z-10">
        <p className="text-label-sm text-slate-text mb-1 tracking-wider uppercase">{label}</p>
        <p className="text-headline-lg text-primary">{value}</p>
      </div>
      <MaterialIcon
        name={icon}
        className="text-primary-fixed-dim absolute right-4 -bottom-2 z-0 text-5xl opacity-30"
      />
    </Card>
  );
}

function LearningHistorySection() {
  return (
    <Card className="p-stack-lg">
      <h2 className="mb-stack-md border-surface-container pb-stack-sm text-headline-sm text-on-surface border-b">
        Learning History
      </h2>
      {/* MOCK: 학습 이력 API 없음. 준비되면 실제 데이터로 교체 */}
      <div className="gap-stack-md flex flex-col">
        {MOCK_HISTORY.map((item) => (
          <div key={item.title} className="p-stack-sm flex items-center justify-between rounded-lg">
            <div className="gap-stack-md flex items-center">
              <div className="bg-primary-fixed text-on-primary-fixed flex h-12 w-12 items-center justify-center rounded-lg">
                <MaterialIcon name="menu_book" />
              </div>
              <div>
                <h3 className="text-label-md text-on-surface">{item.title}</h3>
                {item.status === "completed" ? (
                  <p className="text-body-sm text-slate-text">{item.detail}</p>
                ) : (
                  <p className="text-body-sm text-slate-text">In Progress ({item.progress}%)</p>
                )}
              </div>
            </div>
            {item.status === "completed" ? (
              <Chip tone="success">Completed</Chip>
            ) : (
              <ProgressBar value={item.progress} className="w-24" />
            )}
          </div>
        ))}
      </div>
    </Card>
  );
}

function PersonalInfoSection({ email }: { email: string }) {
  const [isEditing, setIsEditing] = useState(false);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSaving(true);
    setMessage(null);
    try {
      await memberApi.changePassword({ currentPassword, newPassword });
      setMessage({ type: "success", text: "비밀번호가 변경되었습니다." });
      setCurrentPassword("");
      setNewPassword("");
      setIsEditing(false);
    } catch (err) {
      setMessage({
        type: "error",
        text: err instanceof ApiError ? err.message : "비밀번호 변경에 실패했습니다.",
      });
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Card className="p-stack-lg">
      <div className="mb-stack-md border-surface-container pb-stack-sm flex items-center justify-between border-b">
        <h2 className="text-headline-sm text-on-surface">Personal Information</h2>
        <button
          type="button"
          onClick={() => setIsEditing((v) => !v)}
          className="text-label-sm text-secondary flex items-center gap-1 hover:underline"
        >
          <MaterialIcon name="edit" className="text-[16px]" />
          {isEditing ? "취소" : "비밀번호 변경"}
        </button>
      </div>

      <div className="mb-stack-md">
        <p className="text-label-sm text-slate-text mb-1">Email Address</p>
        <p className="text-body-md text-on-surface">{email}</p>
      </div>

      {isEditing && (
        <form
          onSubmit={handleSubmit}
          className="gap-stack-md border-surface-container pt-stack-md flex flex-col border-t"
        >
          <Input
            id="currentPassword"
            type="password"
            label="현재 비밀번호"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
          />
          <Input
            id="newPassword"
            type="password"
            label="새 비밀번호"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            minLength={6}
          />
          <Button type="submit" disabled={isSaving} className="w-fit">
            {isSaving ? "저장 중..." : "변경하기"}
          </Button>
        </form>
      )}

      {message && (
        <p
          className={`mt-stack-sm text-body-sm ${message.type === "success" ? "text-success-green" : "text-error-red"}`}
        >
          {message.text}
        </p>
      )}
    </Card>
  );
}

function InstructorProfileSection() {
  const [name, setName] = useState("");
  const [profileImageUrl, setProfileImageUrl] = useState("");
  const [introduction, setIntroduction] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [message, setMessage] = useState<{ type: "success" | "error"; text: string } | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSaving(true);
    setMessage(null);
    try {
      await memberApi.updateInstructorProfile({
        name,
        profileImageUrl: profileImageUrl || undefined,
        introduction: introduction || undefined,
      });
      setMessage({ type: "success", text: "강사 프로필이 저장되었습니다." });
    } catch (err) {
      setMessage({
        type: "error",
        text: err instanceof ApiError ? err.message : "저장에 실패했습니다.",
      });
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Card className="p-stack-lg">
      <h2 className="mb-stack-md border-surface-container pb-stack-sm text-headline-sm text-on-surface border-b">
        강사 프로필
      </h2>
      <form onSubmit={handleSubmit} className="gap-stack-md flex flex-col">
        <Input
          id="instructorName"
          label="이름"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <Input
          id="profileImageUrl"
          label="프로필 이미지 URL"
          value={profileImageUrl}
          onChange={(e) => setProfileImageUrl(e.target.value)}
        />
        <Input
          id="introduction"
          label="소개"
          value={introduction}
          onChange={(e) => setIntroduction(e.target.value)}
        />
        <Button type="submit" disabled={isSaving} className="w-fit">
          {isSaving ? "저장 중..." : "저장"}
        </Button>
        {message && (
          <p
            className={`text-body-sm ${message.type === "success" ? "text-success-green" : "text-error-red"}`}
          >
            {message.text}
          </p>
        )}
      </form>
    </Card>
  );
}

function BadgesSection() {
  return (
    <Card className="p-stack-lg h-full">
      <h2 className="mb-stack-md border-surface-container pb-stack-sm text-headline-sm text-on-surface border-b">
        Achievement Badges
      </h2>
      {/* MOCK: 뱃지 API 없음. 준비되면 실제 데이터로 교체 */}
      <div className="gap-stack-md grid grid-cols-2">
        {MOCK_BADGES.map((badge) => (
          <div
            key={badge.title}
            className="border-surface-container p-stack-md hover:bg-surface-container-low flex flex-col items-center justify-center rounded-lg border text-center transition-colors"
          >
            <div className="mb-stack-sm from-warning-amber to-tertiary-fixed-dim flex h-16 w-16 items-center justify-center rounded-full bg-gradient-to-br shadow-sm">
              <MaterialIcon name={badge.icon} className="text-3xl text-white" />
            </div>
            <h4 className="text-label-sm text-on-surface leading-tight">{badge.title}</h4>
          </div>
        ))}
      </div>
    </Card>
  );
}

function DangerZoneSection({ onWithdrawn }: { onWithdrawn: () => void }) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleWithdraw() {
    if (!window.confirm("정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.")) return;
    setIsSubmitting(true);
    setError(null);
    try {
      await memberApi.withdraw();
      onWithdrawn();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "탈퇴 처리에 실패했습니다.");
      setIsSubmitting(false);
    }
  }

  return (
    <Card className="p-stack-lg">
      <h2 className="mb-stack-md text-headline-sm text-on-surface">계정 탈퇴</h2>
      <p className="mb-stack-md text-body-sm text-slate-text">
        탈퇴하면 계정 정보가 삭제되고 로그인할 수 없게 됩니다.
      </p>
      {error && <p className="mb-stack-sm text-body-sm text-error-red">{error}</p>}
      <Button variant="danger" onClick={handleWithdraw} disabled={isSubmitting} className="w-fit">
        {isSubmitting ? "처리 중..." : "회원 탈퇴"}
      </Button>
    </Card>
  );
}
