import { Outlet, useLocation, Navigate } from 'react-router-dom'
import Sidebar from './Sidebar'
import Header from './Header'
import { useAuthStore } from '@/store/authStore'

// Супер-админ управляет только платформой (организации + свой профиль).
// Операционные модули (филиалы, группы, контингент, оплата и т.д.) принадлежат
// конкретной организации и ему недоступны — при попытке зайти по прямому URL
// его вернёт на страницу организаций.
const SYSTEM_ADMIN_ALLOWED = ['/organizations', '/profile']

export default function AppLayout() {
  const roleCode = useAuthStore((s) => s.roleCode)
  const { pathname } = useLocation()

  if (roleCode === 'SYSTEM_ADMIN') {
    const allowed = SYSTEM_ADMIN_ALLOWED.some(
      (p) => pathname === p || pathname.startsWith(p + '/')
    )
    if (!allowed) {
      return <Navigate to="/organizations" replace />
    }
  }

  return (
    <div className="flex h-screen bg-gray-50 overflow-hidden">
      <Sidebar />
      <div className="flex flex-col flex-1 overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto p-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
