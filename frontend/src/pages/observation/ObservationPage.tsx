import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Eye, ChevronDown, ChevronUp, Save } from 'lucide-react'
import { observationApi, branchApi, groupApi, childApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import {
  Button, Select, Spinner, Empty, Card,
} from '@/components/common'
import type { ObservationIndicator, ObservationResultEntry } from '@/types'

// Периоды наблюдения
const PERIODS = [
  { value: 'start',  label: 'Начало года' },
  { value: 'middle', label: 'Середина года' },
  { value: 'end',    label: 'Конец года' },
]

// Уровни развития
const LEVELS = [
  { value: 'H', label: 'Н', title: 'Низкий',   color: 'bg-red-100 text-red-700 border-red-300' },
  { value: 'S', label: 'С', title: 'Средний',  color: 'bg-yellow-100 text-yellow-700 border-yellow-300' },
  { value: 'V', label: 'В', title: 'Высокий',  color: 'bg-green-100 text-green-700 border-green-300' },
]

function getLevelColor(level: string) {
  return LEVELS.find((l) => l.value === level)?.color ?? 'bg-gray-100 text-gray-500 border-gray-200'
}

function academicYears() {
  const year = new Date().getFullYear()
  return [
    { value: `${year - 1}-${year}`,     label: `${year - 1}–${year}` },
    { value: `${year}-${year + 1}`,     label: `${year}–${year + 1}` },
  ]
}

// Group indicators by domain
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

  // Filters
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')
  const [childId, setChildId] = useState('')
  const [period, setPeriod] = useState('start')
  const [academicYear, setAcademicYear] = useState(() => {
    const y = new Date().getFullYear()
    const m = new Date().getMonth()
    return m >= 8 ? `${y}-${y + 1}` : `${y - 1}-${y}`
  })

  // Expanded domains
  const [expanded, setExpanded] = useState<Set<string>>(new Set())
  const toggleDomain = (d: string) =>
    setExpanded((prev) => {
      const next = new Set(prev)
      next.has(d) ? next.delete(d) : next.add(d)
      return next
    })

  // Local results state (before save)
  const [localResults, setLocalResults] = useState<Record<string, string>>({})

  // ── Data fetching ─────────────────────────────────────────────────────────
  const { data: branchesRes } = useQuery({
    queryKey: ['branches-active', organizationId],
    queryFn: () => branchApi.listActive(organizationId!),
    enabled: !!organizationId,
  })
  const branches = branchesRes?.data.data ?? []

  const { data: groupsRes } = useQuery({
    queryKey: ['groups', organizationId, branchId],
    queryFn: () => groupApi.list(organizationId!, branchId, 0, 100),
    enabled: !!organizationId && !!branchId,
  })
  const groups = groupsRes?.data.data?.content ?? []

  const { data: childrenRes } = useQuery({
    queryKey: ['children-obs', organizationId, branchId, groupId],
    queryFn: () => childApi.list(organizationId!, branchId || undefined, groupId || undefined, 0, 100),
    enabled: !!organizationId && !!groupId,
  })
  const children = childrenRes?.data.data?.content ?? []

  // Load observation
  const enabled = !!organizationId && !!branchId && !!groupId && !!childId && !!period && !!academicYear
  const { data: obsRes, isLoading: obsLoading } = useQuery({
    queryKey: ['observation', organizationId, branchId, groupId, childId, period, academicYear],
    queryFn: async () => {
      const res = await observationApi.getOrCreate(
        organizationId!, branchId, groupId, childId, period, academicYear
      )
      // Initialize local results from server
      const serverResults: Record<string, string> = {}
      for (const r of res.data.data?.results ?? []) {
        serverResults[r.indicatorId] = r.level
      }
      setLocalResults(serverResults)
      return res
    },
    enabled,
  })
  const obsData = obsRes?.data.data

  // ── Save a single result ──────────────────────────────────────────────────
  const setResultMutation = useMutation({
    mutationFn: ({ indicatorId, level }: { indicatorId: string; level: string | null }) =>
      observationApi.setResult(obsData!.id, indicatorId, level),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['observation'] })
    },
  })

  const handleLevelClick = (indicatorId: string, level: string) => {
    if (!obsData) return
    // Toggle: if same level clicked again, remove it
    const current = localResults[indicatorId]
    const newLevel = current === level ? null : level
    setLocalResults((prev) => {
      const next = { ...prev }
      if (newLevel === null) delete next[indicatorId]
      else next[indicatorId] = newLevel
      return next
    })
    setResultMutation.mutate({ indicatorId, level: newLevel })
  }

  // ── Computed stats ────────────────────────────────────────────────────────
  const totalIndicators = obsData?.indicators.length ?? 0
  const filledCount = Object.keys(localResults).length
  const fillPct = totalIndicators > 0 ? Math.round((filledCount / totalIndicators) * 100) : 0

  const levelStats = { H: 0, S: 0, V: 0 }
  for (const lvl of Object.values(localResults)) {
    if (lvl in levelStats) levelStats[lvl as keyof typeof levelStats]++
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Наблюдение за детьми</h1>
          <p className="text-sm text-gray-500 mt-1">Мониторинг развития воспитанников (ИКР)</p>
        </div>
      </div>

      {/* Filters */}
      <Card className="p-4">
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3">
          <Select
            options={branches.map((b) => ({ value: b.id, label: b.name }))}
            value={branchId}
            onChange={(e) => { setBranchId(e.target.value); setGroupId(''); setChildId('') }}
            placeholder="Филиал"
          />
          <Select
            options={groups.map((g) => ({ value: g.id, label: g.name }))}
            value={groupId}
            onChange={(e) => { setGroupId(e.target.value); setChildId('') }}
            placeholder="Группа"
            disabled={!branchId}
          />
          <Select
            options={children.map((c) => ({
              value: c.id,
              label: `${c.lastName} ${c.firstName}`,
            }))}
            value={childId}
            onChange={(e) => setChildId(e.target.value)}
            placeholder="Ребёнок"
            disabled={!groupId}
          />
          <Select
            options={PERIODS}
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
          />
          <Select
            options={academicYears()}
            value={academicYear}
            onChange={(e) => setAcademicYear(e.target.value)}
          />
        </div>
      </Card>

      {/* Content */}
      {!enabled ? (
        <Empty message="Выберите филиал, группу, ребёнка и период наблюдения" />
      ) : obsLoading ? (
        <Spinner />
      ) : !obsData ? (
        <Empty message="Не удалось загрузить данные наблюдения" />
      ) : (
        <>
          {/* Stats bar */}
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            <Card className="p-4 text-center">
              <div className="text-2xl font-bold text-primary-600">{fillPct}%</div>
              <div className="text-xs text-gray-500 mt-1">Заполнено</div>
            </Card>
            <Card className="p-4 text-center">
              <div className="text-2xl font-bold text-gray-700">{totalIndicators}</div>
              <div className="text-xs text-gray-500 mt-1">Показателей</div>
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

          {/* Progress bar */}
          <div className="w-full bg-gray-100 rounded-full h-2">
            <div
              className="bg-primary-500 h-2 rounded-full transition-all duration-300"
              style={{ width: `${fillPct}%` }}
            />
          </div>

          {/* Legend */}
          <div className="flex items-center gap-4 text-xs">
            <span className="text-gray-500 font-medium">Уровень:</span>
            {LEVELS.map((l) => (
              <span
                key={l.value}
                className={`inline-flex items-center gap-1 px-2 py-1 rounded border text-xs font-semibold ${l.color}`}
              >
                {l.label} — {l.title}
              </span>
            ))}
          </div>

          {/* Indicators by domain */}
          <div className="space-y-3">
            {Array.from(groupByDomain(obsData.indicators)).map(([domain, indicators]) => {
              const isOpen = expanded.has(domain)
              const domainFilled = indicators.filter((i) => localResults[i.id]).length
              return (
                <div key={domain} className="rounded-xl border border-gray-100 bg-white overflow-hidden">
                  {/* Domain header */}
                  <button
                    type="button"
                    onClick={() => toggleDomain(domain)}
                    className="w-full flex items-center justify-between px-5 py-4 hover:bg-gray-50 transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <Eye className="h-5 w-5 text-primary-500" />
                      <span className="font-semibold text-gray-800">{domain}</span>
                      <span className="text-xs text-gray-400">
                        {domainFilled}/{indicators.length}
                      </span>
                    </div>
                    {isOpen ? (
                      <ChevronUp className="h-4 w-4 text-gray-400" />
                    ) : (
                      <ChevronDown className="h-4 w-4 text-gray-400" />
                    )}
                  </button>

                  {/* Indicators table */}
                  {isOpen && (
                    <div className="border-t border-gray-100">
                      <table className="min-w-full text-sm">
                        <thead>
                          <tr className="bg-gray-50">
                            <th className="px-5 py-2 text-left text-xs font-semibold text-gray-500 uppercase w-1/3">
                              Критерий
                            </th>
                            <th className="px-5 py-2 text-left text-xs font-semibold text-gray-500 uppercase">
                              Показатель
                            </th>
                            <th className="px-5 py-2 text-left text-xs font-semibold text-gray-500 uppercase w-32">
                              Уровень
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
                                        <button
                                          key={l.value}
                                          type="button"
                                          onClick={() => handleLevelClick(ind.id, l.value)}
                                          title={l.title}
                                          className={`w-8 h-8 rounded border text-xs font-bold transition-all ${
                                            active
                                              ? l.color + ' border-2'
                                              : 'bg-white text-gray-300 border-gray-200 hover:border-gray-400'
                                          }`}
                                        >
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
