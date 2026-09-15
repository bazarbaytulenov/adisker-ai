import { useEffect, useState } from 'react'
import { nomenclatureApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Card } from '@/components/common'
import { Plus, Trash2, Search } from 'lucide-react'
import { useT } from '@/i18n'

interface Item {
  id: string
  indexCode?: string
  title: string
  retentionPeriod?: string
  notes?: string
  sortOrder: number
}

export default function NomenclaturePage() {
  const t = useT()
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [items, setItems] = useState<Item[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({ indexCode: '', title: '', retentionPeriod: '', sortOrder: 0 })

  const load = async () => {
    setLoading(true)
    try {
      const { data } = await nomenclatureApi.list(organizationId, search || undefined)
      setItems(data.data || [])
    } finally { setLoading(false) }
  }

  useEffect(() => { load() }, []) // eslint-disable-line

  const create = async () => {
    if (!form.title.trim()) return
    await nomenclatureApi.create(organizationId, form)
    setForm({ indexCode: '', title: '', retentionPeriod: '', sortOrder: 0 })
    load()
  }

  const remove = async (id: string) => {
    if (!confirm(t('nomenclature.deleteConfirm'))) return
    await nomenclatureApi.delete(organizationId, id)
    load()
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">{t('nomenclature.title')}</h1>

      <Card>
        <div className="flex gap-2 items-end flex-wrap">
          <Input label={t('nomenclature.index')} value={form.indexCode}
            onChange={(e) => setForm({ ...form, indexCode: e.target.value })} className="w-28" />
          <Input label={t('nomenclature.name')} value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })} className="flex-1 min-w-64" />
          <Input label={t('nomenclature.retention')} value={form.retentionPeriod}
            onChange={(e) => setForm({ ...form, retentionPeriod: e.target.value })} className="w-40" />
          <Button onClick={create}><Plus className="h-4 w-4" /> {t('nomenclature.add')}</Button>
        </div>
      </Card>

      <div className="flex gap-2 items-center">
        <Input placeholder={t('nomenclature.searchPlaceholder')} value={search}
          onChange={(e) => setSearch(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && load()} className="w-72" />
        <Button variant="secondary" onClick={load}><Search className="h-4 w-4" /> {t('nomenclature.search')}</Button>
      </div>

      <Card>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-gray-500">
              <th className="py-2 px-2 w-24">{t('nomenclature.col.index')}</th>
              <th className="py-2 px-2">{t('nomenclature.col.name')}</th>
              <th className="py-2 px-2 w-40">{t('nomenclature.col.retention')}</th>
              <th className="py-2 px-2 w-16"></th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr><td colSpan={4} className="py-6 text-center text-gray-400">{t('common.loading')}</td></tr>
            )}
            {!loading && items.length === 0 && (
              <tr><td colSpan={4} className="py-6 text-center text-gray-400">{t('nomenclature.empty')}</td></tr>
            )}
            {items.map((it) => (
              <tr key={it.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2 text-gray-600">{it.indexCode}</td>
                <td className="py-2 px-2">{it.title}</td>
                <td className="py-2 px-2 text-gray-600">{it.retentionPeriod}</td>
                <td className="py-2 px-2">
                  <button onClick={() => remove(it.id)} className="text-gray-400 hover:text-red-600">
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
