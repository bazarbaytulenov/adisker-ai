import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Send, Eye, EyeOff, Save } from 'lucide-react'
import { dailyPostApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Spinner, Empty } from '@/components/common'
import type { DailyPost } from '@/types'
import { clsx } from 'clsx'

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function displayDate(iso: string): string {
  const [y, m, day] = iso.split('-')
  const months = ['янв', 'фев', 'мар', 'апр', 'май', 'июн',
    'июл', 'авг', 'сен', 'окт', 'ноя', 'дек']
  return `${Number(day)} ${months[Number(m) - 1]} ${y}`
}

function addDays(iso: string, n: number): string {
  const d = new Date(iso)
  d.setDate(d.getDate() + n)
  return formatDate(d)
}

// ── Компонент ─────────────────────────────────────────────────────────────────

export default function DailyInfoPage() {
  const qc = useQueryClient()
  const { organizationId, roleCode } = useAuthStore()

  const [date, setDate] = useState(formatDate(new Date()))
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')

  // Форма редактирования
  const [form, setForm] = useState({ theme: '', description: '', homeTasks: '' })
  const [dirty, setDirty] = useState(false)

  const canEdit = ['EDUCATOR', 'METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')
  const canUnpublish = ['METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')

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

  // ── Загрузка записи ──────────────────────────────────────────────────────

  const queryKey = ['daily-post', organizationId, activeBranch, activeGroup, date]

  const { data: postRes, isLoading } = useQuery({
    queryKey,
    queryFn: () => dailyPostApi.getOrCreate(organizationId!, activeBranch, activeGroup, date),
    enabled: !!organizationId && !!activeBranch && !!activeGroup,
  })
  const post: DailyPost | undefined = postRes?.data.data

  // Синхронизация формы при загрузке/смене записи
  useEffect(() => {
    const p = postRes?.data.data
    if (p) {
      setForm({
        theme: p.theme ?? '',
        description: p.description ?? '',
        homeTasks: p.homeTasks ?? '',
      })
      setDirty(false)
    }
  }, [postRes])

  // ── Мутации ──────────────────────────────────────────────────────────────

  const saveMutation = useMutation({
    mutationFn: () =>
      dailyPostApi.save(post!.id, organizationId!, {
        branchId: post!.branchId,
        groupId: post!.groupId,
        postDate: post!.postDate,
        theme: form.theme || undefined,
        description: form.description || undefined,
        homeTasks: form.homeTasks || undefined,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey })
      setDirty(false)
    },
  })

  const publishMutation = useMutation({
    mutationFn: () => dailyPostApi.publish(post!.id, organizationId!),
    onSuccess: () => qc.invalidateQueries({ queryKey }),
  })

  const unpublishMutation = useMutation({
    mutationFn: () => dailyPostApi.unpublish(post!.id, organizationId!),
    onSuccess: () => qc.invalidateQueries({ queryKey }),
  })

  // ── Обработка формы ───────────────────────────────────────────────────────

  const handleChange = (field: keyof typeof form, value: string) => {
    setForm((f) => ({ ...f, [field]: value }))
    setDirty(true)
  }

  // ── Навигация по датам ────────────────────────────────────────────────────

  const prev = () => setDate((d) => addDays(d, -1))
  const next = () => setDate((d) => addDays(d, +1))
  const today = () => setDate(formatDate(new Date()))
  const isToday = date === formatDate(new Date())

  const published = post?.published ?? false
  const formDisabled = !canEdit || published

  // ── JSX ──────────────────────────────────────────────────────────────────

  return (
    <div className="space-y-4 max-w-3xl">
      {/* Заголовок */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Информация за день</h1>
          {activeGroupObj && (
            <p className="text-sm text-gray-500 mt-0.5">Группа: {activeGroupObj.name}</p>
          )}
        </div>

        {/* Кнопки публикации */}
        {post && canEdit && (
          <div className="flex items-center gap-2">
            {dirty && !published && (
              <Button
                size="sm"
                variant="secondary"
                onClick={() => saveMutation.mutate()}
                loading={saveMutation.isPending}
              >
                <Save className="h-4 w-4" /> Сохранить
              </Button>
            )}
            {!published ? (
              <Button
                size="sm"
                onClick={() => {
                  if (dirty) saveMutation.mutate()
                  publishMutation.mutate()
                }}
                loading={publishMutation.isPending}
              >
                <Send className="h-4 w-4" /> Опубликовать
              </Button>
            ) : canUnpublish ? (
              <Button
                size="sm"
                variant="secondary"
                onClick={() => unpublishMutation.mutate()}
                loading={unpublishMutation.isPending}
              >
                <EyeOff className="h-4 w-4" /> Снять публикацию
              </Button>
            ) : (
              <span className="flex items-center gap-1.5 text-sm text-green-600 font-medium">
                <Eye className="h-4 w-4" /> Опубликовано
              </span>
            )}
          </div>
        )}
      </div>

      {/* Фильтры + навигация по дате */}
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

        {/* Навигация по дате */}
        <div className="flex items-center gap-2 ml-auto">
          <button
            onClick={prev}
            className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
            aria-label="Предыдущий день"
          >
            <ChevronLeft className="h-5 w-5 text-gray-600" />
          </button>
          <span className="text-base font-semibold text-gray-800 w-36 text-center">
            {displayDate(date)}
          </span>
          <button
            onClick={next}
            className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors"
            aria-label="Следующий день"
          >
            <ChevronRight className="h-5 w-5 text-gray-600" />
          </button>
          {!isToday && (
            <button
              onClick={today}
              className="text-xs text-primary-600 hover:underline ml-1"
            >
              Сегодня
            </button>
          )}
        </div>
      </div>

      {/* Статус публикации */}
      {post && (
        <div className={clsx(
          'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium',
          published
            ? 'bg-green-50 text-green-700 border border-green-200'
            : 'bg-yellow-50 text-yellow-700 border border-yellow-200'
        )}>
          {published ? (
            <><Eye className="h-4 w-4" /> Запись опубликована — видна родителям</>
          ) : (
            <><EyeOff className="h-4 w-4" /> Черновик — родители не видят</>
          )}
        </div>
      )}

      {/* Форма / контент */}
      {!activeBranch || !activeGroup ? (
        <Empty message="Выберите филиал и группу" />
      ) : isLoading ? (
        <Spinner />
      ) : !post ? (
        <Empty message="Ошибка загрузки" />
      ) : (
        <div className="space-y-4 bg-white rounded-xl border border-gray-200 p-6">

          {/* Тема дня */}
          <div className="space-y-1">
            <label className="label">Тема дня</label>
            <input
              className="input"
              placeholder="Введите тему..."
              value={form.theme}
              disabled={formDisabled}
              onChange={(e) => handleChange('theme', e.target.value)}
            />
          </div>

          {/* Описание занятий */}
          <div className="space-y-1">
            <label className="label">Описание занятий</label>
            <textarea
              className="input min-h-[120px] resize-y"
              placeholder="Что проходили, чем занимались, итоги дня..."
              value={form.description}
              disabled={formDisabled}
              onChange={(e) => handleChange('description', e.target.value)}
            />
          </div>

          {/* Домашнее задание */}
          <div className="space-y-1">
            <label className="label">Домашнее задание</label>
            <textarea
              className="input min-h-[80px] resize-y"
              placeholder="Задание для родителей / домашняя работа..."
              value={form.homeTasks}
              disabled={formDisabled}
              onChange={(e) => handleChange('homeTasks', e.target.value)}
            />
          </div>

          {/* Кнопки действий внутри карточки */}
          {canEdit && !published && (
            <div className="flex gap-3 pt-2 border-t border-gray-100">
              <Button
                variant="secondary"
                onClick={() => saveMutation.mutate()}
                loading={saveMutation.isPending}
                disabled={!dirty}
              >
                <Save className="h-4 w-4" /> Сохранить черновик
              </Button>
              <Button
                onClick={() => publishMutation.mutate()}
                loading={publishMutation.isPending}
              >
                <Send className="h-4 w-4" /> Опубликовать
              </Button>
            </div>
          )}

          {/* Метаинфо */}
          <div className="text-xs text-gray-400 pt-1">
            Создано: {new Date(post.createdAt).toLocaleString('ru-KZ')}
            {post.publishedAt && (
              <> · Опубликовано: {new Date(post.publishedAt).toLocaleString('ru-KZ')}</>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
