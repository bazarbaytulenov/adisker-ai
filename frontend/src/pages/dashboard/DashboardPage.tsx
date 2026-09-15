import { useQuery } from '@tanstack/react-query'
import { Building2, Users, Baby, CalendarCheck } from 'lucide-react'
import { branchApi, groupApi, childApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Spinner } from '@/components/common'
import { useT } from '@/i18n'

interface StatCardProps {
  icon: React.ElementType
  label: string
  value: number | string
  color: string
}

function StatCard({ icon: Icon, label, value, color }: StatCardProps) {
  return (
    <div className="card flex items-center gap-4">
      <div className={`rounded-xl p-3 ${color}`}>
        <Icon className="h-6 w-6 text-white" />
      </div>
      <div>
        <p className="text-sm text-gray-500">{label}</p>
        <p className="text-2xl font-bold text-gray-900">{value}</p>
      </div>
    </div>
  )
}

export default function DashboardPage() {
  const { organizationId } = useAuthStore()
  const t = useT()

  const { data: branchesRes, isLoading: loadingBranches } = useQuery({
    queryKey: ['branches', organizationId],
    queryFn: () => branchApi.list(organizationId!, 0, 100),
    enabled: !!organizationId,
  })

  const { data: childrenRes, isLoading: loadingChildren } = useQuery({
    queryKey: ['children', organizationId],
    queryFn: () => childApi.list(organizationId!),
    enabled: !!organizationId,
  })

  if (loadingBranches || loadingChildren) return <Spinner />

  const totalBranches = branchesRes?.data.data?.totalElements ?? 0
  const totalChildren = childrenRes?.data.data?.totalElements ?? 0

  const quickLinks = [
    { label: t('dashboard.quickAccess.addChild'), href: '/children' },
    { label: t('dashboard.quickAccess.attendance'), href: '/attendance' },
    { label: t('dashboard.quickAccess.plans'), href: '/plans' },
    { label: t('dashboard.quickAccess.protocols'), href: '/protocols' },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t('dashboard.title')}</h1>
        <p className="text-gray-500 text-sm mt-1">{t('dashboard.subtitle')}</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={Building2}
          label={t('dashboard.stat.branches')}
          value={totalBranches}
          color="bg-blue-500"
        />
        <StatCard
          icon={Baby}
          label={t('dashboard.stat.children')}
          value={totalChildren}
          color="bg-green-500"
        />
        <StatCard
          icon={Users}
          label={t('dashboard.stat.staff')}
          value="—"
          color="bg-purple-500"
        />
        <StatCard
          icon={CalendarCheck}
          label={t('dashboard.stat.attendance')}
          value="—"
          color="bg-orange-500"
        />
      </div>

      <div className="card">
        <h2 className="text-lg font-semibold mb-4">{t('dashboard.quickAccess.title')}</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {quickLinks.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="flex items-center justify-center rounded-lg border border-gray-200 p-3 text-sm text-gray-700 hover:bg-primary-50 hover:border-primary-300 hover:text-primary-700 transition-colors text-center"
            >
              {link.label}
            </a>
          ))}
        </div>
      </div>
    </div>
  )
}
