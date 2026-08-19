import { AuthGuard } from "@/components/layout/AuthGuard";
import { AppHeader } from "@/components/layout/AppHeader";

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <AppHeader />
      <main className="max-w-container-max px-margin-mobile py-stack-lg md:px-margin-desktop mx-auto w-full flex-1">
        {children}
      </main>
    </AuthGuard>
  );
}
