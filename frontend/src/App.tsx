import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import LoginPage from '@/pages/auth/LoginPage'
import AppLayout from '@/components/layout/AppLayout'
import DashboardPage from '@/pages/dashboard/DashboardPage'
import BranchesPage from '@/pages/branches/BranchesPage'
import GroupsPage from '@/pages/groups/GroupsPage'
import ChildrenPage from '@/pages/children/ChildrenPage'
import UsersPage from '@/pages/users/UsersPage'
import OrganizationsPage from '@/pages/organizations/OrganizationsPage'
import AttendancePage from '@/pages/attendance/AttendancePage'
import CyclogramPage from '@/pages/cyclogram/CyclogramPage'
import DailyInfoPage from '@/pages/dailyinfo/DailyInfoPage'
import PlanPage from '@/pages/plan/PlanPage'
import SchedulePage from '@/pages/schedule/SchedulePage'
import ObservationPage from '@/pages/observation/ObservationPage'
import NomenclaturePage from '@/pages/nomenclature/NomenclaturePage'
import ProtocolsPage from '@/pages/protocols/ProtocolsPage'
import PaymentsPage from '@/pages/payments/PaymentsPage'
import RoutinePage from '@/pages/routine/RoutinePage'
import AnnualPlanPage from '@/pages/annual/AnnualPlanPage'
import OrdersPage from '@/pages/orders/OrdersPage'
import JanitorPage from '@/pages/janitor/JanitorPage'
import InvitationsPage from '@/pages/parent/InvitationsPage'
import ChatPage from '@/pages/chat/ChatPage'
import MedicalPage from '@/pages/medical/MedicalPage'
import AssetsPage from '@/pages/assets/AssetsPage'
import LibraryPage from '@/pages/library/LibraryPage'
import AiPage from '@/pages/ai/AiPage'
import SettingsPage from '@/pages/settings/SettingsPage'
import ProfilePage from '@/pages/profile/ProfilePage'
import NotFoundPage from '@/pages/NotFoundPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

function RequireRole({ roles, children }: { roles: string[]; children: React.ReactNode }) {
  const roleCode = useAuthStore((s) => s.roleCode)
  if (!roleCode || !roles.includes(roleCode)) {
    return <Navigate to="/dashboard" replace />
  }
  return <>{children}</>
}

function HomeRedirect() {
  const roleCode = useAuthStore((s) => s.roleCode)
  return <Navigate to={roleCode === 'SYSTEM_ADMIN' ? '/organizations' : '/dashboard'} replace />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <AppLayout />
            </RequireAuth>
          }
        >
          <Route index element={<HomeRedirect />} />
          <Route path="dashboard"    element={<DashboardPage />} />
          <Route path="branches"     element={<BranchesPage />} />
          <Route path="groups"       element={<GroupsPage />} />
          <Route path="children"     element={<ChildrenPage />} />
          <Route path="attendance"   element={<AttendancePage />} />
          <Route path="cyclogram"    element={<CyclogramPage />} />
          <Route path="daily-info"   element={<DailyInfoPage />} />
          {/* plan → /plans */}
          <Route path="plan"         element={<Navigate to="/plans" replace />} />
          <Route path="plans"        element={<PlanPage />} />
          <Route path="schedule"     element={<SchedulePage />} />
          <Route path="observation"  element={<ObservationPage />} />
          <Route path="protocols"    element={<ProtocolsPage />} />
          <Route path="chat"         element={<ChatPage />} />
          <Route path="medical"      element={<MedicalPage />} />
          <Route path="assets"       element={<AssetsPage />} />
          <Route path="library"      element={<LibraryPage />} />
          <Route path="payments"     element={<PaymentsPage />} />
          <Route path="nomenclature" element={<NomenclaturePage />} />
          <Route path="routine"      element={<RoutinePage />} />
          <Route path="annual-plans" element={<AnnualPlanPage />} />
          <Route path="orders"       element={<OrdersPage />} />
          <Route path="janitor"      element={<JanitorPage />} />
          <Route path="invitations"  element={<InvitationsPage />} />
          <Route path="ai"           element={<AiPage />} />
          <Route path="settings"     element={<RequireRole roles={['SYSTEM_ADMIN','DIRECTOR']}><SettingsPage /></RequireRole>} />
          <Route path="profile"      element={<ProfilePage />} />
          <Route path="users"        element={<RequireRole roles={['SYSTEM_ADMIN','DIRECTOR']}><UsersPage /></RequireRole>} />
          <Route path="organizations" element={<RequireRole roles={['SYSTEM_ADMIN']}><OrganizationsPage /></RequireRole>} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}
