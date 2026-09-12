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
import { useT } from '@/i18n'

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
  const t = useT()
  const qc = useQueryClient()
  const { organizationId } = useAuthStore()
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<User | null>(null)
  const [form, setForm] = useState(emptyForm)
  const [confirmDeactivate, setConfirmDeactivate] = useState<User | null>(null)

  // Role labels using translations
  const ROLES: { value: RoleCode; label: string }[] = [
    { value: 'SYSTEM_ADMIN',   label: t('users.role.systemAdmin') },
    { value: 'FOUNDER',        label: t('users.role.founder') },
    { value: 'DIRECTOR',       label: t('users.role.director') },
    { value: 'METHODIST',      label: t('users.role.methodist') },
    { value: 'EDUCATOR',       label: t('users.role.educator') },
    { value: 'KAZ_TEACHER',    label: t('users.role.kazTeacher') },
    { value: 'MUSIC_TEACHER',  label: t('users.role.musicTeacher') },
    { value: 'PE_INSTRUCTOR',  label: t('users.role.peInstructor') },
    { value: 'NURSE',          label: t('users.role.nurse') },
    { value: 'JANITOR',        label: t('users.role.janitor') },
    { value: 'ACCOUNTANT',     label: t('users.role.accountant') },
    { value: 'PARENT',         label: t('users.role.parent') },
  ]

  const ROLE_LABELS: Record<RoleCode, string> = Object.fromEntries(
    ROLES.map((r) => [r.value, r.label])
  ) as Record<RoleCode, string>

  // ── Queries ──────────────────────────────────────────────────────────────
  const { data, isLoading } = useQuery({
    queryKey: ['users', organizationId, page],
    queryFn: () => userApi.list(organizationId!, page),
    enabled: !!organizationId,
  })

  const { data: rolesData } = useQuery({
    queryKey: ['assignable-roles'],
    queryFn: () => userApi.assignableRoles(),
  })
  const assignable = rolesData?.data.data ?? []
  const roleOptions = ROLES.filter((r) => assignable.includes(r.value))

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
  const openCreate = () => { setEditing(null); setForm(emptyForm); setModalOpen(true) }

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

  const closeModal = () => { setModalOpen(false); setEditing(null); setForm(emptyForm) }

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
    return <span className={colorMap[role] ?? 'badge-gray'}>{ROLE_LABELS[role] ?? role}</span>
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t('users.title')}</h1>
          <p className="text-sm text-gray-500 mt-1">{t('users.subtitle')}</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> {t('users.add')}
        </Button>
      </div>

      {/* Table */}
      {isLoading ? (
        <Spinner />
      ) : pageData?.content.length === 0 ? (
        <Empty message={t('users.empty')} />
      ) : (
        <>
          <Table>
            <thead>
              <tr>
                <Th>{t('users.col.fio')}</Th>
                <Th>{t('users.col.emailPhone')}</Th>
                <Th>{t('users.col.role')}</Th>
                <Th>{t('users.col.language')}</Th>
                <Th>{t('users.col.lastLogin')}</Th>
                <Th>{t('common.status')}</Th>
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
                    {u.lastLoginAt ? format(parseISO(u.lastLoginAt), 'dd.MM.yyyy HH:mm') : '—'}
                  </Td>
                  <Td>
                    {u.active ? (
                      <span className="badge-green">{t('users.status.active')}</span>
                    ) : (
                      <span className="badge-red">{t('users.status.deactivated')}</span>
                    )}
                  </Td>
                  <Td>
                    <div className="flex gap-2">
                      <button
                        onClick={() => openEdit(u)}
                        className="text-gray-400 hover:text-primary-600 transition-colors"
                        title={t('common.edit')}
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      {u.active ? (
                        <button
                          onClick={() => setConfirmDeactivate(u)}
                          className="text-gray-400 hover:text-red-600 transition-colors"
                          title={t('users.deactivate.action')}
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
        title={editing ? t('users.modal.edit') : t('users.modal.create')}
        width="max-w-2xl"
      >
        <form
          onSubmit={(e) => { e.preventDefault(); saveMutation.mutate(form) }}
          className="space-y-4"
        >
          <div className="grid grid-cols-3 gap-3">
            <Input label={t('users.field.lastName')} value={form.lastName} onChange={set('lastName')} required />
            <Input label={t('users.field.firstName')} value={form.firstName} onChange={set('firstName')} required />
            <Input label={t('users.field.middleName')} value={form.middleName} onChange={set('middleName')} />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Input label={t('users.field.email')} type="email" value={form.email} onChange={set('email')} required />
            <Input label={t('users.field.phone')} value={form.phone} onChange={set('phone')} placeholder="+7..." />
          </div>

          <div className="grid grid-cols-2 gap-3">
            <Select
              label={t('users.field.role')}
              options={roleOptions}
              value={form.roleCode}
              onChange={set('roleCode')}
              placeholder={t('users.field.rolePlaceholder')}
              required
            />
            <Select
              label={t('users.field.language')}
              options={[
                { value: 'ru', label: t('groups.lang.ru') },
                { value: 'kk', label: t('groups.lang.kk') },
              ]}
              value={form.preferredLanguage}
              onChange={set('preferredLanguage')}
            />
          </div>

          {!editing && (
            <Input
              label={t('users.field.password')}
              type="password"
              value={form.password}
              onChange={set('password')}
              required={!editing}
              placeholder={t('users.field.passwordPlaceholder')}
              minLength={8}
            />
          )}

          <div className="flex gap-3 justify-end pt-2">
            <Button type="button" variant="secondary" onClick={closeModal}>{t('common.cancel')}</Button>
            <Button type="submit" loading={saveMutation.isPending}>{t('common.save')}</Button>
          </div>
        </form>
      </Modal>

      {/* Confirm Deactivate Modal */}
      <Modal
        open={!!confirmDeactivate}
        onClose={() => setConfirmDeactivate(null)}
        title={t('users.modal.deactivate')}
        width="max-w-md"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            {t('users.deactivate.confirm')}{' '}
            <strong>
              {confirmDeactivate?.lastName} {confirmDeactivate?.firstName}
            </strong>
            {t('users.deactivate.note')}
          </p>
          <div className="flex gap-3 justify-end">
            <Button type="button" variant="secondary" onClick={() => setConfirmDeactivate(null)}>
              {t('common.cancel')}
            </Button>
            <Button
              variant="danger"
              loading={deactivateMutation.isPending}
              onClick={() => confirmDeactivate && deactivateMutation.mutate(confirmDeactivate.id)}
            >
              {t('users.deactivate.action')}
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  )
}
