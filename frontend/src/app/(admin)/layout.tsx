import { AuthGuard } from "@/components/layout/AuthGuard";
import { RoleGuard } from "@/components/layout/RoleGuard";
import { AdminSidebar } from "@/components/admin/AdminSidebar";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <RoleGuard allowedRoles={["ROLE_ADMIN"]}>
        <div className="flex min-h-screen flex-1">
          <AdminSidebar />
          <main className="p-stack-lg flex-1 overflow-x-auto">{children}</main>
        </div>
      </RoleGuard>
    </AuthGuard>
  );
}
