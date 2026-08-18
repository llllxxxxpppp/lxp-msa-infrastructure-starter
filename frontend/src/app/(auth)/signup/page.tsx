"use client";

import { FormEvent, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import * as memberApi from "@/features/member/api";
import { ApiError } from "@/types/api";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

export default function SignupPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsLoading(true);
    setError(null);
    try {
      await memberApi.signup({ email, password });
      router.push("/login");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "회원가입에 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="gap-stack-lg border-outline-variant bg-surface-container-lowest p-stack-lg flex flex-col rounded-xl border shadow-[0_2px_12px_rgba(0,0,0,0.08)]">
      <div className="gap-stack-sm flex flex-col items-center text-center">
        <div className="border-outline-variant bg-surface-container mb-2 flex h-16 w-16 items-center justify-center overflow-hidden rounded-lg border">
          <span className="text-headline-sm text-primary font-bold">LXP</span>
        </div>
        <h1 className="text-headline-sm text-primary m-0">Create Account</h1>
        <p className="text-body-sm text-slate-text m-0">
          Join EduSphere LXP to start your learning journey.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="gap-stack-md flex flex-col">
        <Input
          id="email"
          type="email"
          label="Email Address"
          icon="mail"
          placeholder="name@company.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Input
          id="password"
          type={showPassword ? "text" : "password"}
          label="Password"
          icon="lock"
          placeholder="••••••••"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          minLength={6}
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

        {error && <p className="text-body-sm text-error-red">{error}</p>}

        <Button type="submit" disabled={isLoading} className="mt-2 w-full">
          {isLoading ? "가입 중..." : "Create Account"}
          <MaterialIcon name="arrow_forward" className="text-[18px]" />
        </Button>
      </form>

      <div className="border-outline-variant pt-stack-sm mt-2 border-t text-center">
        <p className="text-body-sm text-slate-text m-0">
          Already have an account?{" "}
          <Link href="/login" className="text-label-md text-secondary hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
}
