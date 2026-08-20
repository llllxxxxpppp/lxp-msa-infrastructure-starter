"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { useLogout } from "@/features/auth/hooks";
import { getAccessToken } from "@/lib/token-storage";
import { decodeAccessToken } from "@/lib/jwt";
import { Avatar } from "@/components/ui/Avatar";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

const NAV_LINKS = [
  { href: "/courses", label: "강좌" },
  { href: "/members", label: "마이페이지" },
];

/** 현재 경로가 이 nav 항목에 해당하는지 — 하위 경로(/courses/123 등)도 같이 활성화한다. */
function isActivePath(pathname: string, href: string) {
  return pathname === href || pathname.startsWith(`${href}/`);
}

/** design/course/course-list, design/mypage 등에 공통으로 나오는 상단 내비게이션. */
export function AppHeader() {
  const router = useRouter();
  const pathname = usePathname();
  const logout = useLogout();
  // 서버/클라이언트 최초 렌더 결과를 동일하게 유지하기 위해 null로 시작하고,
  // mount 이후 effect에서만 토큰을 읽어 실제 email로 갱신한다.
  const [email, setEmail] = useState<string | null>(null);
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const token = getAccessToken();
    setEmail(token ? (decodeAccessToken(token)?.sub ?? null) : null);
  }, []);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setIsMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  async function handleLogout() {
    await logout();
    router.push("/login");
  }

  return (
    <nav className="border-outline-variant bg-surface sticky top-0 z-50 w-full border-b">
      <div className="max-w-container-max px-margin-mobile md:px-margin-desktop mx-auto flex h-16 w-full items-center justify-between">
        <div className="gap-stack-lg flex items-center">
          <Link href="/courses" className="text-headline-md text-primary font-bold">
            llllxxxxpppp
          </Link>
        </div>

        <div className="gap-stack-lg hidden items-center md:flex">
          {NAV_LINKS.map((link) => {
            const active = isActivePath(pathname, link.href);
            return (
              <Link
                key={link.href}
                href={link.href}
                className={`text-body-md pb-1 transition-colors ${
                  active
                    ? "border-secondary text-secondary border-b-2 font-bold"
                    : "text-on-surface-variant hover:text-secondary"
                }`}
              >
                {link.label}
              </Link>
            );
          })}
        </div>

        <div className="gap-stack-md flex items-center">
          <button
            type="button"
            aria-label="Notifications"
            className="text-on-surface-variant hover:bg-surface-container hover:text-secondary rounded-full p-2 transition-colors"
          >
            <MaterialIcon name="notifications" />
          </button>

          <div ref={menuRef} className="relative">
            <button
              type="button"
              onClick={() => setIsMenuOpen((open) => !open)}
              className="text-on-surface-variant hover:text-secondary flex items-center gap-2 transition-colors"
            >
              <Avatar label={email ?? "?"} className="h-8 w-8" />
              <span className="text-label-md hidden md:block">{email ?? "Profile"}</span>
            </button>

            {isMenuOpen && (
              <div className="border-outline-variant bg-surface-container-lowest absolute right-0 mt-2 w-48 rounded-lg border py-1 shadow-[0_12px_24px_rgba(0,0,0,0.12)]">
                <Link
                  href="/members"
                  className="text-body-sm text-on-surface hover:bg-surface-container block px-4 py-2"
                  onClick={() => setIsMenuOpen(false)}
                >
                  마이페이지
                </Link>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="text-body-sm text-on-surface hover:bg-surface-container block w-full px-4 py-2 text-left"
                >
                  로그아웃
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
