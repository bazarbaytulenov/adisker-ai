import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2 } from 'lucide-react'
import { groupApi, branchApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import {
  Button, Table, Th, Td, Modal, Input, Select, Spinner, Empty, Pagination
} from '@/components/common'
import type { Group } from '@/types'

const GROUP_TYPES = [
  { value: 'full_day',   label: 'Полный день' },
  { value: 'short_day',  label: 'Кратковременное пребывание' },
  { value: 'mixed_age',  label: 'Разновозрастная' },
  { value: 'special',    label: 'Специальная' },
]

const emptyForm = {
  name: '', branchId: '', language: 'ru', groupType: '',
  ageFromMonths: '', ageToMonths: '', academicYear: '2025-2026',
  educatorPhone: '', educatorEmail: '',
}

export default function GroupsPage() {
  const qc = useQueryClient()
  const { organizationId } = useAuthStore()
  const [page, setPage] = useState(0)
  const [branchId, setBranchId] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Group | null>(null)
  const [form, setForm] = useState(emptyForm)

  const { data: branchesRes } = useQuery({
    queryKey: ['branches-active', organizationId],
    queryFn: () => branchApi.listActive(organizationId!),
    enabled: !!organizationId,
  })
  const branches = branchesRes?.data.data ?? []
  const branchOptions = branches.map((b) => ({ value: b.id, label: b.name }))

  const { data, isLoading } = useQuery({
    queryKey: ['groups', organizationId, branchId, page],
    queryFn: () => groupApi.list(organizationId!, branchId || branches[0]?.id, page),
    enabled: !!organizationId && (!!branchId || branches.length > 0),
  })

  const activeBranchId = branchId || branches[0]?.id

  const saveMutation = useMutation({
    mutationFn: (d: typeof form) => {
      const payload = {
        ...d,
        organizationId: organizationId ?? undefined,
        branchId: d.branchId || activeBranchId,
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

  const openCreate = () => { setEditing(null); setForm({ ...emptyForm, branchId: activeBranchId }); setModalOpen(true) }
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
          <h1 className="text-2xl font-bold text-gray-900">Группы</h1>
          <p className="text-sm text-gray-500 mt-1">Возрастные группы воспитанников</p>
        </div>
        <Button onClick={openCreate}><Plus className="h-4 w-4" /> Добавить группу</Button>
      </div>

      {branchOptions.length > 1 && (
        <Select
          label="Фильтр по филиалу"
          options={branchOptions}
          value={branchId}
          onChange={(e) => setBranchId(e.target.value)}
          placeholder="Все филиалы"
          className="max-w-xs"
        />
      )}

      {isLoading ? <Spinner /> : pageData?.content.length === 0 ? <Empty message="Нет групп" /> : (
        <>
          <Table>
            <thead>
              <tr>
                <Th>Название</Th>
                <Th>Тип</Th>
                <Th>Язык</Th>
                <Th>Возраст (мес.)</Th>
                <Th>Учебный год</Th>
                <Th>Статус</Th>
                <Th>{''}</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {pageData?.content.map((g) => (
                <tr key={g.id} className="hover:bg-gray-50">
                  <Td><span className="font-medium">{g.name}</span></Td>
                  <Td>{GROUP_TYPES.find((t) => t.value === g.groupType)?.label ?? g.groupType ?? '—'}</Td>
                  <Td><span className={g.language === 'kk' ? 'badge-blue' : 'badge-gray'}>{g.language === 'kk' ? 'Қазақша' : 'Русский'}</span></Td>
                  <Td>{g.ageFromMonths && g.ageToMonths ? `${g.ageFromMonths}–${g.ageToMonths}` : '—'}</Td>
                  <Td>{g.academicYear ?? '—'}</Td>
                  <Td><span className={g.active ? 'badge-green' : 'badge-gray'}>{g.active ? 'Активна' : 'Архив'}</span></Td>
                  <Td>
                    <div className="flex items-center gap-2">
                      <button onClick={() => openEdit(g)} className="text-gray-400 hover:text-primary-600 transition-colors"><Pencil className="h-4 w-4" /></button>
                      <button onClick={() => { if (confirm('Удалить группу?')) deleteMutation.mutate(g.id) }} className="text-gray-400 hover:text-red-600 transition-colors"><Trash2 className="h-4 w-4" /></button>
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
          {pageData && <Pagination page={page} totalPages={pageData.totalPages} totalElements={pageData.totalElements} onPageChange={setPage} />}
        </>
      )}

      <Modal open={modalOpen} onClose={closeModal} title={editing ? 'Редактировать группу' : 'Новая группа'}>
        <form onSubmit={(e) => { e.preventDefault(); saveMutation.mutate(form) }} className="space-y-4">
          <Input label="Название *" value={form.name} onChange={set('name')} required />
          {branchOptions.length > 1 && (
            <Select label="Филиал *" options={branchOptions} value={form.branchId} onChange={set('branchId')} required />
          )}
          <div className="grid grid-cols-2 gap-3">
            <Select label="Язык" options={[{ value: 'ru', label: 'Русский' }, { value: 'kk', label: 'Қазақша' }]} value={form.language} onChange={set('language')} />
            <Select label="Тип группы" options={GROUP_TYPES} value={form.groupType} onChange={set('groupType')} placeholder="Не указан" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Возраст от (мес.)" type="number" value={form.ageFromMonths} onChange={set('ageFromMonths')} />
            <Input label="Возраст до (мес.)" type="number" value={form.ageToMonths} onChange={set('ageToMonths')} />
          </div>
          <Input label="Учебный год" value={form.academicYear} onChange={set('academicYear')} placeholder="2025-2026" />
          <div className="flex gap-3 justify-end pt-2">
            <Button type="button" variant="secondary" onClick={closeModal}>Отмена</Button>
            <Button type="submit" loading={saveMutation.isPending}>Сохранить</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
