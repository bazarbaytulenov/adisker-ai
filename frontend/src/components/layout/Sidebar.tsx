import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Building2, Users, BookOpen,
  Baby, CalendarCheck, ClipboardList, BookMarked,
  MessageSquare, Stethoscope, Wrench, DollarSign, CalendarDays,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import type { RoleCode } from '@/types'
import { clsx } from 'clsx'

interface NavItem {
  to: string
  icon: React.ElementType
  label: string
  roles: RoleCode[]
}

const navItems: NavItem[] = [
  { to: '/dashboard',  icon: LayoutDashboard, label: 'Главная',       roles: ['SYSTEM_ADMIN','FOUNDER','DIRECTOR','METHODIST','EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR','NURSE','JANITOR','ACCOUNTANT'] },
  { to: '/branches',   icon: Building2,       label: 'Филиалы',       roles: ['SYSTEM_ADMIN','FOUNDER','DIRECTOR','METHODIST'] },
  { to: '/groups',     icon: BookOpen,         label: 'Группы',        roles: ['SYSTEM_ADMIN','DIRECTOR','METHODIST','EDUCATOR'] },
  { to: '/children',   icon: Baby,             label: 'Контингент',    roles: ['SYSTEM_ADMIN','FOUNDER','DIRECTOR','METHODIST','EDUCATOR','NURSE'] },
  { to: '/attendance', icon: CalendarCheck,   label: 'Посещаемость',  roles: ['SYSTEM_ADMIN','DIRECTOR','METHODIST','EDUCATOR','ACCOUNTANT'] },
  { to: '/cyclogram',  icon: CalendarDays,    label: 'Циклограмма',   roles: ['SYSTEM_ADMIN','DIRECTOR','METHODIST','EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR'] },
  { to: '/plans',      icon: ClipboardList,   label: 'Планы',         roles: ['SYSTEM_ADMIN','DIRECTOR','METHODIST','EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR'] },
  { to: '/protocols',  icon: BookMarked,       label: 'Протоколы',     roles: ['SYSTEM_ADMIN','DIRECTOR','METHODIST'] },
  { to: '/chat',       icon: MessageSquare,   label: 'Чат',           roles: ['EDUCATOR','PARENT'] },
  { to: '/medical',    icon: Stethoscope,     label: 'Медицина',      roles: ['NURSE','DIRECTOR','METHODIST'] },
  { to: '/assets',     icon: Wrench,          label: 'МТБ',           roles: ['JANITOR','DIRECTOR','METHODIST'] },
  { to: '/payments',   icon: DollarSign,      label: 'Оплата',        roles: ['ACCOUNTANT','DIRECTOR'] },
  { to: '/users',      icon: Users,           label: 'Пользователи',  roles: ['SYSTEM_ADMIN','DIRECTOR'] },
]

export default function Sidebar() {
  const roleCode = useAuthStore((s) => s.roleCode)

  const visible = navItems.filter(
    (item) => roleCode && item.roles.includes(roleCode)
  )

  return (
    <aside className="w-64 bg-white border-r border-gray-200 flex flex-col shrink-0">
      {/* Logo */}
      <div className="h-16 flex items-center px-6 border-b border-gray-200">
        <span className="text-xl font-bold text-primary-700">Әдіскер-AI</span>
      </div>

      {/* Nav */}
      <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-0.5">
        {visible.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              clsx(
                'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                isActive
                  ? 'bg-primary-50 text-primary-700'
                  : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
              )
            }
          >
            <item.icon className="h-5 w-5 shrink-0" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
