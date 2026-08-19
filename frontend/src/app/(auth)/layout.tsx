/** design/login, design/signup 공통 배경(Deep Navy + 점 패턴) — 로그인/회원가입 카드를 감싼다. */
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <main className="bg-primary px-margin-mobile py-stack-lg relative flex min-h-screen flex-1 items-center justify-center overflow-hidden">
      <div
        className="pointer-events-none absolute inset-0 opacity-50"
        style={{
          backgroundImage: "radial-gradient(rgba(255,255,255,0.1) 1px, transparent 1px)",
          backgroundSize: "24px 24px",
        }}
      />
      <div className="relative z-10 w-full max-w-[420px]">{children}</div>
    </main>
  );
}
