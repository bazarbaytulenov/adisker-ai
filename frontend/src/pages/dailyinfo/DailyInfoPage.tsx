import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, Send, Eye, EyeOff, Save } from 'lucide-react'
import { dailyPostApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Spinner, Empty } from '@/components/common'
import type { DailyPost } from '@/types'
import { clsx } from 'clsx'
import { useT } from '@/i18n'

function formatDate(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function addDays(iso: string, n: number): string {
  const d = new Date(iso)
  d.setDate(d.getDate() + n)
  return formatDate(d)
}

export default function DailyInfoPage() {
  const qc = useQueryClient()
  const { organizationId, roleCode } = useAuthStore()
  const t = useT()

  const [date, setDate] = useState(formatDate(new Date()))
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')
  const [form, setForm] = useState({ theme: '', description: '', homeTasks: '' })
  const [dirty, setDirty] = useState(false)

  const canEdit = ['EDUCATOR', 'METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')
  const canUnpublish = ['METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')

  // Короткие названия месяцев через i18n
  const MONTH_SHORT = Array.from({ length: 12 }, (_, i) => t(`dailyInfo.month.${i}`))

  function displayDate(iso: string): string {
    const [y, m, day] = iso.split('-')
    return `${Number(day)} ${MONTH_SHORT[Number(m) - 1]} ${y}`
  }

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
  const activeGroup = groupId || groups[0]?.id
  const activeGroupObj = groups.find((g) => g.id === activeGroup)

  const queryKey = ['daily-post', organizationId, activeBranch, activeGroup, date]

  const { data: postRes, isLoading } = useQuery({
    queryKey,
    queryFn: () => dailyPostApi.getOrCreate(organizationId!, activeBranch as any, activeGroup, date),
    enabled: !!organizationId && !!activeGroup,
  })
  const post: DailyPost | undefined = postRes?.data.data

  useEffect(() => {
    const p = postRes?.data.data
    if (p) { setForm({ theme: p.theme ?? '', description: p.description ?? '', homeTasks: p.homeTasks ?? '' }); setDirty(false) }
  }, [postRes])

  const saveMutation = useMutation({
    mutationFn: () => dailyPostApi.save(post!.id, organizationId!, {
      branchId: post!.branchId, groupId: post!.groupId, postDate: post!.postDate,
      theme: form.theme || undefined, description: form.description || undefined, homeTasks: form.homeTasks || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey }); setDirty(false) },
  })

  const publishMutation = useMutation({
    mutationFn: () => dailyPostApi.publish(post!.id, organizationId!),
    onSuccess: () => qc.invalidateQueries({ queryKey }),
  })

  const unpublishMutation = useMutation({
    mutationFn: () => dailyPostApi.unpublish(post!.id, organizationId!),
    onSuccess: () => qc.invalidateQueries({ queryKey }),
  })

  const handleChange = (field: keyof typeof form, value: string) => {
    setForm((f) => ({ ...f, [field]: value }))
    setDirty(true)
  }

  const isToday = date === formatDate(new Date())
  const published = post?.published ?? false
  const formDisabled = !canEdit || published

  return (
    <div className="space-y-4 max-w-3xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t('dailyInfo.title')}</h1>
          {activeGroupObj && (
            <p className="text-sm text-gray-500 mt-0.5">{t('dailyInfo.group')}: {activeGroupObj.name}</p>
          )}
        </div>
        {post && canEdit && (
          <div className="flex items-center gap-2">
            {dirty && !published && (
              <Button size="sm" variant="secondary" onClick={() => saveMutation.mutate()} loading={saveMutation.isPending}>
                <Save className="h-4 w-4" /> {t('dailyInfo.save')}
              </Button>
            )}
            {!published ? (
              <Button size="sm" onClick={() => { if (dirty) saveMutation.mutate(); publishMutation.mutate() }} loading={publishMutation.isPending}>
                <Send className="h-4 w-4" /> {t('dailyInfo.publish')}
              </Button>
            ) : canUnpublish ? (
              <Button size="sm" variant="secondary" onClick={() => unpublishMutation.mutate()} loading={unpublishMutation.isPending}>
                <EyeOff className="h-4 w-4" /> {t('dailyInfo.unpublish')}
              </Button>
            ) : (
              <span className="flex items-center gap-1.5 text-sm text-green-600 font-medium">
                <Eye className="h-4 w-4" /> {t('dailyInfo.meta.published')}
              </span>
            )}
          </div>
        )}
      </div>

      <div className="flex flex-wrap items-end gap-3">
        {branches.length > 0 && (
          <Select label={t('dailyInfo.branch')}
            options={branches.map((b) => ({ value: b.id, label: b.name }))}
            value={branchId} onChange={(e) => { setBranchId(e.target.value); setGroupId('') }}
            placeholder={t('dailyInfo.selectBranch')} className="w-44" />
        )}
        <Select label={t('dailyInfo.group')}
          options={groups.map((g) => ({ value: g.id, label: g.name }))}
          value={groupId} onChange={(e) => setGroupId(e.target.value)}
          placeholder={t('dailyInfo.selectGroup')} className="w-44" />
        <div className="flex items-center gap-2 ml-auto">
          <button onClick={() => setDate((d) => addDays(d, -1))} className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors">
            <ChevronLeft className="h-5 w-5 text-gray-600" />
          </button>
          <span className="text-base font-semibold text-gray-800 w-36 text-center">{displayDate(date)}</span>
          <button onClick={() => setDate((d) => addDays(d, +1))} className="p-1.5 rounded-lg hover:bg-gray-100 transition-colors">
            <ChevronRight className="h-5 w-5 text-gray-600" />
          </button>
          {!isToday && (
            <button onClick={() => setDate(formatDate(new Date()))} className="text-xs text-primary-600 hover:underline ml-1">
              {t('dailyInfo.today')}
            </button>
          )}
        </div>
      </div>

      {post && (
        <div className={clsx('flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium',
          published ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-yellow-50 text-yellow-700 border border-yellow-200')}>
          {published
            ? <><Eye className="h-4 w-4" /> {t('dailyInfo.published')}</>
            : <><EyeOff className="h-4 w-4" /> {t('dailyInfo.draft')}</>}
        </div>
      )}

      {!activeGroup ? (
        <Empty message={t('dailyInfo.selectGroupHint')} />
      ) : isLoading ? (
        <Spinner />
      ) : !post ? (
        <Empty message={t('dailyInfo.loadError')} />
      ) : (
        <div className="space-y-4 bg-white rounded-xl border border-gray-200 p-6">
          <div className="space-y-1">
            <label className="label">{t('dailyInfo.field.theme')}</label>
            <input className="input" placeholder={t('dailyInfo.field.themePlaceholder')}
              value={form.theme} disabled={formDisabled} onChange={(e) => handleChange('theme', e.target.value)} />
          </div>
          <div className="space-y-1">
            <label className="label">{t('dailyInfo.field.description')}</label>
            <textarea className="input min-h-[120px] resize-y" placeholder={t('dailyInfo.field.descriptionPlaceholder')}
              value={form.description} disabled={formDisabled} onChange={(e) => handleChange('description', e.target.value)} />
          </div>
          <div className="space-y-1">
            <label className="label">{t('dailyInfo.field.homeTasks')}</label>
            <textarea className="input min-h-[80px] resize-y" placeholder={t('dailyInfo.field.homeTasksPlaceholder')}
              value={form.homeTasks} disabled={formDisabled} onChange={(e) => handleChange('homeTasks', e.target.value)} />
          </div>
          {canEdit && !published && (
            <div className="flex gap-3 pt-2 border-t border-gray-100">
              <Button variant="secondary" onClick={() => saveMutation.mutate()} loading={saveMutation.isPending} disabled={!dirty}>
                <Save className="h-4 w-4" /> {t('dailyInfo.saveDraft')}
              </Button>
              <Button onClick={() => publishMutation.mutate()} loading={publishMutation.isPending}>
                <Send className="h-4 w-4" /> {t('dailyInfo.publish')}
              </Button>
            </div>
          )}
          <div className="text-xs text-gray-400 pt-1">
            {t('dailyInfo.meta.created')}: {new Date(post.createdAt).toLocaleString('ru-KZ')}
            {post.publishedAt && <> · {t('dailyInfo.meta.published')}: {new Date(post.publishedAt).toLocaleString('ru-KZ')}</>}
          </div>
        </div>
      )}
    </div>
  )
}
