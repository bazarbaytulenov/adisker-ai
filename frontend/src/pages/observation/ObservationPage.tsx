import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Eye, ChevronDown, ChevronUp } from 'lucide-react'
import { observationApi, branchApi, groupApi, childApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Select, Spinner, Empty, Card } from '@/components/common'
import type { ObservationIndicator } from '@/types'
import { useT } from '@/i18n'

function academicYears() {
  const year = new Date().getFullYear()
  return [
    { value: `${year - 1}-${year}`, label: `${year - 1}–${year}` },
    { value: `${year}-${year + 1}`, label: `${year}–${year + 1}` },
  ]
}

function groupByDomain(indicators: ObservationIndicator[]) {
  const map = new Map<string, ObservationIndicator[]>()
  for (const ind of indicators) {
    if (!map.has(ind.domain)) map.set(ind.domain, [])
    map.get(ind.domain)!.push(ind)
  }
  return map
}

export default function ObservationPage() {
  const qc = useQueryClient()
  const { organizationId } = useAuthStore()
  const t = useT()

  const PERIODS = [
    { value: 'start',  label: t('observation.period.start') },
    { value: 'middle', label: t('observation.period.middle') },
    { value: 'end',    label: t('observation.period.end') },
  ]

  const LEVELS = [
    { value: 'H', label: 'Н', title: t('observation.level.low'),  color: 'bg-red-100 text-red-700 border-red-300' },
    { value: 'S', label: 'С', title: t('observation.level.mid'),  color: 'bg-yellow-100 text-yellow-700 border-yellow-300' },
    { value: 'V', label: 'В', title: t('observation.level.high'), color: 'bg-green-100 text-green-700 border-green-300' },
  ]

  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')
  const [childId, setChildId] = useState('')
  const [period, setPeriod] = useState('start')
  const [academicYear, setAcademicYear] = useState(() => {
    const y = new Date().getFullYear()
    const m = new Date().getMonth()
    return m >= 8 ? `${y}-${y + 1}` : `${y - 1}-${y}`
  })

  const [expanded, setExpanded] = useState<Set<string>>(new Set())
  const toggleDomain = (d: string) =>
    setExpanded((prev) => { const next = new Set(prev); next.has(d) ? next.delete(d) : next.add(d); return next })

  const [localResults, setLocalResults] = useState<Record<string, string>>({})

  const { data: branchesRes } = useQuery({
    queryKey: ['branches-active', organizationId],
    queryFn: () => branchApi.listActive(organizationId!),
    enabled: !!organizationId,
  })
  const branches = branchesRes?.data.data ?? []

  const { data: groupsRes } = useQuery({
    queryKey: ['groups', organizationId, branchId],
    queryFn: () => groupApi.list(organizationId!, branchId || undefined, 0, 100),
    enabled: !!organizationId,
  })
  const groups = groupsRes?.data.data?.content ?? []

  const { data: childrenRes } = useQuery({
    queryKey: ['children-obs', organizationId, branchId, groupId],
    queryFn: () => childApi.list(organizationId!, branchId || undefined, groupId || undefined, 0, 100),
    enabled: !!organizationId && !!groupId,
  })
  const children = childrenRes?.data.data?.content ?? []

  const enabled = !!organizationId && !!groupId && !!childId && !!period && !!academicYear

  const { data: obsRes, isLoading: obsLoading } = useQuery({
    queryKey: ['observation', organizationId, branchId, groupId, childId, period, academicYear],
    queryFn: async () => {
      const res = await observationApi.getOrCreate(organizationId!, branchId, groupId, childId, period, academicYear)
      const serverResults: Record<string, string> = {}
      for (const r of res.data.data?.results ?? []) serverResults[r.indicatorId] = r.level
      setLocalResults(serverResults)
      return res
    },
    enabled,
  })
  const obsData = obsRes?.data.data

  const setResultMutation = useMutation({
    mutationFn: ({ indicatorId, level }: { indicatorId: string; level: string | null }) =>
      observationApi.setResult(obsData!.id, indicatorId, level),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['observation'] }),
  })

  const handleLevelClick = (indicatorId: string, level: string) => {
    if (!obsData) return
    const current = localResults[indicatorId]
    const newLevel = current === level ? null : level
    setLocalResults((prev) => { const next = { ...prev }; if (newLevel === null) delete next[indicatorId]; else next[indicatorId] = newLevel; return next })
    setResultMutation.mutate({ indicatorId, level: newLevel })
  }

  const totalIndicators = obsData?.indicators.length ?? 0
  const filledCount = Object.keys(localResults).length
  const fillPct = totalIndicators > 0 ? Math.round((filledCount / totalIndicators) * 100) : 0
  const levelStats = { H: 0, S: 0, V: 0 }
  for (const lvl of Object.values(localResults)) {
    if (lvl in levelStats) levelStats[lvl as keyof typeof levelStats]++
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">{t('observation.title')}</h1>
        <p className="text-sm text-gray-500 mt-1">{t('observation.subtitle')}</p>
      </div>

      <Card className="p-4">
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
          {branches.length > 0 && (
            <Select options={branches.map((b) => ({ value: b.id, label: b.name }))}
              value={branchId} onChange={(e) => { setBranchId(e.target.value); setGroupId(''); setChildId('') }}
              placeholder={t('observation.placeholder.branch')} />
          )}
          <Select options={groups.map((g) => ({ value: g.id, label: g.name }))}
            value={groupId} onChange={(e) => { setGroupId(e.target.value); setChildId('') }}
            placeholder={t('observation.placeholder.group')} />
          <Select options={children.map((c) => ({ value: c.id, label: `${c.lastName} ${c.firstName}` }))}
            value={childId} onChange={(e) => setChildId(e.target.value)}
            placeholder={t('observation.placeholder.child')} disabled={!groupId} />
          <Select options={PERIODS} value={period} onChange={(e) => setPeriod(e.target.value)} />
          <Select options={academicYears()} value={academicYear} onChange={(e) => setAcademicYear(e.target.value)} />
        </div>
      </Card>

      {!enabled ? (
        <Empty message={t('observation.selectAll')} />
      ) : obsLoading ? (
        <Spinner />
      ) : !obsData ? (
        <Empty message={t('observation.loadError')} />
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            <Card className="p-4 text-center">
              <div className="text-2xl font-bold text-primary-600">{fillPct}%</div>
              <div className="text-xs text-gray-500 mt-1">{t('observation.stat.filled')}</div>
            </Card>
            <Card className="p-4 text-center">
              <div className="text-2xl font-bold text-gray-700">{totalIndicators}</div>
              <div className="text-xs text-gray-500 mt-1">{t('observation.stat.indicators')}</div>
            </Card>
            {LEVELS.map((l) => (
              <Card key={l.value} className="p-4 text-center">
                <div className={`text-2xl font-bold ${l.color.split(' ')[1]}`}>
                  {levelStats[l.value as keyof typeof levelStats]}
                </div>
                <div className="text-xs text-gray-500 mt-1">{l.title}</div>
              </Card>
            ))}
          </div>

          <div className="w-full bg-gray-100 rounded-full h-2">
            <div className="bg-primary-500 h-2 rounded-full transition-all duration-300" style={{ width: `${fillPct}%` }} />
          </div>

          <div className="flex items-center gap-4 text-xs">
            <span className="text-gray-500 font-medium">{t('observation.level.label')}</span>
            {LEVELS.map((l) => (
              <span key={l.value} className={`inline-flex items-center gap-1 px-2 py-1 rounded border text-xs font-semibold ${l.color}`}>
                {l.label} — {l.title}
              </span>
            ))}
          </div>

          <div className="space-y-3">
            {Array.from(groupByDomain(obsData.indicators)).map(([domain, indicators]) => {
              const isOpen = expanded.has(domain)
              const domainFilled = indicators.filter((i) => localResults[i.id]).length
              return (
                <div key={domain} className="rounded-xl border border-gray-100 bg-white overflow-hidden">
                  <button type="button" onClick={() => toggleDomain(domain)}
                    className="w-full flex items-center justify-between px-5 py-4 hover:bg-gray-50 transition-colors">
                    <div className="flex items-center gap-3">
                      <Eye className="h-5 w-5 text-primary-500" />
                      <span className="font-semibold text-gray-800">{domain}</span>
                      <span className="text-xs text-gray-400">{domainFilled}/{indicators.length}</span>
                    </div>
                    {isOpen ? <ChevronUp className="h-4 w-4 text-gray-400" /> : <ChevronDown className="h-4 w-4 text-gray-400" />}
                  </button>
                  {isOpen && (
                    <div className="border-t border-gray-100">
                      <table className="min-w-full text-sm">
                        <thead>
                          <tr className="bg-gray-50">
                            <th className="px-5 py-2 text-left text-xs font-semibold text-gray-500 uppercase w-1/3">
                              {t('observation.col.criterion')}
                            </th>
                            <th className="px-5 py-2 text-left text-xs font-semibold text-gray-500 uppercase">
                              {t('observation.col.indicator')}
                            </th>
                            <th className="px-5 py-2 text-left text-xs font-semibold text-gray-500 uppercase w-32">
                              {t('observation.col.level')}
                            </th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-50">
                          {indicators.map((ind) => {
                            const currentLevel = localResults[ind.id]
                            return (
                              <tr key={ind.id} className="hover:bg-gray-50">
                                <td className="px-5 py-3 text-gray-600 text-xs">{ind.criterion}</td>
                                <td className="px-5 py-3 text-gray-700">{ind.indicator}</td>
                                <td className="px-5 py-3">
                                  <div className="flex gap-1">
                                    {LEVELS.map((l) => {
                                      const active = currentLevel === l.value
                                      return (
                                        <button key={l.value} type="button"
                                          onClick={() => handleLevelClick(ind.id, l.value)}
                                          title={l.title}
                                          className={`w-8 h-8 rounded border text-xs font-bold transition-all ${active ? l.color + ' border-2' : 'bg-white text-gray-300 border-gray-200 hover:border-gray-400'}`}>
                                          {l.label}
                                        </button>
                                      )
                                    })}
                                  </div>
                                </td>
                              </tr>
                            )
                          })}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </>
      )}
    </div>
  )
}
