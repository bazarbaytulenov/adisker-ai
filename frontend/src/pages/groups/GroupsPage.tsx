import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, Users } from 'lucide-react'
import { groupApi, branchApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import {
  Button, Table, Th, Td, Modal, Input, Select, Spinner, Empty, Pagination
} from '@/components/common'
import type { Group } from '@/types'
import { useT } from '@/i18n'
import { useNavigate } from 'react-router-dom'

export default function GroupsPage() {
  const t = useT()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { organizationId } = useAuthStore()
  const [page, setPage] = useState(0)
  const [branchId, setBranchId] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Group | null>(null)
  const [form, setForm] = useState({
    name: '', branchId: '', language: 'ru', groupType: '',
    ageFromMonths: '', ageToMonths: '', academicYear: '2025-2026',
    educatorPhone: '', educatorEmail: '',
  })

  const GROUP_TYPES = [
    { value: 'full_day',   label: t('groups.type.fullDay') },
    { value: 'short_day',  label: t('groups.type.shortDay') },
    { value: 'mixed_age',  label: t('groups.type.mixedAge') },
    { value: 'special',    label: t('groups.type.special') },
  ]

  const { data: branchesRes } = useQuery({
    queryKey: ['branches-active', organizationId],
    queryFn: () => branchApi.listActive(organizationId!),
    enabled: !!organizationId,
  })
  const branches = branchesRes?.data.data ?? []
  const branchOptions = branches.map((b) => ({ value: b.id, label: b.name }))
  const showBranchSelector = branches.length > 1 || (branches.length === 1 && !branches[0].isDefault)

  const { data, isLoading } = useQuery({
    queryKey: ['groups', organizationId, branchId, page],
    queryFn: () => groupApi.list(organizationId!, branchId || undefined, page),
    enabled: !!organizationId,
  })

  const activeBranchId = branchId || ''

  const emptyForm = {
    name: '', branchId: '', language: 'ru', groupType: '',
    ageFromMonths: '', ageToMonths: '', academicYear: '2025-2026',
    educatorPhone: '', educatorEmail: '',
  }

  const saveMutation = useMutation({
    mutationFn: (d: typeof emptyForm) => {
      const payload = {
        ...d,
        organizationId: organizationId ?? undefined,
        branchId: d.branchId || undefined,   // пусто → null → группа без филиала
        ageFromMonths: d.ageFromMonths ? Number(d.ageFromMonths) : undefined,
        ageToMonths: d.ageToMonths ? Number(d.ageToMonths) : undefined,
      }
      return editing ? groupApi.update(editing.id, payload as Partial<Group>) : groupApi.create(payload as Partial<Group>)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['groups'] }); closeModal() },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => groupApi.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['groups'] }),
  })

  // При открытии формы сразу прописываем branchId из загруженного списка
  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm, branchId: branchId || '' })
    setModalOpen(true)
  }
  const openEdit = (g: Group) => {
    setEditing(g)
    setForm({
      name: g.name, branchId: g.branchId, language: g.language,
      groupType: g.groupType ?? '', ageFromMonths: g.ageFromMonths?.toString() ?? '',
      ageToMonths: g.ageToMonths?.toString() ?? '', academicYear: g.academicYear ?? '2025-2026',
      educatorPhone: g.educatorPhone ?? '', educatorEmail: g.educatorEmail ?? '',
    })
    setModalOpen(true)
  }
  const closeModal = () => { setModalOpen(false); setEditing(null) }
  const set = (k: keyof typeof emptyForm) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  const pageData = data?.data.data

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t('groups.title')}</h1>
          <p className="text-sm text-gray-500 mt-1">{t('groups.subtitle')}</p>
        </div>
        <Button onClick={openCreate}><Plus className="h-4 w-4" /> {t('groups.add')}</Button>
      </div>

      {showBranchSelector && (
        <Select
          label={t('common.filterByBranch')}
          options={branchOptions}
          value={branchId}
          onChange={(e) => setBranchId(e.target.value)}
          placeholder={t('common.allBranches')}
          className="max-w-xs"
        />
      )}

      {isLoading ? <Spinner /> : pageData?.content.length === 0 ? <Empty message={t('groups.empty')} /> : (
        <>
          <Table>
            <thead>
              <tr>
                <Th>{t('groups.col.name')}</Th>
                <Th>{t('groups.col.type')}</Th>
                <Th>{t('groups.col.language')}</Th>
                <Th>{t('groups.col.ageMonths')}</Th>
                <Th>{t('groups.col.academicYear')}</Th>
                <Th>{t('common.status')}</Th>
                <Th>{''}</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {pageData?.content.map((g) => (
                <tr key={g.id} className="hover:bg-gray-50">
                  <Td>
                    <button
                      onClick={() => {
                        const params = new URLSearchParams({ groupId: g.id })
                        if (g.branchId) params.set('branchId', g.branchId)
                        navigate(`/children?${params.toString()}`)
                      }}
                      className="font-medium text-primary-600 hover:text-primary-800 hover:underline flex items-center gap-1"
                      title={t('nav.children')}
                    >
                      {g.name}
                      <Users className="h-3.5 w-3.5 opacity-60" />
                    </button>
                  </Td>
                  <Td>{GROUP_TYPES.find((tp) => tp.value === g.groupType)?.label ?? g.groupType ?? '—'}</Td>
                  <Td>
                    <span className={g.language === 'kk' ? 'badge-blue' : 'badge-gray'}>
                      {g.language === 'kk' ? t('groups.lang.kk') : t('groups.lang.ru')}
                    </span>
                  </Td>
                  <Td>{g.ageFromMonths && g.ageToMonths ? `${g.ageFromMonths}–${g.ageToMonths}` : '—'}</Td>
                  <Td>{g.academicYear ?? '—'}</Td>
                  <Td>
                    <span className={g.active ? 'badge-green' : 'badge-gray'}>
                      {g.active ? t('common.active') : t('common.archive')}
                    </span>
                  </Td>
                  <Td>
                    <div className="flex items-center gap-2">
                      <button onClick={() => openEdit(g)} className="text-gray-400 hover:text-primary-600 transition-colors">
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => { if (confirm(t('groups.delete.confirm'))) deleteMutation.mutate(g.id) }}
                        className="text-gray-400 hover:text-red-600 transition-colors"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
          {pageData && <Pagination page={page} totalPages={pageData.totalPages} totalElements={pageData.totalElements} onPageChange={setPage} />}
        </>
      )}

      <Modal open={modalOpen} onClose={closeModal} title={editing ? t('groups.modal.edit') : t('groups.modal.create')}>
        <form onSubmit={(e) => { e.preventDefault(); saveMutation.mutate(form) }} className="space-y-4">
          <Input label={t('groups.field.name')} value={form.name} onChange={set('name')} required />
          {showBranchSelector && (
            <Select label={t('groups.field.branch')} options={branchOptions} value={form.branchId} onChange={set('branchId')} required />
          )}
          <div className="grid grid-cols-2 gap-3">
            <Select
              label={t('groups.field.language')}
              options={[
                { value: 'ru', label: t('groups.lang.ru') },
                { value: 'kk', label: t('groups.lang.kk') },
              ]}
              value={form.language}
              onChange={set('language')}
            />
            <Select
              label={t('groups.field.type')}
              options={GROUP_TYPES}
              value={form.groupType}
              onChange={set('groupType')}
              placeholder={t('groups.type.notSet')}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label={t('groups.field.ageFrom')} type="number" value={form.ageFromMonths} onChange={set('ageFromMonths')} />
            <Input label={t('groups.field.ageTo')} type="number" value={form.ageToMonths} onChange={set('ageToMonths')} />
          </div>
          <Input label={t('groups.field.academicYear')} value={form.academicYear} onChange={set('academicYear')} placeholder="2025-2026" />
          <div className="flex gap-3 justify-end pt-2">
            <Button type="button" variant="secondary" onClick={closeModal}>{t('common.cancel')}</Button>
            <Button type="submit" loading={saveMutation.isPending}>{t('common.save')}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
