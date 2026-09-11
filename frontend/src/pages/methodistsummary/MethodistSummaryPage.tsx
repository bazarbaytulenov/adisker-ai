import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { RefreshCw, Download } from 'lucide-react'
import { methodistSummaryApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Spinner, Empty, Card } from '@/components/common'
import type { MethodistSummaryRow } from '@/types'
import { clsx } from 'clsx'

const PERIODS = [
  { value: 'start',  label: 'Начало года' },
  { value: 'middle', label: 'Середина года' },
  { value: 'end',    label: 'Конец года' },
]

function academicYears() {
  const y = new Date().getFullYear()
  const m = new Date().getMonth()
  const cur = m >= 8 ? y : y - 1
  return [
    { value: `${cur - 1}-${cur}`,   label: `${cur - 1}–${cur}` },
    { value: `${cur}-${cur + 1}`,   label: `${cur}–${cur + 1}` },
  ]
}

// Группируем строки по domain
function groupByDomain(rows: MethodistSummaryRow[]) {
  const map = new Map<string, MethodistSummaryRow[]>()
  for (const row of rows) {
    if (!map.has(row.domain)) map.set(row.domain, [])
    map.get(row.domain)!.push(row)
  }
  return map
}

// Мини-бар прогресса В/С/Н
function LevelBar({ high, mid, low }: { high: number; mid: number; low: number }) {
  const total = high + mid + low
  if (total === 0) return <span className="text-gray-300 text-xs">—</span>
  const hPct = Math.round(high / total * 100)
  const mPct = Math.round(mid  / total * 100)
  const lPct = 100 - hPct - mPct
  return (
    <div className="flex h-4 rounded overflow-hidden w-full min-w-[80px]" title={`В:${hPct}% С:${mPct}% Н:${lPct}%`}>
      {hPct > 0 && <div className="bg-green-400" style={{ width: `${hPct}%` }} />}
      {mPct > 0 && <div className="bg-yellow-400" style={{ width: `${mPct}%` }} />}
      {lPct > 0 && <div className="bg-red-400"    style={{ width: `${lPct}%` }} />}
    </div>
  )
}

export default function MethodistSummaryPage() {
  const qc = useQueryClient()
  const { organizationId, roleCode } = useAuthStore()

  const [branchId, setBranchId]       = useState('')
  const [groupId, setGroupId]         = useState('')
  const [period, setPeriod]           = useState('start')
  const [academicYear, setAcademicYear] = useState(() => {
    const y = new Date().getFullYear()
    const m = new Date().getMonth()
    return m >= 8 ? `${y}-${y + 1}` : `${y - 1}-${y}`
  })

  const canRecalculate = ['METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')

  // ── Филиалы ───────────────────────────────────────────────────────────────
  const { data: branchesRes } = useQuery({
    queryKey: ['branches-active', organizationId],
    queryFn: () => branchApi.listActive(organizationId!),
    enabled: !!organizationId,
  })
  const branches = branchesRes?.data.data ?? []
  const activeBranch = branchId || branches[0]?.id

  // ── Группы ────────────────────────────────────────────────────────────────
  const { data: groupsRes } = useQuery({
    queryKey: ['groups-list', organizationId, activeBranch],
    queryFn: () => groupApi.list(organizationId!, activeBranch, 0, 100),
    enabled: !!organizationId && !!activeBranch,
  })
  const groups = groupsRes?.data.data?.content ?? []

  const activeGroup = groupId || null

  // ── Свод ──────────────────────────────────────────────────────────────────
  const enabled = !!organizationId && !!activeBranch && !!period && !!academicYear
  const { data: summaryRes, isLoading } = useQuery({
    queryKey: ['methodist-summary', organizationId, activeBranch, activeGroup, period, academicYear],
    queryFn: () => methodistSummaryApi.getSummary(
      organizationId!, activeBranch, activeGroup, period, academicYear
    ),
    enabled,
  })
  const summary = summaryRes?.data.data

  // ── Пересчёт ──────────────────────────────────────────────────────────────
  const recalcMutation = useMutation({
    mutationFn: () => methodistSummaryApi.recalculate(
      organizationId!, activeBranch, activeGroup, period, academicYear
    ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['methodist-summary'] }),
  })

  // ── Итого по всем строкам ─────────────────────────────────────────────────
  const totals = (summary?.rows ?? []).reduce(
    (acc, r) => ({
      total: acc.total + r.totalChildren,
      high:  acc.high  + r.highCount,
      mid:   acc.mid   + r.midCount,
      low:   acc.low   + r.lowCount,
    }),
    { total: 0, high: 0, mid: 0, low: 0 }
  )
  const grandTotal = totals.high + totals.mid + totals.low

  return (
    <div className="space-y-6">
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Свод методиста</h1>
          <p className="text-sm text-gray-500 mt-1">
            Сводная таблица результатов наблюдения по направлениям развития
          </p>
        </div>
        {canRecalculate && enabled && (
          <Button
            onClick={() => recalcMutation.mutate()}
            loading={recalcMutation.isPending}
            variant="secondary"
          >
            <RefreshCw className="h-4 w-4" />
            Пересчитать
          </Button>
        )}
      </div>

      {/* Фильтры */}
      <Card className="p-4">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <Select
            label="Филиал"
            options={branches.map((b) => ({ value: b.id, label: b.name }))}
            value={branchId}
            onChange={(e) => { setBranchId(e.target.value); setGroupId('') }}
            placeholder="Все филиалы"
          />
          <Select
            label="Группа"
            options={[{ value: '', label: 'Все группы' }, ...groups.map((g) => ({ value: g.id, label: g.name }))]}
            value={groupId}
            onChange={(e) => setGroupId(e.target.value)}
          />
          <Select
            label="Период"
            options={PERIODS}
            value={period}
            onChange={(e) => setPeriod(e.target.value)}
          />
          <Select
            label="Учебный год"
            options={academicYears()}
            value={academicYear}
            onChange={(e) => setAcademicYear(e.target.value)}
          />
        </div>
      </Card>

      {/* Легенда */}
      <div className="flex items-center gap-6 text-xs text-gray-500">
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded bg-green-400 inline-block" /> В — Высокий уровень
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded bg-yellow-400 inline-block" /> С — Средний уровень
        </span>
        <span className="flex items-center gap-1.5">
          <span className="w-3 h-3 rounded bg-red-400 inline-block" /> Н — Низкий уровень
        </span>
        {summary?.cached && (
          <span className="text-gray-400 italic ml-auto">
            Данные из кэша · {summary.totalObservations > 0 ? `${summary.totalObservations} наблюдений` : ''}
          </span>
        )}
      </div>

      {/* Контент */}
      {!enabled ? (
        <Empty message="Выберите филиал, период и учебный год" />
      ) : isLoading ? (
        <Spinner />
      ) : !summary || summary.rows.length === 0 ? (
        <Empty message="Нет данных наблюдений за выбранный период. Сначала заполните наблюдения по детям." />
      ) : (
        <>
          {/* Сводные карточки */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <Card className="p-4 text-center border-l-4 border-primary-400">
              <div className="text-2xl font-bold text-primary-600">{summary.rows.length}</div>
              <div className="text-xs text-gray-500 mt-1">Направлений</div>
            </Card>
            <Card className="p-4 text-center border-l-4 border-green-400">
              <div className="text-2xl font-bold text-green-600">
                {grandTotal > 0 ? Math.round(totals.high / grandTotal * 100) : 0}%
              </div>
              <div className="text-xs text-gray-500 mt-1">Высокий уровень</div>
            </Card>
            <Card className="p-4 text-center border-l-4 border-yellow-400">
              <div className="text-2xl font-bold text-yellow-600">
                {grandTotal > 0 ? Math.round(totals.mid / grandTotal * 100) : 0}%
              </div>
              <div className="text-xs text-gray-500 mt-1">Средний уровень</div>
            </Card>
            <Card className="p-4 text-center border-l-4 border-red-400">
              <div className="text-2xl font-bold text-red-600">
                {grandTotal > 0 ? Math.round(totals.low / grandTotal * 100) : 0}%
              </div>
              <div className="text-xs text-gray-500 mt-1">Низкий уровень</div>
            </Card>
          </div>

          {/* Таблица по направлениям */}
          <div className="space-y-4">
            {Array.from(groupByDomain(summary.rows)).map(([domain, rows]) => {
              // Итог по домену
              const dTotal = rows.reduce((a, r) => a + r.highCount + r.midCount + r.lowCount, 0)
              const dHigh  = rows.reduce((a, r) => a + r.highCount, 0)
              const dMid   = rows.reduce((a, r) => a + r.midCount, 0)
              const dLow   = rows.reduce((a, r) => a + r.lowCount, 0)

              return (
                <div key={domain} className="rounded-xl border border-gray-100 bg-white overflow-hidden">
                  {/* Заголовок направления */}
                  <div className="px-5 py-3 bg-gray-50 border-b border-gray-100 flex items-center justify-between">
                    <span className="font-semibold text-gray-800">{domain}</span>
                    <div className="flex items-center gap-3 text-xs text-gray-500">
                      <span className="text-green-600 font-semibold">В: {dTotal ? Math.round(dHigh/dTotal*100) : 0}%</span>
                      <span className="text-yellow-600 font-semibold">С: {dTotal ? Math.round(dMid/dTotal*100) : 0}%</span>
                      <span className="text-red-600 font-semibold">Н: {dTotal ? Math.round(dLow/dTotal*100) : 0}%</span>
                      <div className="w-24">
                        <LevelBar high={dHigh} mid={dMid} low={dLow} />
                      </div>
                    </div>
                  </div>

                  {/* Строки по возрастным группам */}
                  <table className="min-w-full text-sm">
                    <thead>
                      <tr className="text-xs text-gray-400 uppercase border-b border-gray-50">
                        <th className="px-5 py-2 text-left">Возрастная группа</th>
                        <th className="px-4 py-2 text-center">Детей</th>
                        <th className="px-4 py-2 text-center text-green-600">В</th>
                        <th className="px-4 py-2 text-center text-green-600">В %</th>
                        <th className="px-4 py-2 text-center text-yellow-600">С</th>
                        <th className="px-4 py-2 text-center text-yellow-600">С %</th>
                        <th className="px-4 py-2 text-center text-red-600">Н</th>
                        <th className="px-4 py-2 text-center text-red-600">Н %</th>
                        <th className="px-5 py-2 text-left">Соотношение</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-50">
                      {rows.map((row) => (
                        <tr key={row.ageGroup} className="hover:bg-gray-50">
                          <td className="px-5 py-2.5 font-medium text-gray-700">
                            {row.ageGroup} лет
                          </td>
                          <td className="px-4 py-2.5 text-center text-gray-500">{row.totalChildren}</td>
                          <td className="px-4 py-2.5 text-center font-semibold text-green-600">{row.highCount}</td>
                          <td className="px-4 py-2.5 text-center text-green-600">
                            <span className={clsx(
                              'inline-block px-1.5 py-0.5 rounded text-xs font-semibold',
                              row.highPct >= 70 ? 'bg-green-100' : row.highPct >= 40 ? 'bg-yellow-50' : 'bg-red-50'
                            )}>
                              {row.highPct}%
                            </span>
                          </td>
                          <td className="px-4 py-2.5 text-center font-semibold text-yellow-600">{row.midCount}</td>
                          <td className="px-4 py-2.5 text-center text-yellow-600 text-xs">{row.midPct}%</td>
                          <td className="px-4 py-2.5 text-center font-semibold text-red-600">{row.lowCount}</td>
                          <td className="px-4 py-2.5 text-center text-red-600 text-xs">{row.lowPct}%</td>
                          <td className="px-5 py-2.5 w-40">
                            <LevelBar high={row.highCount} mid={row.midCount} low={row.lowCount} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )
            })}
          </div>
        </>
      )}
    </div>
  )
}
