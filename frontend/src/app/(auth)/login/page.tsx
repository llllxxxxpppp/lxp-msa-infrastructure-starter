"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useLogin } from "@/features/auth/hooks";
import { getAccessToken } from "@/lib/token-storage";
import { decodeAccessToken, getRoles } from "@/lib/jwt";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

export default function LoginPage() {
  const router = useRouter();
  const { login, isLoading, error } = useLogin();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    const success = await login({ email, password });
    if (!success) return;

    // 관리자 계정은 학습자 화면(/courses) 대신 바로 백오피스로 보낸다.
    const token = getAccessToken();
    const roles = getRoles(token ? decodeAccessToken(token) : null);
    router.push(roles.includes("ROLE_ADMIN") ? "/policy-explorer" : "/courses");
  }

  return (
    <div className="gap-stack-lg border-outline-variant bg-surface-container-lowest p-stack-lg flex flex-col rounded-xl border shadow-[0_2px_12px_rgba(0,0,0,0.08)]">
      <div className="gap-stack-sm flex flex-col items-center text-center">
        {/* <div className="border-outline-variant bg-surface-container mb-2 flex h-16 w-16 items-center justify-center overflow-hidden rounded-lg border">
          <span className="text-headline-sm text-primary font-bold">LXP</span>
        </div> */}
        <h1 className="text-headline-sm text-primary m-0">llllxxxxpppp에 오신걸 환영합니다</h1>
        <p className="text-body-sm text-slate-text m-0">
          학습을 계속하려면 로그인하세요
        </p>
      </div>

      <form onSubmit={handleSubmit} className="gap-stack-md flex flex-col">
        <Input
          id="email"
          type="email"
          label="이메일"
          icon="mail"
          placeholder="name@company.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <div className="gap-base flex flex-col">
          <div className="flex items-center justify-between">
            <label htmlFor="password" className="text-label-md text-primary">
              비밀번호
            </label>
            {/* <a href="#" className="text-body-sm text-secondary hover:underline">
              Forgot Password?
            </a> */}
          </div>
          <Input
            id="password"
            type={showPassword ? "text" : "password"}
            icon="lock"
            placeholder="••••••••"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            trailing={
              <button
                type="button"
                aria-label="Toggle password visibility"
                onClick={() => setShowPassword((v) => !v)}
                className="hover:text-primary"
              >
                <MaterialIcon
                  name={showPassword ? "visibility_off" : "visibility"}
                  className="text-[20px]"
                />
              </button>
            }
          />
        </div>

        {/* Remember me는 로컬 UI 상태만 있고 실제 저장/자동로그인 로직은 없다 (백엔드에 해당 기능 없음). */}
        {/* <label className="mt-1 flex cursor-pointer items-center gap-2 select-none">
          <input
            type="checkbox"
            className="border-outline-variant text-secondary h-4 w-4 rounded-sm"
          />
          <span className="text-body-sm text-slate-text">Remember me for 30 days</span>
        </label> */}

        {error && <p className="text-body-sm text-error-red">{error}</p>}

        <Button type="submit" disabled={isLoading} className="mt-2 w-full">
          {isLoading ? "로그인 중..." : "로그인"}
          <MaterialIcon name="arrow_forward" className="text-[18px]" />
        </Button>
      </form>

      <div className="border-outline-variant pt-stack-sm mt-2 flex flex-col gap-stack-sm border-t text-center">
        <p className="text-body-sm text-slate-text m-0">Don&apos;t have an account?</p>
        <Link href="/signup">
          <Button type="button" variant="secondary" className="w-full">
            회원가입
          </Button>
        </Link>
      </div>
    </div>
  );
}
