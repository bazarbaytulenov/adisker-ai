import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Baby } from 'lucide-react'
import { childApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import {
  Button, Table, Th, Td, Modal, Input, Select, Spinner, Empty, Pagination
} from '@/components/common'
import type { Child } from '@/types'
import { format, differenceInMonths, parseISO } from 'date-fns'

const emptyForm = {
  lastName: '', firstName: '', middleName: '', birthDate: '',
  gender: '', iin: '', branchId: '', groupId: '',
  admissionDate: '', admissionOrderNum: '', parentName: '', parentPhone: '',
}

function ageLabel(birthDate: string) {
  const months = differenceInMonths(new Date(), parseISO(birthDate))
  const y = Math.floor(months / 12)
  const m = months % 12
  return y > 0 ? `${y} л. ${m} мес.` : `${m} мес.`
}

export default function ChildrenPage() {
  const qc = useQueryClient()
  const { organizationId } = useAuthStore()
  const [page, setPage] = useState(0)
  const [branchFilter, setBranchFilter] = useState('')
  const [groupFilter, setGroupFilter] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Child | null>(null)
  const [form, setForm] = useState(emptyForm)

  const { data: branchesRes } = useQuery({
    queryKey: ['branches-active', organizationId],
    queryFn: () => branchApi.listActive(organizationId!),
    enabled: !!organizationId,
  })
  const branches = branchesRes?.data.data ?? []

  const { data: groupsRes } = useQuery({
    queryKey: ['groups-active', organizationId, branchFilter],
    queryFn: () => groupApi.list(organizationId!, branchFilter || branches[0]?.id, 0, 100),
    enabled: !!organizationId && (!!branchFilter || branches.length > 0),
  })
  const groups = groupsRes?.data.data?.content ?? []

  const { data, isLoading } = useQuery({
    queryKey: ['children', organizationId, branchFilter, groupFilter, page],
    queryFn: () => childApi.list(organizationId!, branchFilter || undefined, groupFilter || undefined, page),
    enabled: !!organizationId,
  })

  const saveMutation = useMutation({
    mutationFn: (d: typeof form) => {
      const payload = { ...d, organizationId: organizationId ?? undefined, branchId: d.branchId || branches[0]?.id }
      return editing ? childApi.update(editing.id, payload as Partial<Child>) : childApi.create(payload as Partial<Child>)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['children'] }); closeModal() },
  })

  const openCreate = () => {
    setEditing(null)
    setForm({ ...emptyForm, branchId: branchFilter || branches[0]?.id || '' })
    setModalOpen(true)
  }
  const openEdit = (c: Child) => {
    setEditing(c)
    setForm({
      lastName: c.lastName, firstName: c.firstName, middleName: c.middleName ?? '',
      birthDate: c.birthDate, gender: c.gender ?? '', iin: c.iin ?? '',
      branchId: c.branchId, groupId: c.groupId ?? '',
      admissionDate: c.admissionDate ?? '', admissionOrderNum: c.admissionOrderNum ?? '',
      parentName: '', parentPhone: '',
    })
    setModalOpen(true)
  }
  const closeModal = () => { setModalOpen(false); setEditing(null) }
  const set = (k: keyof typeof emptyForm) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm((f) => ({ ...f, [k]: e.target.value }))

  const pageData = data?.data.data

  const statusBadge = (s: Child['status']) => {
    const map = { active: 'badge-green', transferred: 'badge-blue', discharged: 'badge-red', graduated: 'badge-yellow' }
    const labels = { active: 'Активен', transferred: 'Переведён', discharged: 'Выбыл', graduated: 'Выпускник' }
    return <span className={map[s]}>{labels[s]}</span>
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Контингент</h1>
          <p className="text-sm text-gray-500 mt-1">Реестр воспитанников</p>
        </div>
        <Button onClick={openCreate}><Plus className="h-4 w-4" /> Добавить ребёнка</Button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-3">
        <Select
          options={branches.map((b) => ({ value: b.id, label: b.name }))}
          value={branchFilter}
          onChange={(e) => { setBranchFilter(e.target.value); setGroupFilter('') }}
          placeholder="Все филиалы"
          className="w-48"
        />
        <Select
          options={groups.map((g) => ({ value: g.id, label: g.name }))}
          value={groupFilter}
          onChange={(e) => setGroupFilter(e.target.value)}
          placeholder="Все группы"
          className="w-48"
        />
      </div>

      {isLoading ? <Spinner /> : pageData?.content.length === 0 ? <Empty message="Нет воспитанников" /> : (
        <>
          <Table>
            <thead>
              <tr>
                <Th>ФИО</Th>
                <Th>Дата рождения</Th>
                <Th>Возраст</Th>
                <Th>Пол</Th>
                <Th>Дата приёма</Th>
                <Th>Статус</Th>
                <Th>{''}</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {pageData?.content.map((c) => (
                <tr key={c.id} className="hover:bg-gray-50">
                  <Td>
                    <div className="flex items-center gap-2">
                      <div className="h-8 w-8 rounded-full bg-primary-100 flex items-center justify-center shrink-0">
                        <Baby className="h-4 w-4 text-primary-600" />
                      </div>
                      <span className="font-medium">{c.lastName} {c.firstName} {c.middleName ?? ''}</span>
                    </div>
                  </Td>
                  <Td>{c.birthDate ? format(parseISO(c.birthDate), 'dd.MM.yyyy') : '—'}</Td>
                  <Td>{c.birthDate ? ageLabel(c.birthDate) : '—'}</Td>
                  <Td>{c.gender === 'male' ? '♂ Мальчик' : c.gender === 'female' ? '♀ Девочка' : '—'}</Td>
                  <Td>{c.admissionDate ? format(parseISO(c.admissionDate), 'dd.MM.yyyy') : '—'}</Td>
                  <Td>{statusBadge(c.status)}</Td>
                  <Td>
                    <button onClick={() => openEdit(c)} className="text-gray-400 hover:text-primary-600 transition-colors">
                      <Pencil className="h-4 w-4" />
                    </button>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
          {pageData && <Pagination page={page} totalPages={pageData.totalPages} totalElements={pageData.totalElements} onPageChange={setPage} />}
        </>
      )}

      <Modal open={modalOpen} onClose={closeModal} title={editing ? 'Редактировать воспитанника' : 'Новый воспитанник'} width="max-w-2xl">
        <form onSubmit={(e) => { e.preventDefault(); saveMutation.mutate(form) }} className="space-y-4">
          <div className="grid grid-cols-3 gap-3">
            <Input label="Фамилия *" value={form.lastName} onChange={set('lastName')} required />
            <Input label="Имя *" value={form.firstName} onChange={set('firstName')} required />
            <Input label="Отчество" value={form.middleName} onChange={set('middleName')} />
          </div>
          <div className="grid grid-cols-3 gap-3">
            <Input label="Дата рождения *" type="date" value={form.birthDate} onChange={set('birthDate')} required />
            <Select label="Пол" options={[{ value: 'male', label: 'Мальчик' }, { value: 'female', label: 'Девочка' }]} value={form.gender} onChange={set('gender')} placeholder="Не указан" />
            <Input label="ИИН" value={form.iin} onChange={set('iin')} maxLength={12} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Select label="Филиал *" options={branches.map((b) => ({ value: b.id, label: b.name }))} value={form.branchId} onChange={set('branchId')} required />
            <Select label="Группа" options={groups.map((g) => ({ value: g.id, label: g.name }))} value={form.groupId} onChange={set('groupId')} placeholder="Не назначена" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="Дата приёма" type="date" value={form.admissionDate} onChange={set('admissionDate')} />
            <Input label="№ приказа о приёме" value={form.admissionOrderNum} onChange={set('admissionOrderNum')} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input label="ФИО родителя" value={form.parentName} onChange={set('parentName')} />
            <Input label="Телефон родителя" value={form.parentPhone} onChange={set('parentPhone')} />
          </div>
          <div className="flex gap-3 justify-end pt-2">
            <Button type="button" variant="secondary" onClick={closeModal}>Отмена</Button>
            <Button type="submit" loading={saveMutation.isPending}>Сохранить</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
