"use client";

import { useRouter } from "next/navigation";
import { useLogout } from "@/features/auth/hooks";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

/**
 * design/policy-explorer 좌측 어드민 내비게이션.
 * 대시보드/애널리틱스/설정/고객센터는 화면이 없어 제거하고 "사내 규정"만 남겼다.
 */
export function AdminSidebar() {
  const router = useRouter();
  const logout = useLogout();

  async function handleLogout() {
    await logout();
    router.push("/login");
  }

  return (
    <aside className="gap-stack-lg bg-primary p-stack-md text-on-primary flex w-64 shrink-0 flex-col">
      <h1 className="text-headline-sm font-black">Admin Portal</h1>
      <nav className="flex flex-1 flex-col gap-1">
        <div className="gap-stack-sm px-stack-md py-stack-sm text-label-md flex items-center rounded-lg bg-secondary text-on-secondary">
          <MaterialIcon name="gavel" className="text-[20px]" />
          사내 규정
        </div>
      </nav>
      <button
        type="button"
        onClick={handleLogout}
        className="gap-stack-sm px-stack-md py-stack-sm text-label-md text-inverse-primary/80 flex items-center rounded-lg hover:bg-white/5"
      >
        <MaterialIcon name="logout" className="text-[20px]" />
        로그아웃
      </button>
    </aside>
  );
}
