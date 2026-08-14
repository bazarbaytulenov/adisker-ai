import { useState, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, CheckCircle, ChevronLeft, ChevronRight } from 'lucide-react'
import { cyclogramApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Spinner, Empty, Modal, Input } from '@/components/common'
import type { Cyclogram, CyclogramContent, CyclogramSlot, DayOfWeek } from '@/types'
import { clsx } from 'clsx'

// ── Константы ─────────────────────────────────────────────────────────────────

const DAYS: { key: DayOfWeek; label: string }[] = [
  { key: 'monday',    label: 'Понедельник' },
  { key: 'tuesday',   label: 'Вторник' },
  { key: 'wednesday', label: 'Среда' },
  { key: 'thursday',  label: 'Четверг' },
  { key: 'friday',    label: 'Пятница' },
]

const MONTH_NAMES = [
  'Январь', 'Февраль', 'Март', 'Апрель', 'Май', 'Июнь',
  'Июль', 'Август', 'Сентябрь', 'Октябрь', 'Ноябрь', 'Декабрь',
]

function currentAcademicYear(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  return m >= 9 ? `${y}-${y + 1}` : `${y - 1}-${y}`
}

// ── Вспомогательные функции для JSONB ────────────────────────────────────────

function parseContent(raw: string | null): CyclogramContent {
  if (!raw) return {}
  try { return JSON.parse(raw) } catch { return {} }
}

function stringifyContent(c: CyclogramContent): string {
  return JSON.stringify(c)
}

// ── Компоненты слота ──────────────────────────────────────────────────────────

interface SlotFormProps {
  slot: Partial<CyclogramSlot>
  onChange: (s: Partial<CyclogramSlot>) => void
  onRemove: () => void
}

function SlotRow({ slot, onChange, onRemove }: SlotFormProps) {
  return (
    <div className="flex items-center gap-2">
      <input
        type="text"
        placeholder="08:00-08:30"
        value={slot.time ?? ''}
        onChange={(e) => onChange({ ...slot, time: e.target.value })}
        className="input w-32 text-sm"
      />
      <input
        type="text"
        placeholder="Вид деятельности"
        value={slot.activity ?? ''}
        onChange={(e) => onChange({ ...slot, activity: e.target.value })}
        className="input flex-1 text-sm"
      />
      <button
        type="button"
        onClick={onRemove}
        className="text-gray-300 hover:text-red-500 transition-colors shrink-0"
        aria-label="Удалить строку"
      >
        <Trash2 className="h-4 w-4" />
      </button>
    </div>
  )
}

// ── Основная страница ─────────────────────────────────────────────────────────

export default function CyclogramPage() {
  const qc = useQueryClient()
  const { organizationId, roleCode } = useAuthStore()

  const now = new Date()
  const [academicYear, setAcademicYear] = useState(currentAcademicYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [week, setWeek] = useState(1)
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')

  // Модальное окно редактирования
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editContent, setEditContent] = useState<CyclogramContent>({})
  const [editCyclogramId, setEditCyclogramId] = useState<string | null>(null)

  // Модальное окно создания
  const [createModalOpen, setCreateModalOpen] = useState(false)

  const canEdit = ['EDUCATOR', 'METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')
  const canApprove = ['METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')

  // ── Фильтры: ветки и группы ───────────────────────────────────────────────
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

  // ── Загрузка циклограмм ───────────────────────────────────────────────────
  const { data: listRes, isLoading } = useQuery({
    queryKey: ['cyclograms', organizationId, activeBranch, activeGroup],
    queryFn: () => cyclogramApi.list(organizationId!, activeBranch, activeGroup),
    enabled: !!organizationId && !!activeBranch && !!activeGroup,
  })
  const cyclograms = listRes?.data.data ?? []

  // Найти циклограмму по текущим фильтрам (month + week)
  const current = cyclograms.find(
    (c) => c.month === month && c.week === week && c.academicYear === academicYear
  )

  // ── Мутации ───────────────────────────────────────────────────────────────

  const invalidate = () =>
    qc.invalidateQueries({ queryKey: ['cyclograms', organizationId, activeBranch, activeGroup] })

  const createMutation = useMutation({
    mutationFn: () =>
      cyclogramApi.create({
        organizationId: organizationId!,
        branchId: activeBranch,
        groupId: activeGroup,
        academicYear,
        month,
        week,
        language: 'ru',
        content: stringifyContent(editContent),
      }),
    onSuccess: () => { invalidate(); setCreateModalOpen(false); setEditContent({}) },
  })

  const updateMutation = useMutation({
    mutationFn: (content: CyclogramContent) =>
      cyclogramApi.update(editCyclogramId!, {
        organizationId: organizationId!,
        content: stringifyContent(content),
      }),
    onSuccess: () => { invalidate(); setEditModalOpen(false) },
  })

  const approveMutation = useMutation({
    mutationFn: (id: string) => cyclogramApi.approve(id, organizationId!),
    onSuccess: () => invalidate(),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => cyclogramApi.delete(id, organizationId!),
    onSuccess: () => invalidate(),
  })

  // ── Открыть редактор ──────────────────────────────────────────────────────

  const openEdit = (c: Cyclogram) => {
    setEditCyclogramId(c.id)
    setEditContent(parseContent(c.content))
    setEditModalOpen(true)
  }

  const openCreate = () => {
    setEditContent({})
    setCreateModalOpen(true)
  }

  // ── Управление слотами ────────────────────────────────────────────────────

  const addSlot = useCallback((day: DayOfWeek) => {
    setEditContent((prev) => ({
      ...prev,
      [day]: [...(prev[day] ?? []), { time: '', activity: '' }],
    }))
  }, [])

  const updateSlot = useCallback((day: DayOfWeek, idx: number, slot: Partial<CyclogramSlot>) => {
    setEditContent((prev) => {
      const slots = [...(prev[day] ?? [])]
      slots[idx] = slot as CyclogramSlot
      return { ...prev, [day]: slots }
    })
  }, [])

  const removeSlot = useCallback((day: DayOfWeek, idx: number) => {
    setEditContent((prev) => {
      const slots = (prev[day] ?? []).filter((_, i) => i !== idx)
      return { ...prev, [day]: slots }
    })
  }, [])

  // ── Навигация по месяцам ──────────────────────────────────────────────────

  const prevMonth = () => {
    if (month === 1) setMonth(12)
    else setMonth((m) => m - 1)
    setWeek(1)
  }
  const nextMonth = () => {
    if (month === 12) setMonth(1)
    else setMonth((m) => m + 1)
    setWeek(1)
  }

  // ── Рендер контента циклограммы ───────────────────────────────────────────

  const renderContent = (raw: string | null) => {
    const content = parseContent(raw)
    const hasData = DAYS.some((d) => (content[d.key]?.length ?? 0) > 0)
    if (!hasData) {
      return <Empty message="Циклограмма пустая — нажмите «Редактировать» для заполнения" />
    }
    return (
      <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
        {DAYS.map(({ key, label }) => (
          <div key={key} className="rounded-lg border border-gray-200 overflow-hidden">
            <div className="bg-primary-50 px-3 py-2 text-xs font-semibold text-primary-700 uppercase tracking-wide">
              {label}
            </div>
            <div className="divide-y divide-gray-100">
              {(content[key] ?? []).length === 0 ? (
                <p className="px-3 py-2 text-xs text-gray-400 italic">Пусто</p>
              ) : (
                (content[key] ?? []).map((slot, i) => (
                  <div key={i} className="px-3 py-2">
                    <p className="text-xs font-medium text-gray-500">{slot.time}</p>
                    <p className="text-sm text-gray-800">{slot.activity}</p>
                  </div>
                ))
              )}
            </div>
          </div>
        ))}
      </div>
    )
  }

  // ── Рендер формы редактирования ───────────────────────────────────────────

  const renderEditForm = () => (
    <div className="space-y-4 max-h-[65vh] overflow-y-auto pr-1">
      {DAYS.map(({ key, label }) => (
        <div key={key}>
          <div className="flex items-center justify-between mb-2">
            <span className="text-sm font-semibold text-gray-700">{label}</span>
            <button
              type="button"
              onClick={() => addSlot(key)}
              className="text-xs text-primary-600 hover:text-primary-700 flex items-center gap-1"
            >
              <Plus className="h-3 w-3" /> Добавить
            </button>
          </div>
          <div className="space-y-2">
            {(editContent[key] ?? []).map((slot, idx) => (
              <SlotRow
                key={idx}
                slot={slot}
                onChange={(s) => updateSlot(key, idx, s)}
                onRemove={() => removeSlot(key, idx)}
              />
            ))}
            {(editContent[key] ?? []).length === 0 && (
              <p className="text-xs text-gray-400 italic">Нет записей</p>
            )}
          </div>
        </div>
      ))}
    </div>
  )

  // ── JSX ───────────────────────────────────────────────────────────────────

  return (
    <div className="space-y-4">
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Циклограмма</h1>
          {activeGroupObj && (
            <p className="text-sm text-gray-500 mt-0.5">Группа: {activeGroupObj.name}</p>
          )}
        </div>
        {canEdit && activeGroup && !current && (
          <Button onClick={openCreate}>
            <Plus className="h-4 w-4" /> Создать циклограмму
          </Button>
        )}
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
          placeholder="2025-2026"
          className="w-28"
        />

        {/* Навигация по месяцу */}
        <div className="flex items-center gap-2 ml-auto">
          <button onClick={prevMonth} className="p-1.5 rounded-lg hover:bg-gray-100" aria-label="Предыдущий месяц">
            <ChevronLeft className="h-5 w-5 text-gray-600" />
          </button>
          <span className="text-base font-semibold text-gray-800 w-28 text-center">
            {MONTH_NAMES[month - 1]}
          </span>
          <button onClick={nextMonth} className="p-1.5 rounded-lg hover:bg-gray-100" aria-label="Следующий месяц">
            <ChevronRight className="h-5 w-5 text-gray-600" />
          </button>
        </div>

        {/* Выбор недели */}
        <div className="flex items-center gap-1">
          {[1, 2, 3, 4, 5].map((w) => (
            <button
              key={w}
              onClick={() => setWeek(w)}
              className={clsx(
                'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
                week === w
                  ? 'bg-primary-600 text-white'
                  : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              )}
              aria-label={`Неделя ${w}`}
            >
              {w}
            </button>
          ))}
        </div>
      </div>

      {/* Контент */}
      {!activeBranch || !activeGroup ? (
        <Empty message="Выберите филиал и группу" />
      ) : isLoading ? (
        <Spinner />
      ) : !current ? (
        <Empty message={`Циклограмма на ${MONTH_NAMES[month - 1]}, неделю ${week} не создана`} />
      ) : (
        <div className="space-y-3">
          {/* Шапка карточки */}
          <div className="flex items-center justify-between bg-white rounded-xl border border-gray-200 px-4 py-3">
            <div className="flex items-center gap-3">
              <span className={clsx(
                'px-2.5 py-0.5 rounded-full text-xs font-semibold',
                current.status === 'approved'
                  ? 'bg-green-100 text-green-700'
                  : 'bg-yellow-100 text-yellow-700'
              )}>
                {current.status === 'approved' ? 'Утверждена' : 'Черновик'}
              </span>
              <span className="text-sm text-gray-500">
                {MONTH_NAMES[current.month - 1]}, {current.week} неделя • {current.academicYear}
              </span>
              {current.generatedByAi && (
                <span className="px-2 py-0.5 rounded-full text-xs bg-purple-100 text-purple-700 font-medium">
                  AI
                </span>
              )}
            </div>
            <div className="flex items-center gap-2">
              {canApprove && current.status === 'draft' && (
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => approveMutation.mutate(current.id)}
                  loading={approveMutation.isPending}
                >
                  <CheckCircle className="h-4 w-4" /> Утвердить
                </Button>
              )}
              {canEdit && current.status !== 'approved' && (
                <Button size="sm" variant="secondary" onClick={() => openEdit(current)}>
                  <Pencil className="h-4 w-4" /> Редактировать
                </Button>
              )}
              {canApprove && (
                <Button
                  size="sm"
                  variant="danger"
                  onClick={() => {
                    if (confirm('Удалить циклограмму?')) deleteMutation.mutate(current.id)
                  }}
                  loading={deleteMutation.isPending}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              )}
            </div>
          </div>

          {/* Контент */}
          {renderContent(current.content)}
        </div>
      )}

      {/* Модал: Редактировать */}
      <Modal
        open={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        title="Редактировать циклограмму"
        width="max-w-3xl"
      >
        {renderEditForm()}
        <div className="flex gap-3 justify-end pt-4 border-t border-gray-100 mt-4">
          <Button variant="secondary" onClick={() => setEditModalOpen(false)}>Отмена</Button>
          <Button
            onClick={() => updateMutation.mutate(editContent)}
            loading={updateMutation.isPending}
          >
            Сохранить
          </Button>
        </div>
      </Modal>

      {/* Модал: Создать */}
      <Modal
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title={`Создать циклограмму — ${MONTH_NAMES[month - 1]}, неделя ${week}`}
        width="max-w-3xl"
      >
        {renderEditForm()}
        <div className="flex gap-3 justify-end pt-4 border-t border-gray-100 mt-4">
          <Button variant="secondary" onClick={() => setCreateModalOpen(false)}>Отмена</Button>
          <Button
            onClick={() => createMutation.mutate()}
            loading={createMutation.isPending}
          >
            Создать
          </Button>
        </div>
      </Modal>
    </div>
  )
}
