import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import LoginPage from '@/pages/auth/LoginPage'
import AppLayout from '@/components/layout/AppLayout'
import DashboardPage from '@/pages/dashboard/DashboardPage'
import BranchesPage from '@/pages/branches/BranchesPage'
import GroupsPage from '@/pages/groups/GroupsPage'
import ChildrenPage from '@/pages/children/ChildrenPage'
import UsersPage from '@/pages/users/UsersPage'
import AttendancePage from '@/pages/attendance/AttendancePage'
import CyclogramPage from '@/pages/cyclogram/CyclogramPage'
import DailyInfoPage from '@/pages/dailyinfo/DailyInfoPage'
import PlanPage from '@/pages/plan/PlanPage'
import SchedulePage from '@/pages/schedule/SchedulePage'
import ObservationPage from '@/pages/observation/ObservationPage'
import NotFoundPage from '@/pages/NotFoundPage'
import StubPage from '@/pages/StubPage'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
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
          <Route index element={<Navigate to="/dashboard" replace />} />
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
          <Route path="protocols"    element={<StubPage title="Протоколы" description="Модуль протоколов педагогических советов" icon="book" />} />
          <Route path="chat"         element={<StubPage title="Чат" description="Чат воспитатель–родитель" icon="message" />} />
          <Route path="medical"      element={<StubPage title="Медицина" description="Медицинские журналы" icon="stethoscope" />} />
          <Route path="assets"       element={<StubPage title="МТБ" description="Материально-техническая база" icon="wrench" />} />
          <Route path="payments"     element={<StubPage title="Оплата" description="Начисления и платежи" icon="dollar" />} />
          <Route path="users"        element={<UsersPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}
