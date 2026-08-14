import { useState, useMemo, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2, Send, Eye } from 'lucide-react'
import { scheduleApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Spinner, Empty, Input } from '@/components/common'
import type { ScheduleEntry, ScheduleData } from '@/types'

// ── Константы ─────────────────────────────────────────────────────────────────

const DAYS = [
  { key: 1, label: 'Понедельник' },
  { key: 2, label: 'Вторник' },
  { key: 3, label: 'Среда' },
  { key: 4, label: 'Четверг' },
  { key: 5, label: 'Пятница' },
]

const EDUCATOR_ROLES = [
  { value: 'EDUCATOR',      label: 'Воспитатель' },
  { value: 'KAZ_TEACHER',   label: 'Казахский язык' },
  { value: 'MUSIC_TEACHER', label: 'Музыкальный руководитель' },
  { value: 'PE_INSTRUCTOR', label: 'Инструктор по физкультуре' },
]

function currentAcademicYear(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  return m >= 9 ? `${y}-${y + 1}` : `${y - 1}-${y}`
}

function emptyEntry(day: number): DraftEntry {
  return {
    _key: Date.now() + Math.random(),
    dayOfWeek: day,
    startTime: '',
    endTime: null,
    subject: '',
    educatorRole: null,
    notes: null,
  }
}

type DraftEntry = {
  _key: number
  dayOfWeek: number
  startTime: string
  endTime: string | null
  subject: string
  educatorRole: string | null
  notes: string | null
}

function entriesToDraft(entries: ScheduleEntry[]): DraftEntry[] {
  return entries.map((e) => ({
    _key: Math.random(),
    dayOfWeek: e.dayOfWeek,
    startTime: e.startTime,
    endTime: e.endTime,
    subject: e.subject,
    educatorRole: e.educatorRole,
    notes: e.notes,
  }))
}

// ── Компонент ─────────────────────────────────────────────────────────────────

export default function SchedulePage() {
  const qc = useQueryClient()
  const { organizationId, roleCode } = useAuthStore()

  const [academicYear, setAcademicYear] = useState(currentAcademicYear())
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState<DraftEntry[]>([])

  const canEdit = ['METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')

  // ── Фильтры ──────────────────────────────────────────────────────────────

  const { data: branchesRes } = useQuery({
    queryKey: ['branches-active', organizationId],
    queryFn: () => branchApi.listActive(organizationId!),
    enabled: !!organizationId,
  })
  const branches = branchesRes?.data.data ?? []
  const activeBranch = branchId || branches[0]?.id

  const { data: groupsRes } = useQuery({
    queryKey: ['groups-list', organizationId, activeBranch],
    queryFn: () => groupApi.list(organizationId!, activeBranch, 0, 100),
    enabled: !!organizationId && !!activeBranch,
  })
  const groups = groupsRes?.data.data?.content ?? []
  const activeGroup = groupId || groups[0]?.id
  const activeGroupObj = groups.find((g) => g.id === activeGroup)

  // ── Загрузка расписания ───────────────────────────────────────────────────

  const queryKey = ['schedule', organizationId, activeBranch, activeGroup, academicYear]

  const { data: schedRes, isLoading } = useQuery({
    queryKey,
    queryFn: () => scheduleApi.get(organizationId!, activeBranch, activeGroup, academicYear),
    enabled: !!organizationId && !!activeBranch && !!activeGroup,
  })
  const schedule: ScheduleData | undefined = schedRes?.data.data ?? undefined

  // При загрузке расписания инициализируем черновик (если не в режиме редактирования)
  useEffect(() => {
    if (schedule && !editing) {
      setDraft(entriesToDraft(schedule.entries))
    }
  }, [schedule?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  // ── Группировка по дням ───────────────────────────────────────────────────

  const entriesByDay = useMemo(() => {
    const map: Record<number, DraftEntry[]> = {}
    DAYS.forEach((d) => { map[d.key] = [] })
    draft.forEach((e) => { map[e.dayOfWeek] = [...(map[e.dayOfWeek] ?? []), e] })
    return map
  }, [draft])

  // ── Мутации ──────────────────────────────────────────────────────────────

  const saveMutation = useMutation({
    mutationFn: () =>
      scheduleApi.saveEntries(
        schedule!.id,
        organizationId!,
        draft
          .filter((e) => e.subject.trim())
          .map((e) => ({
            dayOfWeek: e.dayOfWeek,
            startTime: e.startTime,
            endTime: e.endTime ?? undefined,
            subject: e.subject,
            educatorRole: e.educatorRole ?? undefined,
            notes: e.notes ?? undefined,
          }))
      ),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey })
      setEditing(false)
    },
  })

  const publishMutation = useMutation({
    mutationFn: () => scheduleApi.publish(schedule!.id, organizationId!),
    onSuccess: () => qc.invalidateQueries({ queryKey }),
  })

  // ── Управление черновиком ─────────────────────────────────────────────────

  const addEntry = (day: number) => {
    setDraft((d) => [...d, emptyEntry(day)])
  }

  const removeEntry = (key: number) => {
    setDraft((d) => d.filter((e) => e._key !== key))
  }

  const updateEntry = (key: number, field: keyof Omit<DraftEntry, '_key'>, value: string | null) => {
    setDraft((d) => d.map((e) => (e._key === key ? { ...e, [field]: value } : e)))
  }

  const startEditing = () => {
    if (schedule) setDraft(entriesToDraft(schedule.entries))
    setEditing(true)
  }

  // ── JSX ──────────────────────────────────────────────────────────────────

  return (
    <div className="space-y-4">
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Расписание ОД</h1>
          {activeGroupObj && (
            <p className="text-sm text-gray-500 mt-0.5">Группа: {activeGroupObj.name}</p>
          )}
        </div>

        <div className="flex items-center gap-2">
          {schedule?.published ? (
            <span className="flex items-center gap-1.5 text-sm text-green-600 font-medium">
              <Eye className="h-4 w-4" /> Опубликовано
            </span>
          ) : canEdit && schedule && !editing ? (
            <Button size="sm" onClick={() => publishMutation.mutate()} loading={publishMutation.isPending}>
              <Send className="h-4 w-4" /> Опубликовать
            </Button>
          ) : null}

          {canEdit && !editing && (
            <Button size="sm" variant="secondary" onClick={startEditing}>
              Редактировать
            </Button>
          )}
          {editing && (
            <>
              <Button size="sm" variant="secondary" onClick={() => setEditing(false)}>
                Отмена
              </Button>
              <Button size="sm" onClick={() => saveMutation.mutate()} loading={saveMutation.isPending}>
                Сохранить
              </Button>
            </>
          )}
        </div>
      </div>

      {/* Фильтры */}
      <div className="flex flex-wrap items-end gap-3">
        <Select
          label="Филиал"
          options={branches.map((b) => ({ value: b.id, label: b.name }))}
          value={branchId}
          onChange={(e) => { setBranchId(e.target.value); setGroupId('') }}
          placeholder="Выберите филиал"
          className="w-44"
        />
        <Select
          label="Группа"
          options={groups.map((g) => ({ value: g.id, label: g.name }))}
          value={groupId}
          onChange={(e) => setGroupId(e.target.value)}
          placeholder="Выберите группу"
          className="w-44"
        />
        <Input
          label="Учебный год"
          value={academicYear}
          onChange={(e) => setAcademicYear(e.target.value)}
          className="w-28"
        />
      </div>

      {/* Контент */}
      {!activeBranch || !activeGroup ? (
        <Empty message="Выберите филиал и группу" />
      ) : isLoading ? (
        <Spinner />
      ) : !schedule ? (
        <Empty message="Ошибка загрузки" />
      ) : editing ? (

        // ── Режим редактирования ─────────────────────────────────────────────
        <div className="space-y-4">
          {DAYS.map(({ key, label }) => (
            <div key={key} className="bg-white rounded-xl border border-gray-200 overflow-hidden">
              <div className="flex items-center justify-between bg-gray-50 px-4 py-2.5 border-b border-gray-200">
                <span className="font-semibold text-gray-700 text-sm">{label}</span>
                <button
                  type="button"
                  onClick={() => addEntry(key)}
                  className="flex items-center gap-1 text-xs text-primary-600 hover:text-primary-700"
                >
                  <Plus className="h-3.5 w-3.5" /> Добавить
                </button>
              </div>

              <div className="divide-y divide-gray-100">
                {entriesByDay[key].length === 0 ? (
                  <p className="px-4 py-3 text-sm text-gray-400 italic">Нет занятий</p>
                ) : (
                  entriesByDay[key].map((entry) => (
                    <div key={entry._key} className="flex items-center gap-2 px-4 py-2">
                      <input
                        type="time"
                        value={entry.startTime}
                        onChange={(e) => updateEntry(entry._key, 'startTime', e.target.value)}
                        className="input w-28 text-sm"
                      />
                      <span className="text-gray-400 text-sm">—</span>
                      <input
                        type="time"
                        value={entry.endTime ?? ''}
                        onChange={(e) => updateEntry(entry._key, 'endTime', e.target.value || null)}
                        className="input w-28 text-sm"
                      />
                      <input
                        type="text"
                        placeholder="Предмет / занятие"
                        value={entry.subject}
                        onChange={(e) => updateEntry(entry._key, 'subject', e.target.value)}
                        className="input flex-1 text-sm"
                      />
                      <select
                        value={entry.educatorRole ?? ''}
                        onChange={(e) => updateEntry(entry._key, 'educatorRole', e.target.value || null)}
                        className="input w-48 text-sm"
                      >
                        <option value="">Педагог (не указан)</option>
                        {EDUCATOR_ROLES.map((r) => (
                          <option key={r.value} value={r.value}>{r.label}</option>
                        ))}
                      </select>
                      <button
                        type="button"
                        onClick={() => removeEntry(entry._key)}
                        className="text-gray-300 hover:text-red-500 transition-colors"
                        aria-label="Удалить"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  ))
                )}
              </div>
            </div>
          ))}
        </div>

      ) : (

        // ── Режим просмотра ──────────────────────────────────────────────────
        <div className="overflow-x-auto rounded-xl border border-gray-200">
          <table className="min-w-full bg-white text-sm">
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase w-28">
                  Время
                </th>
                {DAYS.map((d) => (
                  <th key={d.key} className="px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase">
                    {d.label}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {schedule.entries.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                    Расписание не заполнено
                  </td>
                </tr>
              ) : (() => {
                const times = Array.from(
                  new Set(schedule.entries.map((e: ScheduleEntry) => e.startTime))
                ).sort()
                const byDayAndTime: Record<string, ScheduleEntry> = {}
                schedule.entries.forEach((e: ScheduleEntry) => {
                  byDayAndTime[`${e.dayOfWeek}_${e.startTime}`] = e
                })
                return times.map((time: string) => (
                  <tr key={time} className="hover:bg-gray-50">
                    <td className="px-4 py-3 font-medium text-gray-500 whitespace-nowrap">
                      {time}
                    </td>
                    {DAYS.map((d) => {
                      const entry: ScheduleEntry | undefined = byDayAndTime[`${d.key}_${time}`]
                      return (
                        <td key={d.key} className="px-4 py-3">
                          {entry ? (
                            <div>
                              <p className="font-medium text-gray-800">{entry.subject}</p>
                              {entry.educatorRole && (
                                <p className="text-xs text-gray-400 mt-0.5">
                                  {EDUCATOR_ROLES.find((r) => r.value === entry.educatorRole)?.label
                                    ?? entry.educatorRole}
                                </p>
                              )}
                            </div>
                          ) : (
                            <span className="text-gray-200">—</span>
                          )}
                        </td>
                      )
                    })}
                  </tr>
                ))
              })()}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
