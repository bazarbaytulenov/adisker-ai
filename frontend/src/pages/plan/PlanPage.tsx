import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight, CheckCircle, RotateCcw, Send, Pencil, X } from 'lucide-react'
import { planApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Spinner, Empty, Input, Modal } from '@/components/common'
import type { PlanSection, PlanStatus } from '@/types'
import { clsx } from 'clsx'
import { useT } from '@/i18n'

const STATUS_COLOR: Record<PlanStatus, string> = {
  draft:    'bg-gray-100 text-gray-600',
  review:   'bg-yellow-100 text-yellow-700',
  approved: 'bg-green-100 text-green-700',
  returned: 'bg-red-100 text-red-600',
}

const DEFAULT_DOMAINS = [
  { domain: 'educator',     domainNameRu: 'Воспитатель',         ownerRole: 'EDUCATOR' },
  { domain: 'kaz_language', domainNameRu: 'Казахский язык',      ownerRole: 'KAZ_TEACHER' },
  { domain: 'music',        domainNameRu: 'Музыка',              ownerRole: 'MUSIC_TEACHER' },
  { domain: 'physical',     domainNameRu: 'Физическая культура', ownerRole: 'PE_INSTRUCTOR' },
]

function currentAcademicYear(): string {
  const now = new Date()
  const y = now.getFullYear()
  const m = now.getMonth() + 1
  return m >= 9 ? `${y}-${y + 1}` : `${y - 1}-${y}`
}

interface SectionCardProps {
  section: PlanSection
  domainLabel: string
  canEdit: boolean
  canApprove: boolean
  statusLabel: string
  contentLabel: string
  objectivesLabel: string
  materialsLabel: string
  notFilledLabel: string
  editLabel: string
  submitLabel: string
  approveLabel: string
  returnLabel: string
  onEdit: (s: PlanSection) => void
  onSubmit: (s: PlanSection) => void
  onApprove: (s: PlanSection) => void
  onReturn: (s: PlanSection) => void
}

function SectionCard({
  section, domainLabel, canEdit, canApprove,
  statusLabel, contentLabel, objectivesLabel, materialsLabel,
  notFilledLabel, editLabel, submitLabel, approveLabel, returnLabel,
  onEdit, onSubmit, onApprove, onReturn,
}: SectionCardProps) {
  const status = section.status as PlanStatus
  const empty = !section.content && !section.objectives && !section.materials

  return (
    <div className={clsx(
      'bg-white rounded-xl border p-4 space-y-3',
      status === 'returned' ? 'border-red-200' : 'border-gray-200'
    )}>
      <div className="flex items-start justify-between gap-2">
        <div>
          <h3 className="font-semibold text-gray-800">{section.domainNameRu ?? domainLabel}</h3>
          {section.ownerRole && (
            <p className="text-xs text-gray-400 mt-0.5">{section.ownerRole}</p>
          )}
        </div>
        <span className={clsx('px-2.5 py-0.5 rounded-full text-xs font-semibold shrink-0', STATUS_COLOR[status])}>
          {statusLabel}
        </span>
      </div>

      {status === 'returned' && section.returnComment && (
        <div className="flex items-start gap-2 bg-red-50 rounded-lg p-3 text-sm text-red-700">
          <RotateCcw className="h-4 w-4 mt-0.5 shrink-0" />
          <span>{section.returnComment}</span>
        </div>
      )}

      {empty ? (
        <p className="text-sm text-gray-400 italic">{notFilledLabel}</p>
      ) : (
        <div className="space-y-2 text-sm text-gray-700">
          {section.content && (
            <div>
              <span className="font-medium text-gray-500 text-xs uppercase tracking-wide">{contentLabel}</span>
              <p className="mt-1 whitespace-pre-wrap">{section.content}</p>
            </div>
          )}
          {section.objectives && (
            <div>
              <span className="font-medium text-gray-500 text-xs uppercase tracking-wide">{objectivesLabel}</span>
              <p className="mt-1 whitespace-pre-wrap">{section.objectives}</p>
            </div>
          )}
          {section.materials && (
            <div>
              <span className="font-medium text-gray-500 text-xs uppercase tracking-wide">{materialsLabel}</span>
              <p className="mt-1 whitespace-pre-wrap">{section.materials}</p>
            </div>
          )}
        </div>
      )}

      <div className="flex items-center gap-2 pt-1 border-t border-gray-100">
        {canEdit && (status === 'draft' || status === 'returned') && (
          <>
            <Button size="sm" variant="secondary" onClick={() => onEdit(section)}>
              <Pencil className="h-3 w-3" /> {editLabel}
            </Button>
            <Button size="sm" onClick={() => onSubmit(section)}>
              <Send className="h-3 w-3" /> {submitLabel}
            </Button>
          </>
        )}
        {canApprove && status === 'review' && (
          <>
            <Button size="sm" onClick={() => onApprove(section)}>
              <CheckCircle className="h-3 w-3" /> {approveLabel}
            </Button>
            <Button size="sm" variant="danger" onClick={() => onReturn(section)}>
              <RotateCcw className="h-3 w-3" /> {returnLabel}
            </Button>
          </>
        )}
        <span className="ml-auto text-xs text-gray-400">v{section.version}</span>
      </div>
    </div>
  )
}

export default function PlanPage() {
  const qc = useQueryClient()
  const { organizationId, roleCode } = useAuthStore()
  const t = useT()

  const now = new Date()
  const [academicYear, setAcademicYear] = useState(currentAcademicYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [week, setWeek] = useState(1)
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')

  const [editSection, setEditSection] = useState<PlanSection | null>(null)
  const [editDomain, setEditDomain] = useState<typeof DEFAULT_DOMAINS[0] | null>(null)
  const [editForm, setEditForm] = useState({ content: '', objectives: '', materials: '' })

  const [returnSection, setReturnSection] = useState<PlanSection | null>(null)
  const [returnComment, setReturnComment] = useState('')

  const MONTH_NAMES = Array.from({ length: 12 }, (_, i) => t(`plan.month.${i}`))

  const STATUS_LABEL: Record<PlanStatus, string> = {
    draft:    t('plan.status.draft'),
    review:   t('plan.status.review'),
    approved: t('plan.status.approved'),
    returned: t('plan.status.returned'),
  }

  const canEdit = ['EDUCATOR', 'KAZ_TEACHER', 'MUSIC_TEACHER', 'PE_INSTRUCTOR',
    'METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')
  const canApprove = ['METHODIST', 'DIRECTOR', 'SYSTEM_ADMIN'].includes(roleCode ?? '')

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

  const planQueryKey = ['plan', organizationId, activeBranch, activeGroup, academicYear, month, week]

  const { data: planRes, isLoading: planLoading } = useQuery({
    queryKey: planQueryKey,
    queryFn: () => planApi.getOrCreate(organizationId!, activeBranch as any, activeGroup, academicYear, month, week),
    enabled: !!organizationId && !!activeGroup,
  })
  const plan = planRes?.data.data

  const sectionsQueryKey = ['plan-sections', plan?.id]

  const { data: sectionsRes, isLoading: sectionsLoading } = useQuery({
    queryKey: sectionsQueryKey,
    queryFn: () => planApi.getSections(plan!.id),
    enabled: !!plan?.id,
  })
  const sections = sectionsRes?.data.data ?? []

  const invalidatePlan = () => {
    qc.invalidateQueries({ queryKey: planQueryKey })
    qc.invalidateQueries({ queryKey: sectionsQueryKey })
  }

  const saveMutation = useMutation({
    mutationFn: (domain: typeof DEFAULT_DOMAINS[0]) =>
      planApi.saveSection(plan!.id, organizationId!, {
        domain: domain.domain,
        domainNameRu: domain.domainNameRu,
        ownerRole: domain.ownerRole,
        content: editForm.content || undefined,
        objectives: editForm.objectives || undefined,
        materials: editForm.materials || undefined,
      }),
    onSuccess: () => { invalidatePlan(); setEditSection(null) },
  })

  const submitMutation = useMutation({
    mutationFn: (sectionId: string) => planApi.submitSection(sectionId, organizationId!),
    onSuccess: () => invalidatePlan(),
  })

  const approveMutation = useMutation({
    mutationFn: (sectionId: string) => planApi.approveSection(sectionId, organizationId!),
    onSuccess: () => invalidatePlan(),
  })

  const returnMutation = useMutation({
    mutationFn: ({ sectionId, comment }: { sectionId: string; comment: string }) =>
      planApi.returnSection(sectionId, comment),
    onSuccess: () => { invalidatePlan(); setReturnSection(null); setReturnComment('') },
  })

  const openEdit = (s: PlanSection) => {
    const domain = DEFAULT_DOMAINS.find((d) => d.domain === s.domain) ?? DEFAULT_DOMAINS[0]
    setEditSection(s)
    setEditDomain(domain)
    setEditForm({ content: s.content ?? '', objectives: s.objectives ?? '', materials: s.materials ?? '' })
  }

  const openNewSection = (domain: typeof DEFAULT_DOMAINS[0]) => {
    setEditSection({
      id: '', planId: plan?.id ?? '', domain: domain.domain,
      domainNameRu: domain.domainNameRu, domainNameKk: null, ownerRole: domain.ownerRole,
      ownerUserId: null, content: null, objectives: null, materials: null,
      status: 'draft', version: 1, sortOrder: DEFAULT_DOMAINS.indexOf(domain), returnComment: null,
    })
    setEditDomain(domain)
    setEditForm({ content: '', objectives: '', materials: '' })
  }

  const prevMonth = () => { if (month === 1) setMonth(12); else setMonth((m) => m - 1); setWeek(1) }
  const nextMonth = () => { if (month === 12) setMonth(1); else setMonth((m) => m + 1); setWeek(1) }

  const fillPct = plan?.fillPct ?? 0

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t('plan.title')}</h1>
          {activeGroupObj && (
            <p className="text-sm text-gray-500 mt-0.5">{t('plan.group')}: {activeGroupObj.name}</p>
          )}
        </div>
        {plan && (
          <div className="flex items-center gap-3">
            <div className="text-right">
              <p className="text-xs text-gray-500">{t('plan.filled')}</p>
              <p className="text-lg font-bold text-primary-600">{Math.round(fillPct)}%</p>
            </div>
            <div className="w-24 bg-gray-200 rounded-full h-2">
              <div className="bg-primary-600 h-2 rounded-full transition-all" style={{ width: `${Math.min(fillPct, 100)}%` }} />
            </div>
          </div>
        )}
      </div>

      <div className="flex flex-wrap items-end gap-3">
        {branches.length > 0 && (
          <Select
            label={t('plan.branch')}
            options={branches.map((b) => ({ value: b.id, label: b.name }))}
            value={branchId}
            onChange={(e) => { setBranchId(e.target.value); setGroupId('') }}
            placeholder={t('plan.selectBranch')}
            className="w-44"
          />
        )}
        <Select
          label={t('plan.group')}
          options={groups.map((g) => ({ value: g.id, label: g.name }))}
          value={groupId}
          onChange={(e) => setGroupId(e.target.value)}
          placeholder={t('plan.selectGroup')}
          className="w-44"
        />
        <Input
          label={t('plan.academicYear')}
          value={academicYear}
          onChange={(e) => setAcademicYear(e.target.value)}
          className="w-28"
        />
        <div className="flex items-center gap-2 ml-auto">
          <button onClick={prevMonth} className="p-1.5 rounded-lg hover:bg-gray-100" aria-label="prev">
            <ChevronLeft className="h-5 w-5 text-gray-600" />
          </button>
          <span className="text-base font-semibold text-gray-800 w-28 text-center">
            {MONTH_NAMES[month - 1]}
          </span>
          <button onClick={nextMonth} className="p-1.5 rounded-lg hover:bg-gray-100" aria-label="next">
            <ChevronRight className="h-5 w-5 text-gray-600" />
          </button>
        </div>
        <div className="flex items-center gap-1">
          {[1, 2, 3, 4, 5].map((w) => (
            <button
              key={w}
              onClick={() => setWeek(w)}
              className={clsx(
                'w-8 h-8 rounded-lg text-sm font-medium transition-colors',
                week === w ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
              )}
            >
              {w}
            </button>
          ))}
        </div>
      </div>

      {!activeGroup ? (
        <Empty message={t('plan.selectGroupHint')} />
      ) : planLoading || sectionsLoading ? (
        <Spinner />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {DEFAULT_DOMAINS.map((domainDef) => {
            const section = sections.find((s) => s.domain === domainDef.domain)
            if (!section) {
              return (
                <div key={domainDef.domain} className="bg-white rounded-xl border border-dashed border-gray-300 p-4 flex items-center justify-between">
                  <div>
                    <h3 className="font-semibold text-gray-700">{domainDef.domainNameRu}</h3>
                    <p className="text-xs text-gray-400 mt-0.5">{t('plan.sectionNotCreated')}</p>
                  </div>
                  {canEdit && (
                    <Button size="sm" variant="secondary" onClick={() => openNewSection(domainDef)}>
                      {t('plan.fill')}
                    </Button>
                  )}
                </div>
              )
            }
            return (
              <SectionCard
                key={section.id}
                section={section}
                domainLabel={domainDef.domainNameRu}
                canEdit={canEdit}
                canApprove={canApprove}
                statusLabel={STATUS_LABEL[section.status as PlanStatus]}
                contentLabel={t('plan.content')}
                objectivesLabel={t('plan.objectives')}
                materialsLabel={t('plan.materials')}
                notFilledLabel={t('plan.notFilled')}
                editLabel={t('plan.edit')}
                submitLabel={t('plan.submit')}
                approveLabel={t('plan.approve')}
                returnLabel={t('plan.return')}
                onEdit={openEdit}
                onSubmit={(s) => submitMutation.mutate(s.id)}
                onApprove={(s) => approveMutation.mutate(s.id)}
                onReturn={(s) => setReturnSection(s)}
              />
            )
          })}
        </div>
      )}

      <Modal
        open={!!editSection}
        onClose={() => setEditSection(null)}
        title={`${editDomain?.domainNameRu ?? ''} — ${MONTH_NAMES[month - 1]}, ${t('plan.week')} ${week}`}
        width="max-w-2xl"
      >
        <div className="space-y-4">
          <div className="space-y-1">
            <label className="label">{t('plan.content')}</label>
            <textarea className="input min-h-[120px] resize-y" placeholder={t('plan.contentPlaceholder')}
              value={editForm.content} onChange={(e) => setEditForm((f) => ({ ...f, content: e.target.value }))} />
          </div>
          <div className="space-y-1">
            <label className="label">{t('plan.objectives')}</label>
            <textarea className="input min-h-[80px] resize-y" placeholder={t('plan.objectivesPlaceholder')}
              value={editForm.objectives} onChange={(e) => setEditForm((f) => ({ ...f, objectives: e.target.value }))} />
          </div>
          <div className="space-y-1">
            <label className="label">{t('plan.materials')}</label>
            <textarea className="input min-h-[60px] resize-y" placeholder={t('plan.materialsPlaceholder')}
              value={editForm.materials} onChange={(e) => setEditForm((f) => ({ ...f, materials: e.target.value }))} />
          </div>
        </div>
        <div className="flex gap-3 justify-end pt-4 border-t border-gray-100 mt-4">
          <Button variant="secondary" onClick={() => setEditSection(null)}>{t('plan.cancel')}</Button>
          <Button onClick={() => editDomain && saveMutation.mutate(editDomain)} loading={saveMutation.isPending}>
            {t('plan.save')}
          </Button>
        </div>
      </Modal>

      <Modal
        open={!!returnSection}
        onClose={() => { setReturnSection(null); setReturnComment('') }}
        title={t('plan.returnTitle')}
      >
        <div className="space-y-1">
          <label className="label">{t('plan.returnComment')}</label>
          <textarea className="input min-h-[100px] resize-y" placeholder={t('plan.returnPlaceholder')}
            value={returnComment} onChange={(e) => setReturnComment(e.target.value)} />
        </div>
        <div className="flex gap-3 justify-end pt-4 border-t border-gray-100 mt-4">
          <Button variant="secondary" onClick={() => { setReturnSection(null); setReturnComment('') }}>
            <X className="h-4 w-4" /> {t('plan.cancel')}
          </Button>
          <Button variant="danger"
            onClick={() => returnSection && returnMutation.mutate({ sectionId: returnSection.id, comment: returnComment })}
            loading={returnMutation.isPending} disabled={!returnComment.trim()}>
            <RotateCcw className="h-4 w-4" /> {t('plan.return')}
          </Button>
        </div>
      </Modal>
    </div>
  )
}
