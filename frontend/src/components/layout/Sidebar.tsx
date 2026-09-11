import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Building2, Users, BookOpen,
  Baby, CalendarCheck, ClipboardList, BookMarked,
  MessageSquare, Stethoscope, Wrench, DollarSign, CalendarDays,
  ClipboardCheck, CalendarRange, Clock, ScrollText, ShieldCheck,
  UserPlus, Library, Sparkles, Settings,
} from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { useT } from '@/i18n'
import type { RoleCode } from '@/types'
import { clsx } from 'clsx'

interface NavItem {
  to: string
  icon: React.ElementType
  labelKey: string
  roles: RoleCode[]
}

const navItems: NavItem[] = [
  { to: '/dashboard',    icon: LayoutDashboard, labelKey: 'nav.dashboard',    roles: ['FOUNDER','DIRECTOR','METHODIST','EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR','NURSE','JANITOR','ACCOUNTANT'] },
  { to: '/organizations', icon: Building2,      labelKey: 'nav.organizations', roles: ['SYSTEM_ADMIN'] },
  { to: '/branches',     icon: Building2,       labelKey: 'nav.branches',     roles: ['FOUNDER','DIRECTOR','METHODIST'] },
  { to: '/groups',       icon: BookOpen,        labelKey: 'nav.groups',       roles: ['DIRECTOR','METHODIST','EDUCATOR'] },
  { to: '/children',     icon: Baby,            labelKey: 'nav.children',     roles: ['FOUNDER','DIRECTOR','METHODIST','EDUCATOR','NURSE'] },
  { to: '/attendance',   icon: CalendarCheck,   labelKey: 'nav.attendance',   roles: ['DIRECTOR','METHODIST','EDUCATOR','ACCOUNTANT'] },
  { to: '/cyclogram',    icon: CalendarDays,    labelKey: 'nav.cyclogram',    roles: ['DIRECTOR','METHODIST','EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR'] },
  { to: '/plans',        icon: ClipboardList,   labelKey: 'nav.plans',        roles: ['DIRECTOR','METHODIST','EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR'] },
  { to: '/annual-plans', icon: ClipboardList,   labelKey: 'nav.annualPlans',  roles: ['DIRECTOR','METHODIST'] },
  { to: '/schedule',     icon: CalendarRange,   labelKey: 'nav.schedule',     roles: ['DIRECTOR','METHODIST','EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR'] },
  { to: '/routine',      icon: Clock,           labelKey: 'nav.routine',      roles: ['DIRECTOR','METHODIST','EDUCATOR'] },
  { to: '/observation',  icon: ClipboardCheck,  labelKey: 'nav.observation',  roles: ['DIRECTOR','METHODIST','EDUCATOR'] },
  { to: '/daily-info',   icon: BookOpen,        labelKey: 'nav.dailyInfo',    roles: ['DIRECTOR','METHODIST','EDUCATOR'] },
  { to: '/protocols',    icon: BookMarked,      labelKey: 'nav.protocols',    roles: ['DIRECTOR','METHODIST'] },
  { to: '/orders',       icon: ScrollText,      labelKey: 'nav.orders',       roles: ['DIRECTOR'] },
  { to: '/invitations',  icon: UserPlus,        labelKey: 'nav.invitations',  roles: ['DIRECTOR','METHODIST','EDUCATOR'] },
  { to: '/chat',         icon: MessageSquare,   labelKey: 'nav.chat',         roles: ['EDUCATOR','PARENT'] },
  { to: '/medical',      icon: Stethoscope,     labelKey: 'nav.medical',      roles: ['NURSE','DIRECTOR','METHODIST'] },
  { to: '/assets',       icon: Wrench,          labelKey: 'nav.assets',       roles: ['JANITOR','DIRECTOR','METHODIST'] },
  { to: '/library',      icon: Library,         labelKey: 'nav.library',      roles: ['DIRECTOR','METHODIST'] },
  { to: '/janitor',      icon: ShieldCheck,     labelKey: 'nav.janitor',      roles: ['JANITOR','DIRECTOR'] },
  { to: '/payments',     icon: DollarSign,      labelKey: 'nav.payments',     roles: ['FOUNDER','DIRECTOR','METHODIST','ACCOUNTANT','PARENT'] },
  { to: '/ai',           icon: Sparkles,        labelKey: 'nav.ai',           roles: ['DIRECTOR','METHODIST','EDUCATOR'] },
  { to: '/nomenclature', icon: ClipboardList,   labelKey: 'nav.nomenclature', roles: ['DIRECTOR','METHODIST'] },
  { to: '/users',        icon: Users,           labelKey: 'nav.users',        roles: ['DIRECTOR'] },
  { to: '/settings',     icon: Settings,        labelKey: 'nav.settings',     roles: ['DIRECTOR'] },
]

export default function Sidebar() {
  const roleCode = useAuthStore((s) => s.roleCode)
  const t = useT()

  const visible = navItems.filter(
    (item) => roleCode && item.roles.includes(roleCode)
  )

  return (
    <aside className="w-64 bg-white border-r border-gray-200 flex flex-col shrink-0">
      <div className="h-16 flex items-center px-6 border-b border-gray-200">
        <span className="text-xl font-bold text-primary-700">{t('app.name')}</span>
      </div>
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
            {t(item.labelKey)}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
