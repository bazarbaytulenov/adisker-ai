import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Pencil, Trash2, Building2 } from 'lucide-react'
import { branchApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import {
  Button, Table, Th, Td, Modal, Input, Spinner, Empty, Pagination
} from '@/components/common'
import type { Branch } from '@/types'
import { useT } from '@/i18n'

export default function BranchesPage() {
  const t = useT()
  const qc = useQueryClient()
  const { organizationId } = useAuthStore()
  const [page, setPage] = useState(0)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<Branch | null>(null)
  const [form, setForm] = useState({ name: '', address: '', phone: '', headName: '', designCapacity: '' })

  const { data, isLoading } = useQuery({
    queryKey: ['branches', organizationId, page],
    queryFn: () => branchApi.list(organizationId!, page),
    enabled: !!organizationId,
  })

  const saveMutation = useMutation({
    mutationFn: (d: typeof form) => {
      const payload = { ...d, designCapacity: d.designCapacity ? Number(d.designCapacity) : undefined }
      return editing
        ? branchApi.update(editing.id, organizationId!, payload)
        : branchApi.create(organizationId!, payload)
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['branches'] }); closeModal() },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => branchApi.delete(id, organizationId!),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['branches'] }),
  })

  const openCreate = () => {
    setEditing(null)
    setForm({ name: '', address: '', phone: '', headName: '', designCapacity: '' })
    setModalOpen(true)
  }

  const openEdit = (b: Branch) => {
    setEditing(b)
    setForm({
      name: b.name,
      address: b.address ?? '',
      phone: b.phone ?? '',
      headName: b.headName ?? '',
      designCapacity: b.designCapacity?.toString() ?? '',
    })
    setModalOpen(true)
  }

  const closeModal = () => { setModalOpen(false); setEditing(null) }

  const pageData = data?.data.data

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">{t('branches.title')}</h1>
          <p className="text-sm text-gray-500 mt-1">{t('branches.subtitle')}</p>
        </div>
        <Button onClick={openCreate}>
          <Plus className="h-4 w-4" /> {t('branches.add')}
        </Button>
      </div>

      {isLoading ? <Spinner /> : pageData?.content.length === 0 ? <Empty message={t('branches.empty')} /> : (
        <>
          <Table>
            <thead>
              <tr>
                <Th>{t('branches.col.name')}</Th>
                <Th>{t('branches.col.head')}</Th>
                <Th>{t('branches.col.address')}</Th>
                <Th>{t('branches.col.phone')}</Th>
                <Th>{t('branches.col.capacity')}</Th>
                <Th>{t('common.status')}</Th>
                <Th>{''}</Th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {pageData?.content.map((b) => (
                <tr key={b.id} className="hover:bg-gray-50">
                  <Td>
                    <div className="flex items-center gap-2">
                      <Building2 className="h-4 w-4 text-gray-400" />
                      <span className="font-medium">{b.name}</span>
                    </div>
                  </Td>
                  <Td>{b.headName || '—'}</Td>
                  <Td>{b.address || '—'}</Td>
                  <Td>{b.phone || '—'}</Td>
                  <Td>{b.designCapacity ?? '—'}</Td>
                  <Td>
                    <span className={b.active ? 'badge-green' : 'badge-gray'}>
                      {b.active ? t('common.active') : t('common.archive')}
                    </span>
                  </Td>
                  <Td>
                    <div className="flex items-center gap-2">
                      <button onClick={() => openEdit(b)} className="text-gray-400 hover:text-primary-600 transition-colors">
                        <Pencil className="h-4 w-4" />
                      </button>
                      <button
                        onClick={() => { if (confirm(t('branches.delete.confirm'))) deleteMutation.mutate(b.id) }}
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

      <Modal open={modalOpen} onClose={closeModal} title={editing ? t('branches.modal.edit') : t('branches.modal.create')}>
        <form onSubmit={(e) => { e.preventDefault(); saveMutation.mutate(form) }} className="space-y-4">
          <Input label={t('branches.field.name')} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          <Input label={t('branches.field.head')} value={form.headName} onChange={(e) => setForm({ ...form, headName: e.target.value })} />
          <Input label={t('branches.field.address')} value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
          <Input label={t('branches.field.phone')} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          <Input label={t('branches.field.capacity')} type="number" value={form.designCapacity} onChange={(e) => setForm({ ...form, designCapacity: e.target.value })} />
          <div className="flex gap-3 justify-end pt-2">
            <Button type="button" variant="secondary" onClick={closeModal}>{t('common.cancel')}</Button>
            <Button type="submit" loading={saveMutation.isPending}>{t('common.save')}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
