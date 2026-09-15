import { useEffect, useState } from 'react'
import { libraryApi, branchApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Select, Card } from '@/components/common'
import { Plus, Trash2 } from 'lucide-react'
import { useT } from '@/i18n'

export default function LibraryPage() {
  const t = useT()
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [items, setItems] = useState<any[]>([])
  const [branches, setBranches] = useState<any[]>([])
  const [branchId, setBranchId] = useState('')
  const [form, setForm] = useState<any>({ title: '', authors: '', publisher: '', publishYear: '', quantity: 1, language: 'ru' })

  const load = async () => {
    const { data } = await libraryApi.list(organizationId, branchId || undefined, 0, 100)
    setItems(data.data?.content || [])
  }
  useEffect(() => {
    branchApi.listActive(organizationId).then(({ data }) => {
      const list = data.data || []; setBranches(list)
      if (list[0]) setBranchId(list[0].id)
    })
  }, []) // eslint-disable-line
  useEffect(() => { if (branchId) load() }, [branchId]) // eslint-disable-line

  const create = async () => {
    if (!branchId || !form.title) return
    await libraryApi.create({ ...form, branchId, publishYear: form.publishYear ? +form.publishYear : undefined })
    setForm({ title: '', authors: '', publisher: '', publishYear: '', quantity: 1, language: 'ru' })
    load()
  }
  const remove = async (id: string) => {
    if (confirm(t('library.deleteConfirm'))) { await libraryApi.delete(id); load() }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">{t('library.title')}</h1>
      <Card>
        <div className="grid grid-cols-3 gap-3 items-end">
          <Select label={t('library.branch')} value={branchId}
            onChange={(e) => setBranchId(e.target.value)}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          <Input label={t('library.bookTitle')} value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })} className="col-span-2" />
          <Input label={t('library.authors')} value={form.authors}
            onChange={(e) => setForm({ ...form, authors: e.target.value })} />
          <Input label={t('library.publisher')} value={form.publisher}
            onChange={(e) => setForm({ ...form, publisher: e.target.value })} />
          <Input label={t('library.year')} type="number" value={form.publishYear}
            onChange={(e) => setForm({ ...form, publishYear: e.target.value })} />
          <Select label={t('library.lang')} value={form.language}
            onChange={(e) => setForm({ ...form, language: e.target.value })}
            options={[{ value: 'ru', label: t('lang.ru') }, { value: 'kk', label: t('lang.kk') }]} />
          <Input label={t('library.quantity')} type="number" value={form.quantity}
            onChange={(e) => setForm({ ...form, quantity: +e.target.value })} />
          <Button onClick={create}><Plus className="h-4 w-4" /> {t('library.add')}</Button>
        </div>
      </Card>
      <Card>
        <table className="w-full text-sm">
          <thead><tr className="border-b text-left text-gray-500">
            <th className="py-2 px-2">{t('library.col.title')}</th>
            <th className="py-2 px-2">{t('library.col.authors')}</th>
            <th className="py-2 px-2">{t('library.col.year')}</th>
            <th className="py-2 px-2">{t('library.col.lang')}</th>
            <th className="py-2 px-2">{t('library.col.qty')}</th>
            <th className="py-2 px-2 w-10"></th>
          </tr></thead>
          <tbody>
            {items.length === 0 && (
              <tr><td colSpan={6} className="py-6 text-center text-gray-400">{t('library.empty')}</td></tr>
            )}
            {items.map((b) => (
              <tr key={b.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2">{b.title}</td>
                <td className="py-2 px-2 text-gray-600">{b.authors}</td>
                <td className="py-2 px-2">{b.publishYear}</td>
                <td className="py-2 px-2">{b.language}</td>
                <td className="py-2 px-2">{b.quantity}</td>
                <td className="py-2 px-2">
                  <button onClick={() => remove(b.id)} className="text-gray-400 hover:text-red-600">
                    <Trash2 className="h-4 w-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  )
}
