import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Lock, ChevronLeft, ChevronRight } from 'lucide-react'
import { attendanceApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Spinner, Empty } from '@/components/common'
import type { MarkSymbol } from '@/types'
import { clsx } from 'clsx'
import { useT } from '@/i18n'

// ── Вспомогательные ───────────────────────────────────────────────────────────

const MARK_CYCLE: (MarkSymbol | null)[] = ['1', 'б', 'о', null]

function getDaysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate()
}

function getWeekday(year: number, month: number, day: number): number {
  return new Date(year, month - 1, day).getDay()
}

function isWeekend(year: number, month: number, day: number): boolean {
  const wd = getWeekday(year, month, day)
  return wd === 0 || wd === 6
}

const MARK_STYLE: Record<string, string> = {
  '1': 'bg-green-100 text-green-700 font-semibold',
  'б': 'bg-red-100 text-red-600 font-semibold',
  'о': 'bg-blue-100 text-blue-600 font-semibold',
}

// ── Компонент ─────────────────────────────────────────────────────────────────

export default function AttendancePage() {
  const qc = useQueryClient()
  const { organizationId, roleCode } = useAuthStore()
  const t = useT()

  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1) // 1-12
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')

  const canEdit = ['EDUCATOR', 'METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')
  const canClose = ['METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')

  // Массивы месяцев и дней недели через i18n
  const MONTH_NAMES = Array.from({ length: 12 }, (_, i) => t(`attendance.month.${i}`))
  const WEEKDAY_SHORT = Array.from({ length: 7 }, (_, i) => t(`attendance.weekday.${i}`))

  // ── Данные для фильтров ────────────────────────────────────────────────────
  const { data: branchesRes } = useQuery({
    queryKey: ['branches-active', organizationId],
    queryFn: () => branchApi.listActive(organizationId!),
    enabled: !!organizationId,
  })
  const branches = branchesRes?.data.data ?? []

  const activeBranch = branchId || undefined

  const { data: groupsRes } = useQuery({
    queryKey: ['groups-list', organizationId, activeBranch],
    queryFn: () => groupApi.list(organizationId!, activeBranch, 0, 100),
    enabled: !!organizationId,
  })
  const groups = groupsRes?.data.data?.content ?? []

  // Не автовыбираем группу — пользователь должен выбрать явно
  const activeGroup = groupId || undefined
  const activeGroupObj = groups.find((g) => g.id === groupId)

  // ── Табель ────────────────────────────────────────────────────────────────
  const { data: sheetRes, isLoading } = useQuery({
    queryKey: ['attendance-sheet', organizationId, activeBranch, activeGroup, year, month],
    queryFn: () =>
      attendanceApi.getSheet(organizationId!, activeBranch, activeGroup!, year, month),
    enabled: !!organizationId && !!activeGroup,
  })
  const sheet = sheetRes?.data.data

  // ── Мутация — установить отметку ──────────────────────────────────────────
  const markMutation = useMutation({
    mutationFn: ({ childId, day, mark }: { childId: string; day: number; mark: string | null }) =>
      attendanceApi.setMark(sheet!.monthId, organizationId!, childId, day, mark),
    onSuccess: () =>
      qc.invalidateQueries({
        queryKey: ['attendance-sheet', organizationId, activeBranch, groupId, year, month],
      }),
  })

  // ── Мутация — закрыть табель ──────────────────────────────────────────────
  const closeMutation = useMutation({
    mutationFn: () => attendanceApi.closeMonth(sheet!.monthId),
    onSuccess: () =>
      qc.invalidateQueries({
        queryKey: ['attendance-sheet', organizationId, activeBranch, groupId, year, month],
      }),
  })

  // ── Сетка дней ────────────────────────────────────────────────────────────
  const daysInMonth = useMemo(() => getDaysInMonth(year, month), [year, month])
  const days = useMemo(
    () => Array.from({ length: daysInMonth }, (_, i) => i + 1),
    [daysInMonth]
  )

  const handleCellClick = (childId: string, day: number, currentMark: string | undefined) => {
    if (!canEdit || sheet?.closed) return
    const idx = MARK_CYCLE.indexOf((currentMark ?? null) as MarkSymbol | null)
    const next = MARK_CYCLE[(idx + 1) % MARK_CYCLE.length]
    markMutation.mutate({ childId, day, mark: next })
  }

  // ── Навигация по месяцам ─────────────────────────────────────────────────
  const prevMonth = () => {
    if (month === 1) { setMonth(12); setYear((y) => y - 1) }
    else setMonth((m) => m - 1)
  }
  const nextMonth = () => {
    if (month === 12) { setMonth(1); setYear((y) => y + 1) }
    else setMonth((m) => m + 1)
  }

  // ── Итого по ребёнку ─────────────────────────────────────────────────────
  const countMarks = (marks: Record<number, string>) => {
    const present = Object.values(marks).filter((m) => m === '1').length
    const sick = Object.values(marks).filter((m) => m === 'б').length
    const vacation = Object.values(marks).filter((m) => m === 'о').length
    return { present, sick, vacation }
  }

  return (
    <div className="space-y-4">
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t('attendance.title')}</h1>
          {activeGroupObj && (
            <p className="text-sm text-gray-500 mt-0.5">
              {t('attendance.group')}: {activeGroupObj.name}
            </p>
          )}
        </div>
        {canClose && sheet && !sheet.closed && (
          <Button
            variant="danger"
            onClick={() => { if (confirm(t('attendance.closeConfirm'))) closeMutation.mutate() }}
            loading={closeMutation.isPending}
          >
            <Lock className="h-4 w-4" /> {t('attendance.close')}
          </Button>
        )}
        {sheet?.closed && (
          <span className="flex items-center gap-1.5 text-sm text-red-600 font-medium">
            <Lock className="h-4 w-4" /> {t('attendance.closed')}
          </span>
        )}
      </div>

      {/* Фильтры + навигация */}
      <div className="flex flex-wrap items-end gap-3">
        {branches.length > 0 && (
          <Select
            label={t('attendance.branch')}
            options={branches.map((b) => ({ value: b.id, label: b.name }))}
            value={branchId}
            onChange={(e) => { setBranchId(e.target.value); setGroupId('') }}
            placeholder={t('attendance.selectBranch')}
            className="w-44"
          />
        )}
        <Select
          label={t('attendance.group')}
          options={groups.map((g) => ({ value: g.id, label: g.name }))}
          value={groupId}
          onChange={(e) => setGroupId(e.target.value)}
          placeholder={t('attendance.selectGroup')}
          className="w-44"
        />
        {/* Навигация по месяцу */}
        <div className="flex items-center gap-2 ml-auto">
          <button
            onClick={prevMonth}
            className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
            aria-label="prev"
          >
            <ChevronLeft className="h-5 w-5 text-gray-600" />
          </button>
          <span className="text-base font-semibold text-gray-800 w-36 text-center">
            {MONTH_NAMES[month - 1]} {year}
          </span>
          <button
            onClick={nextMonth}
            className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
            aria-label="next"
          >
            <ChevronRight className="h-5 w-5 text-gray-600" />
          </button>
        </div>
      </div>

      {/* Легенда */}
      <div className="flex items-center gap-4 text-xs text-gray-500">
        <span className="flex items-center gap-1">
          <span className="inline-flex items-center justify-center w-5 h-5 rounded bg-green-100 text-green-700 font-semibold text-xs">1</span>
          {t('attendance.legend.present')}
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-flex items-center justify-center w-5 h-5 rounded bg-red-100 text-red-600 font-semibold text-xs">б</span>
          {t('attendance.legend.sick')}
        </span>
        <span className="flex items-center gap-1">
          <span className="inline-flex items-center justify-center w-5 h-5 rounded bg-blue-100 text-blue-600 font-semibold text-xs">о</span>
          {t('attendance.legend.vacation')}
        </span>
        {canEdit && !sheet?.closed && (
          <span className="text-gray-400 italic">{t('attendance.legend.hint')}</span>
        )}
      </div>

      {/* Таблица */}
      {!activeGroup ? (
        <Empty message={t('attendance.selectGroupHint')} />
      ) : isLoading ? (
        <Spinner />
      ) : !sheet || sheet.rows.length === 0 ? (
        <Empty message={t('attendance.empty')} />
      ) : (
        <div className="overflow-x-auto rounded-xl border border-gray-200">
          <table className="min-w-full text-xs bg-white">
            <thead>
              {/* Строка с числами */}
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="sticky left-0 z-10 bg-gray-50 px-3 py-2 text-left text-xs font-semibold text-gray-600 whitespace-nowrap min-w-[180px]">
                  {t('attendance.col.fio')}
                </th>
                {days.map((d) => (
                  <th
                    key={d}
                    className={clsx(
                      'w-8 min-w-[2rem] py-2 text-center font-semibold',
                      isWeekend(year, month, d) ? 'text-red-400' : 'text-gray-600'
                    )}
                  >
                    {d}
                  </th>
                ))}
                <th className="px-2 py-2 text-center font-semibold text-green-700 whitespace-nowrap"
                    title={t('attendance.legend.present')}>
                  {t('attendance.col.present')}
                </th>
                <th className="px-2 py-2 text-center font-semibold text-red-600 whitespace-nowrap"
                    title={t('attendance.legend.sick')}>
                  {t('attendance.col.sick')}
                </th>
                <th className="px-2 py-2 text-center font-semibold text-blue-600 whitespace-nowrap"
                    title={t('attendance.legend.vacation')}>
                  {t('attendance.col.vacation')}
                </th>
              </tr>
              {/* Строка с днями недели */}
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="sticky left-0 z-10 bg-gray-50" />
                {days.map((d) => (
                  <th
                    key={d}
                    className={clsx(
                      'w-8 py-1 text-center text-gray-400',
                      isWeekend(year, month, d) && 'text-red-300'
                    )}
                  >
                    {WEEKDAY_SHORT[getWeekday(year, month, d)]}
                  </th>
                ))}
                <th colSpan={3} />
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {sheet.rows.map((row, idx) => {
                const { present, sick, vacation } = countMarks(row.marks)
                return (
                  <tr key={row.childId} className={clsx('hover:bg-gray-50', idx % 2 === 1 && 'bg-gray-50/50')}>
                    <td className="sticky left-0 z-10 bg-inherit px-3 py-1.5 font-medium text-gray-800 whitespace-nowrap">
                      {idx + 1}. {row.fullName}
                    </td>
                    {days.map((d) => {
                      const mark = row.marks[d]
                      const weekend = isWeekend(year, month, d)
                      return (
                        <td
                          key={d}
                          onClick={() => !weekend && handleCellClick(row.childId, d, mark)}
                          className={clsx(
                            'w-8 h-8 text-center align-middle p-0',
                            weekend
                              ? 'bg-gray-100 cursor-default'
                              : canEdit && !sheet.closed
                              ? 'cursor-pointer hover:bg-primary-50 transition-colors'
                              : 'cursor-default'
                          )}
                        >
                          {mark ? (
                            <span
                              className={clsx(
                                'inline-flex items-center justify-center w-6 h-6 rounded text-xs',
                                MARK_STYLE[mark] ?? 'bg-gray-100 text-gray-600'
                              )}
                            >
                              {mark}
                            </span>
                          ) : weekend ? null : (
                            <span className="text-gray-200">·</span>
                          )}
                        </td>
                      )
                    })}
                    <td className="px-2 py-1.5 text-center font-semibold text-green-700">{present || ''}</td>
                    <td className="px-2 py-1.5 text-center font-semibold text-red-600">{sick || ''}</td>
                    <td className="px-2 py-1.5 text-center font-semibold text-blue-600">{vacation || ''}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
