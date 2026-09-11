import { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Building2, Users, KeyRound } from 'lucide-react'
import { orgApi, userApi } from '@/api'
import {
  Button, Table, Th, Td, Modal, Input, Spinner, Empty, Pagination,
} from '@/components/common'
import type { Organization, User } from '@/types'
import { format, parseISO } from 'date-fns'

/** Достаёт человекочитаемое сообщение из ошибки axios/backend, включая ошибки валидации. */
function extractError(e: any): string {
  const data = e?.response?.data
  if (data?.errors && typeof data.errors === 'object') {
    const parts = Object.entries(data.errors).map(([f, m]) => `${f}: ${m}`)
    if (parts.length) return parts.join('; ')
  }
  return data?.message ?? e?.message ?? 'Ошибка сохранения'
}

const emptyOrgForm = {
  name: '',
  legalName: '',
  bin: '',
  address: '',
  phone: '',
  email: '',
}

// Форма директора создаётся вместе с организацией (только при создании новой).
const emptyDirForm = {
  lastName: '',
  firstName: '',
  middleName: '',
  email: '',
  phone: '',
  password: '',
}

export default function OrganizationsPage() {
  const qc = useQueryClient()
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Organization | null>(null)
  const [orgForm, setOrgForm] = useState(emptyOrgForm)
  const [dirForm, setDirForm] = useState(emptyDirForm)
  const [withDirector, setWithDirector] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [adminOrg, setAdminOrg] = useState<Organization | null>(null)
  const [fieldErr, setFieldErr] = useState<Record<string, string>>({})
  const [touched, setTouched] = useState<Record<string, boolean>>({})

  // ── Валидация формы (клиентская, синхронно с backend-правилами) ─────────────
  const validate = (): Record<string, string> => {
    const e: Record<string, string> = {}
    // организация
    if (!orgForm.name.trim()) e.name = 'Укажите название'
    if (orgForm.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(orgForm.email))
      e.orgEmail = 'Некорректный email'
    if (orgForm.bin && !/^\d{12}$/.test(orgForm.bin))
      e.bin = 'БИН должен содержать 12 цифр'
    // директор (если создаём)
    if (!editing && withDirector) {
      if (!dirForm.lastName.trim()) e.lastName = 'Укажите фамилию'
      if (!dirForm.firstName.trim()) e.firstName = 'Укажите имя'
      if (!dirForm.email.trim()) e.dirEmail = 'Укажите email'
      else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(dirForm.email))
        e.dirEmail = 'Некорректный email'
      if (!dirForm.password) e.password = 'Укажите пароль'
      else if (dirForm.password.length < 6) e.password = 'Минимум 6 символов'
    }
    return e
  }

  const markTouched = (k: string) => () => setTouched((t) => ({ ...t, [k]: true }))
  // ошибку показываем, если поле трогали ИЛИ была попытка отправки (submitAttempted)
  const [submitAttempted, setSubmitAttempted] = useState(false)
  const show = (k: string, msg?: string) =>
    msg && (touched[k] || submitAttempted) ? msg : undefined

  // Живой пересчёт ошибок при вводе, чтобы подсказки исчезали по мере исправления.
  useEffect(() => {
    if (modalOpen) setFieldErr(validate())
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orgForm, dirForm, withDirector, modalOpen])

  const { data, isLoading } = useQuery({
    queryKey: ['organizations', page],
    queryFn: () => orgApi.list(page),
  })

  const saveMutation = useMutation({
    mutationFn: async () => {
      const orgPayload = {
        name: orgForm.name,
        legalName: orgForm.legalName || undefined,
        bin: orgForm.bin || undefined,
        address: orgForm.address || undefined,
        phone: orgForm.phone || undefined,
        email: orgForm.email || undefined,
      }

      if (editing) {
        await orgApi.update(editing.id, orgPayload)
        return
      }

      // 1) создать организацию
      const res = await orgApi.create(orgPayload)
      const orgId = res.data.data?.id
      if (!orgId) throw new Error('Не удалось создать организацию')

      // 2) при необходимости — создать директора этой организации
      if (withDirector) {
        try {
          await userApi.create({
            organizationId: orgId,
            email: dirForm.email,
            phone: dirForm.phone || undefined,
            firstName: dirForm.firstName,
            lastName: dirForm.lastName,
            middleName: dirForm.middleName || undefined,
            roleCode: 'DIRECTOR',
            preferredLanguage: 'ru',
            password: dirForm.password,
          })
        } catch (e) {
          // организация уже создана — сообщаем об этом явно, чтобы админа можно было
          // добавить позже через кнопку-ключ, а не создавать организацию заново
          throw new Error(
            `Организация «${orgForm.name}» создана, но администратора добавить не удалось: ` +
              extractError(e) +
              '. Добавьте администратора позже через кнопку с ключом.'
          )
        }
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['organizations'] })
      closeModal()
    },
    onError: (e: any) => {
      qc.invalidateQueries({ queryKey: ['organizations'] })
      setError(e?.message?.startsWith('Организация') ? e.message : extractError(e))
    },
  })

  const openCreate = () => {
    setEditing(null)
    setOrgForm(emptyOrgForm)
    setDirForm(emptyDirForm)
    setWithDirector(true)
    setError(null)
    setFieldErr({})
    setTouched({})
    setSubmitAttempted(false)
    setModalOpen(true)
  }

  const openEdit = (o: Organization) => {
    setEditing(o)
    setOrgForm({
      name: o.name,
      legalName: o.legalName ?? '',
      bin: o.bin ?? '',
      address: o.address ?? '',
      phone: o.phone ?? '',
      email: o.email ?? '',
    })
    setWithDirector(false)
    setError(null)
    setFieldErr({})
    setTouched({})
    setSubmitAttempted(false)
    setModalOpen(true)
  }

  const closeModal = () => {
    setModalOpen(false)
    setEditing(null)
    setError(null)
    setFieldErr({})
    setTouched({})
    setSubmitAttempted(false)
  }

  const setOrg =
    (k: keyof typeof emptyOrgForm) =>
    (e: React.ChangeEvent<HTMLInputElement>) =>
      setOrgForm((f) => ({ ...f, [k]: e.target.value }))

  const setDir =
    (k: keyof typeof emptyDirForm) =>
    (e: React.ChangeEvent<HTMLInputElement>) =>
      setDirForm((f) => ({ ...f, [k]: e.target.value }))

  const pageData = data?.data.data

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Организации</h1>
          <p className="text-sm text-gray-500 mt-1">
            Добавление дошкольных организаций и назначение их руководителей
          </p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> Добавить организацию
        </Button>
      </div>

      {isLoading ? (
        <Spinner />
      ) : pageData?.content.length === 0 ? (
        <Empty message="Нет организаций" />
      ) : (
        <>
          <Table>
            <thead>
              <tr>
                <Th>Название</Th>
                <Th>БИН</Th>
                <Th>Контакты</Th>
                <Th>Тариф/день</Th>
                <Th>Создана</Th>
                <Th>Статус</Th>
                <Th>{''}</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {pageData?.content.map((o) => (
                <tr key={o.id} className="hover:bg-gray-50">
                  <Td>
                    <div className="flex items-center gap-2">
                      <div className="h-8 w-8 rounded-full bg-primary-100 flex items-center justify-center shrink-0">
                        <Building2 className="h-4 w-4 text-primary-600" />
                      </div>
                      <div>
                        <div className="font-medium">{o.name}</div>
                        {o.legalName && (
                          <div className="text-xs text-gray-400">{o.legalName}</div>
                        )}
                      </div>
                    </div>
                  </Td>
                  <Td>{o.bin ?? '—'}</Td>
                  <Td>
                    <div className="text-sm">
                      {o.email && <div>{o.email}</div>}
                      {o.phone && <div className="text-gray-400">{o.phone}</div>}
                      {!o.email && !o.phone && '—'}
                    </div>
                  </Td>
                  <Td>{o.dailyRate != null ? `${o.dailyRate} ₸` : '—'}</Td>
                  <Td>{o.createdAt ? format(parseISO(o.createdAt), 'dd.MM.yyyy') : '—'}</Td>
                  <Td>
                    {o.active ? (
                      <span className="badge-green">Активна</span>
                    ) : (
                      <span className="badge-red">Отключена</span>
                    )}
                  </Td>
                  <Td>
                    <div className="flex gap-3">
                      <button
                        onClick={() => openEdit(o)}
                        className="text-gray-400 hover:text-primary-600 transition-colors"
                        title="Редактировать"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => setAdminOrg(o)}
                        className="text-gray-400 hover:text-primary-600 transition-colors"
                        title="Администратор организации"
                      >
                        <KeyRound className="h-4 w-4" />
                      </button>
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

      <Modal
        open={modalOpen}
        onClose={closeModal}
        title={editing ? 'Редактировать организацию' : 'Новая организация'}
        width="max-w-2xl"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault()
            setError(null)
            setSubmitAttempted(true)
            const errs = validate()
            setFieldErr(errs)
            if (Object.keys(errs).length > 0) return
            saveMutation.mutate()
          }}
          className="space-y-5"
        >
          {/* Данные организации */}
          <div className="space-y-4">
            <div className="flex items-center gap-2 text-sm font-semibold text-gray-700">
              <Building2 className="h-4 w-4 text-primary-600" /> Данные организации
            </div>
            <Input
              label="Название *"
              value={orgForm.name}
              onChange={setOrg('name')}
              onBlur={markTouched('name')}
              error={show('name', fieldErr.name)}
              required
            />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Юр. название" value={orgForm.legalName} onChange={setOrg('legalName')} />
              <Input
                label="БИН"
                value={orgForm.bin}
                onChange={setOrg('bin')}
                onBlur={markTouched('bin')}
                error={show('bin', fieldErr.bin)}
                placeholder="12 цифр"
                inputMode="numeric"
              />
            </div>
            <Input label="Адрес" value={orgForm.address} onChange={setOrg('address')} />
            <div className="grid grid-cols-2 gap-3">
              <Input
                label="Email"
                type="email"
                value={orgForm.email}
                onChange={setOrg('email')}
                onBlur={markTouched('orgEmail')}
                error={show('orgEmail', fieldErr.orgEmail)}
              />
              <Input label="Телефон" value={orgForm.phone} onChange={setOrg('phone')} placeholder="+7..." />
            </div>
          </div>

          {/* Директор — только при создании новой организации */}
          {!editing && (
            <div className="space-y-4 border-t border-gray-100 pt-4">
              <label className="flex items-center gap-2 text-sm font-semibold text-gray-700 cursor-pointer">
                <input
                  type="checkbox"
                  checked={withDirector}
                  onChange={(e) => setWithDirector(e.target.checked)}
                  className="h-4 w-4 rounded border-gray-300"
                />
                <Users className="h-4 w-4 text-primary-600" />
                Создать руководителя (директора) организации
              </label>

              {withDirector && (
                <div className="space-y-3 pl-6">
                  <div className="grid grid-cols-3 gap-3">
                    <Input
                      label="Фамилия *"
                      value={dirForm.lastName}
                      onChange={setDir('lastName')}
                      onBlur={markTouched('lastName')}
                      error={show('lastName', fieldErr.lastName)}
                      required={withDirector}
                    />
                    <Input
                      label="Имя *"
                      value={dirForm.firstName}
                      onChange={setDir('firstName')}
                      onBlur={markTouched('firstName')}
                      error={show('firstName', fieldErr.firstName)}
                      required={withDirector}
                    />
                    <Input label="Отчество" value={dirForm.middleName} onChange={setDir('middleName')} />
                  </div>
                  <div className="grid grid-cols-2 gap-3">
                    <Input
                      label="Email *"
                      type="email"
                      value={dirForm.email}
                      onChange={setDir('email')}
                      onBlur={markTouched('dirEmail')}
                      error={show('dirEmail', fieldErr.dirEmail)}
                      required={withDirector}
                    />
                    <Input label="Телефон" value={dirForm.phone} onChange={setDir('phone')} placeholder="+7..." />
                  </div>
                  <Input
                    label="Пароль *"
                    type="password"
                    value={dirForm.password}
                    onChange={setDir('password')}
                    onBlur={markTouched('password')}
                    error={show('password', fieldErr.password)}
                    required={withDirector}
                    placeholder="Минимум 6 символов"
                    minLength={6}
                  />
                </div>
              )}
            </div>
          )}

          {error && <p className="text-sm text-red-600">{error}</p>}

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

      {adminOrg && (
        <AdminModal org={adminOrg} onClose={() => setAdminOrg(null)} />
      )}
    </div>
  )
}

// ─── Управление админом (директором) организации ──────────────────────────────
function AdminModal({ org, onClose }: { org: Organization; onClose: () => void }) {
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['org-admins', org.id],
    queryFn: () => userApi.orgAdmins(org.id),
  })
  const admins = data?.data.data ?? []

  const [pwd, setPwd] = useState('')
  const [target, setTarget] = useState<User | null>(null)
  const [msg, setMsg] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)

  // форма создания нового админа, если его нет
  const [newAdmin, setNewAdmin] = useState({
    lastName: '', firstName: '', middleName: '', email: '', phone: '', password: '',
  })

  const resetMutation = useMutation({
    mutationFn: () => userApi.resetPassword(target!.id, pwd),
    onSuccess: () => { setMsg('Пароль обновлён'); setErr(null); setTarget(null); setPwd('') },
    onError: (e: any) => setErr(extractError(e)),
  })

  const createMutation = useMutation({
    mutationFn: () => userApi.create({
      organizationId: org.id,
      email: newAdmin.email,
      phone: newAdmin.phone || undefined,
      firstName: newAdmin.firstName,
      lastName: newAdmin.lastName,
      middleName: newAdmin.middleName || undefined,
      roleCode: 'DIRECTOR',
      preferredLanguage: 'ru',
      password: newAdmin.password,
    }),
    onSuccess: () => {
      setMsg('Администратор создан'); setErr(null)
      setNewAdmin({ lastName: '', firstName: '', middleName: '', email: '', phone: '', password: '' })
      refetch()
    },
    onError: (e: any) => setErr(extractError(e)),
  })

  const setNA = (k: keyof typeof newAdmin) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setNewAdmin((f) => ({ ...f, [k]: e.target.value }))

  return (
    <Modal open onClose={onClose} title={`Администратор: ${org.name}`} width="max-w-xl">
      {isLoading ? (
        <Spinner />
      ) : (
        <div className="space-y-5">
          {msg && <p className="text-sm text-green-600">{msg}</p>}
          {err && <p className="text-sm text-red-600">{err}</p>}

          {admins.length > 0 ? (
            <div className="space-y-3">
              <div className="text-sm font-semibold text-gray-700">Текущие администраторы</div>
              {admins.map((a) => (
                <div key={a.id} className="rounded-lg border border-gray-100 p-3">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-medium">{a.lastName} {a.firstName} {a.middleName ?? ''}</div>
                      <div className="text-sm text-gray-500">{a.email}</div>
                      {!a.active && <span className="badge-red mt-1 inline-block">Деактивирован</span>}
                    </div>
                    <Button size="sm" variant="secondary" onClick={() => { setTarget(a); setMsg(null); setErr(null) }}>
                      <KeyRound className="h-4 w-4" /> Сбросить пароль
                    </Button>
                  </div>

                  {target?.id === a.id && (
                    <form
                      className="mt-3 flex items-end gap-2"
                      onSubmit={(e) => { e.preventDefault(); resetMutation.mutate() }}
                    >
                      <Input
                        label="Новый пароль"
                        type="text"
                        value={pwd}
                        onChange={(e) => setPwd(e.target.value)}
                        placeholder="Минимум 6 символов"
                        minLength={6}
                        required
                        className="flex-1"
                      />
                      <Button type="submit" size="sm" loading={resetMutation.isPending}>Сохранить</Button>
                      <Button type="button" size="sm" variant="secondary" onClick={() => { setTarget(null); setPwd('') }}>Отмена</Button>
                    </form>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className="space-y-3">
              <div className="text-sm font-semibold text-gray-700">
                У организации нет администратора — создайте
              </div>
              <form
                className="space-y-3"
                onSubmit={(e) => { e.preventDefault(); createMutation.mutate() }}
              >
                <div className="grid grid-cols-3 gap-3">
                  <Input label="Фамилия *" value={newAdmin.lastName} onChange={setNA('lastName')} required />
                  <Input label="Имя *" value={newAdmin.firstName} onChange={setNA('firstName')} required />
                  <Input label="Отчество" value={newAdmin.middleName} onChange={setNA('middleName')} />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <Input label="Email *" type="email" value={newAdmin.email} onChange={setNA('email')} required />
                  <Input label="Телефон" value={newAdmin.phone} onChange={setNA('phone')} placeholder="+7..." />
                </div>
                <Input label="Пароль *" type="text" value={newAdmin.password} onChange={setNA('password')} required minLength={6} placeholder="Минимум 6 символов" />
                <div className="flex justify-end">
                  <Button type="submit" loading={createMutation.isPending}>Создать администратора</Button>
                </div>
              </form>
            </div>
          )}

          <div className="flex justify-end pt-2 border-t border-gray-100">
            <Button variant="secondary" onClick={onClose}>Закрыть</Button>
          </div>
        </div>
      )}
    </Modal>
  )
}
