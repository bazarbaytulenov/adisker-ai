import { useEffect, useState } from 'react'
import { janitorApi, branchApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Select, Card } from '@/components/common'
import { Plus, Trash2 } from 'lucide-react'
import { useT } from '@/i18n'

export default function JanitorPage() {
  const t = useT()
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [items, setItems] = useState<any[]>([])
  const [branches, setBranches] = useState<any[]>([])
  const [filter, setFilter] = useState('')
  const [form, setForm] = useState({ branchId: '', recordType: 'safety_briefing', title: '', responsible: '', recordDate: '' })

  const TYPES = [
    { value: 'safety_briefing', label: t('janitor.type.safety_briefing') },
    { value: 'incident',        label: t('janitor.type.incident') },
    { value: 'fire_safety',     label: t('janitor.type.fire_safety') },
    { value: 'inventory',       label: t('janitor.type.inventory') },
  ]

  const load = async () => {
    const { data } = await janitorApi.list(organizationId, form.branchId || undefined, filter || undefined)
    setItems(data.data || [])
  }
  useEffect(() => {
    branchApi.listActive(organizationId).then(({ data }) => {
      const list = data.data || []; setBranches(list)
      if (list[0]) setForm((f) => ({ ...f, branchId: list[0].id }))
    })
  }, []) // eslint-disable-line
  useEffect(() => { if (form.branchId) load() }, [form.branchId, filter]) // eslint-disable-line

  const create = async () => {
    if (!form.branchId || !form.title) return
    await janitorApi.create(organizationId, form)
    setForm({ ...form, title: '', responsible: '', recordDate: '' })
    load()
  }
  const remove = async (id: string) => {
    if (confirm(t('janitor.deleteConfirm'))) { await janitorApi.delete(organizationId, id); load() }
  }
  const typeName = (v: string) => TYPES.find((x) => x.value === v)?.label ?? v

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">{t('janitor.title')}</h1>
      <Card>
        <div className="grid grid-cols-2 gap-3">
          <Select label={t('janitor.branch')} value={form.branchId}
            onChange={(e) => setForm({ ...form, branchId: e.target.value })}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          <Select label={t('janitor.type')} value={form.recordType}
            onChange={(e) => setForm({ ...form, recordType: e.target.value })}
            options={TYPES} />
          <Input label={t('janitor.name')} value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })} />
          <Input label={t('janitor.responsible')} value={form.responsible}
            onChange={(e) => setForm({ ...form, responsible: e.target.value })} />
          <Input label={t('janitor.date')} type="date" value={form.recordDate}
            onChange={(e) => setForm({ ...form, recordDate: e.target.value })} />
        </div>
        <div className="mt-3">
          <Button onClick={create}><Plus className="h-4 w-4" /> {t('janitor.add')}</Button>
        </div>
      </Card>
      <div className="w-64">
        <Select label={t('janitor.filterType')} value={filter}
          onChange={(e) => setFilter(e.target.value)}
          options={[{ value: '', label: t('janitor.all') }, ...TYPES]} />
      </div>
      <Card>
        <table className="w-full text-sm">
          <thead><tr className="border-b text-left text-gray-500">
            <th className="py-2 px-2">{t('janitor.col.type')}</th>
            <th className="py-2 px-2">{t('janitor.col.name')}</th>
            <th className="py-2 px-2">{t('janitor.col.responsible')}</th>
            <th className="py-2 px-2 w-10"></th>
          </tr></thead>
          <tbody>
            {items.length === 0 && (
              <tr><td colSpan={4} className="py-6 text-center text-gray-400">{t('janitor.empty')}</td></tr>
            )}
            {items.map((r) => (
              <tr key={r.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2">{typeName(r.recordType)}</td>
                <td className="py-2 px-2">{r.title}</td>
                <td className="py-2 px-2 text-gray-600">{r.responsible}</td>
                <td className="py-2 px-2">
                  <button onClick={() => remove(r.id)} className="text-gray-400 hover:text-red-600">
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
