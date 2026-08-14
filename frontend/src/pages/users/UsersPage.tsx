import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, UserX, UserCheck, Users } from 'lucide-react'
import { userApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import {
  Button, Table, Th, Td, Modal, Input, Select, Spinner, Empty, Pagination,
} from '@/components/common'
import type { User, RoleCode } from '@/types'
import { format, parseISO } from 'date-fns'

const ROLES: { value: RoleCode; label: string }[] = [
  { value: 'SYSTEM_ADMIN',   label: 'Системный админ' },
  { value: 'FOUNDER',        label: 'Учредитель' },
  { value: 'DIRECTOR',       label: 'Руководитель' },
  { value: 'METHODIST',      label: 'Методист' },
  { value: 'EDUCATOR',       label: 'Воспитатель' },
  { value: 'KAZ_TEACHER',    label: 'Педагог казахского' },
  { value: 'MUSIC_TEACHER',  label: 'Муз. руководитель' },
  { value: 'PE_INSTRUCTOR',  label: 'Инструктор по физ.' },
  { value: 'NURSE',          label: 'Медсестра' },
  { value: 'JANITOR',        label: 'Завхоз' },
  { value: 'ACCOUNTANT',     label: 'Бухгалтер' },
  { value: 'PARENT',         label: 'Родитель' },
]

const ROLE_LABELS: Record<RoleCode, string> = Object.fromEntries(
  ROLES.map((r) => [r.value, r.label])
) as Record<RoleCode, string>

const emptyForm = {
  firstName: '',
  lastName: '',
  middleName: '',
  email: '',
  phone: '',
  roleCode: '' as RoleCode | '',
  password: '',
  preferredLanguage: 'ru' as 'ru' | 'kk',
}

export default function UsersPage() {
  const qc = useQueryClient()
  const { organizationId } = useAuthStore()
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [confirmDeactivate, setConfirmDeactivate] = useState<User | null>(null)

  // ── Queries ──────────────────────────────────────────────────────────────
  const { data, isLoading } = useQuery({
    queryKey: ['users', organizationId, page],
    queryFn: () => userApi.list(organizationId!, page),
    enabled: !!organizationId,
  })

  // ── Mutations ─────────────────────────────────────────────────────────────
  const saveMutation = useMutation({
    mutationFn: (d: typeof emptyForm) => {
      const payload = {
        ...d,
        organizationId: organizationId ?? undefined,
        roleCode: d.roleCode as RoleCode,
      }
      if (editing) {
        const { password, ...updatePayload } = payload
        return userApi.update(editing.id, updatePayload)
      }
      return userApi.create({ ...payload, password: d.password })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] })
      closeModal()
    },
  })

  const deactivateMutation = useMutation({
    mutationFn: (userId: string) => userApi.deactivate(userId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users'] })
      setConfirmDeactivate(null)
    },
  })

  // ── Handlers ──────────────────────────────────────────────────────────────
  const openCreate = () => {
    setEditing(null)
    setForm(emptyForm)
    setModalOpen(true)
  }

  const openEdit = (u: User) => {
    setEditing(u)
    setForm({
      firstName: u.firstName,
      lastName: u.lastName,
      middleName: u.middleName ?? '',
      email: u.email,
      phone: u.phone ?? '',
      roleCode: u.roleCode,
      password: '',
      preferredLanguage: u.preferredLanguage,
    })
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setEditing(null)
    setForm(emptyForm)
  }

  const set =
    (k: keyof typeof emptyForm) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((f) => ({ ...f, [k]: e.target.value }))

  const pageData = data?.data.data

  // ── Helpers ───────────────────────────────────────────────────────────────
  const roleBadge = (role: RoleCode) => {
    const colorMap: Record<string, string> = {
      SYSTEM_ADMIN: 'badge-red',
      FOUNDER: 'badge-purple',
      DIRECTOR: 'badge-blue',
      METHODIST: 'badge-indigo',
      EDUCATOR: 'badge-green',
      KAZ_TEACHER: 'badge-yellow',
      MUSIC_TEACHER: 'badge-yellow',
      PE_INSTRUCTOR: 'badge-yellow',
      NURSE: 'badge-pink',
      JANITOR: 'badge-gray',
      ACCOUNTANT: 'badge-orange',
      PARENT: 'badge-teal',
    }
    return (
      <span className={colorMap[role] ?? 'badge-gray'}>
        {ROLE_LABELS[role] ?? role}
      </span>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Пользователи</h1>
          <p className="text-sm text-gray-500 mt-1">Управление сотрудниками организации</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> Добавить пользователя
        </Button>
      </div>

      {/* Table */}
      {isLoading ? (
        <Spinner />
      ) : pageData?.content.length === 0 ? (
        <Empty message="Нет пользователей" />
      ) : (
        <>
          <Table>
            <thead>
              <tr>
                <Th>ФИО</Th>
                <Th>Email / Телефон</Th>
                <Th>Роль</Th>
                <Th>Язык</Th>
                <Th>Последний вход</Th>
                <Th>Статус</Th>
                <Th>{''}</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {pageData?.content.map((u) => (
                <tr key={u.id} className="hover:bg-gray-50">
                  <Td>
                    <div className="flex items-center gap-2">
                      <div className="h-8 w-8 rounded-full bg-primary-100 flex items-center justify-center shrink-0">
                        <Users className="h-4 w-4 text-primary-600" />
                      </div>
                      <span className="font-medium">
                        {u.lastName} {u.firstName} {u.middleName ?? ''}
                      </span>
                    </div>
                  </Td>
                  <Td>
                    <div className="text-sm">
                      <div>{u.email}</div>
                      {u.phone && <div className="text-gray-400">{u.phone}</div>}
                    </div>
                  </Td>
                  <Td>{roleBadge(u.roleCode)}</Td>
                  <Td>
                    <span className="uppercase text-xs font-semibold text-gray-500">
                      {u.preferredLanguage}
                    </span>
                  </Td>
                  <Td>
                    {u.lastLoginAt
                      ? format(parseISO(u.lastLoginAt), 'dd.MM.yyyy HH:mm')
                      : '—'}
                  </Td>
                  <Td>
                    {u.active ? (
                      <span className="badge-green">Активен</span>
                    ) : (
                      <span className="badge-red">Деактивирован</span>
                    )}
                  </Td>
                  <Td>
                    <div className="flex gap-2">
                      <button
                        onClick={() => openEdit(u)}
                        className="text-gray-400 hover:text-primary-600 transition-colors"
                        title="Редактировать"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      {u.active ? (
                        <button
                          onClick={() => setConfirmDeactivate(u)}
                          className="text-gray-400 hover:text-red-600 transition-colors"
                          title="Деактивировать"
                        >
                          <UserX className="h-4 w-4" />
                        </button>
                      ) : (
                        <UserCheck className="h-4 w-4 text-gray-200" />
                      )}
                    </div>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
          {pageData && (
            <Pagination
              page={page}
              totalPages={pageData.totalPages}
              totalElements={pageData.totalElements}
              onPageChange={setPage}
            />
          )}
        </>
      )}

      {/* Create / Edit Modal */}
      <Modal
        open={modalOpen}
        onClose={closeModal}
        title={editing ? 'Редактировать пользователя' : 'Новый пользователь'}
        width="max-w-2xl"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault()
            saveMutation.mutate(form)
          }}
          className="space-y-4"
        >
          <div className="grid grid-cols-3 gap-3">
            <Input label="Фамилия *" value={form.lastName} onChange={set('lastName')} required />
            <Input label="Имя *" value={form.firstName} onChange={set('firstName')} required />
            <Input label="Отчество" value={form.middleName} onChange={set('middleName')} />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input label="Email *" type="email" value={form.email} onChange={set('email')} required />
            <Input label="Телефон" value={form.phone} onChange={set('phone')} placeholder="+7..." />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Select
              label="Роль *"
              options={ROLES}
              value={form.roleCode}
              onChange={set('roleCode')}
              placeholder="Выберите роль"
              required
            />
            <Select
              label="Язык интерфейса"
              options={[
                { value: 'ru', label: 'Русский' },
                { value: 'kk', label: 'Қазақша' },
              ]}
              value={form.preferredLanguage}
              onChange={set('preferredLanguage')}
            />
          </div>

          {!editing && (
            <Input
              label="Пароль *"
              type="password"
              value={form.password}
              onChange={set('password')}
              required={!editing}
              placeholder="Минимум 8 символов"
              minLength={8}
            />
          )}

          <div className="flex gap-3 justify-end pt-2">
            <Button type="button" variant="secondary" onClick={closeModal}>
              Отмена
            </Button>
            <Button type="submit" loading={saveMutation.isPending}>
              Сохранить
            </Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Deactivate Modal */}
      <Modal
        open={!!confirmDeactivate}
        onClose={() => setConfirmDeactivate(null)}
        title="Деактивировать пользователя"
        width="max-w-md"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Вы уверены, что хотите деактивировать пользователя{' '}
            <strong>
              {confirmDeactivate?.lastName} {confirmDeactivate?.firstName}
            </strong>
            ? Пользователь не сможет войти в систему.
          </p>
          <div className="flex gap-3 justify-end">
            <Button
              type="button"
              variant="secondary"
              onClick={() => setConfirmDeactivate(null)}
            >
              Отмена
            </Button>
            <Button
              variant="danger"
              loading={deactivateMutation.isPending}
              onClick={() =>
                confirmDeactivate && deactivateMutation.mutate(confirmDeactivate.id)
              }
            >
              Деактивировать
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
