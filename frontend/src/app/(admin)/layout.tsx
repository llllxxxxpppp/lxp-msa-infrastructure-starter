import { AuthGuard } from "@/components/layout/AuthGuard";
import { RoleGuard } from "@/components/layout/RoleGuard";
import { AdminSidebar } from "@/components/admin/AdminSidebar";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <AuthGuard>
      <RoleGuard allowedRoles={["ROLE_ADMIN"]}>
        <div className="flex h-screen overflow-hidden">
          <AdminSidebar />
          <main className="p-stack-lg min-h-0 flex-1 overflow-y-auto overflow-x-auto">{children}</main>
        </div>
      </RoleGuard>
    </AuthGuard>
  );
}
