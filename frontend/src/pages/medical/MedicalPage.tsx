import { useEffect, useState } from 'react'
import { medicalApi, branchApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Select, Card } from '@/components/common'
import { Plus, Trash2 } from 'lucide-react'
import { useT } from '@/i18n'

export default function MedicalPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const t = useT()

  const TYPES = [
    { value: 'vaccination',  label: t('medical.type.vaccination') },
    { value: 'growth',       label: t('medical.type.growth') },
    { value: 'bracket',      label: t('medical.type.bracket') },
    { value: 'daily_sample', label: t('medical.type.daily_sample') },
    { value: 'fridge',       label: t('medical.type.fridge') },
  ]

  const [items, setItems] = useState<any[]>([])
  const [branches, setBranches] = useState<any[]>([])
  const [branchId, setBranchId] = useState('')
  const [form, setForm] = useState<any>({ journalType: 'vaccination', title: '', journalDate: '' })

  const load = async () => {
    if (!branchId) return
    const { data } = await medicalApi.list(organizationId, branchId, 0, 100)
    setItems(data.data?.content || [])
  }
  useEffect(() => { branchApi.listActive(organizationId).then(({ data }) => setBranches(data.data || [])) }, []) // eslint-disable-line
  useEffect(() => { load() }, [branchId]) // eslint-disable-line

  const create = async () => {
    if (!form.title) return
    await medicalApi.create({ ...form, branchId })
    setForm({ journalType: 'vaccination', title: '', journalDate: '' }); load()
  }
  const remove = async (id: string) => { if (confirm(t('medical.deleteConfirm'))) { await medicalApi.delete(id); load() } }
  const typeName = (v: string) => TYPES.find((x) => x.value === v)?.label ?? v

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">{t('medical.title')}</h1>
      <Card>
        <div className="grid grid-cols-3 gap-3 items-end">
          {branches.length > 0 && (
            <Select label={t('medical.branch')} value={branchId} onChange={(e) => setBranchId(e.target.value)}
              options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          )}
          <Select label={t('medical.type')} value={form.journalType}
            onChange={(e) => setForm({ ...form, journalType: e.target.value })} options={TYPES} />
          <Input label={t('medical.date')} type="date" value={form.journalDate}
            onChange={(e) => setForm({ ...form, journalDate: e.target.value })} />
          <Input label={t('medical.heading')} value={form.title}
            onChange={(e) => setForm({ ...form, title: e.target.value })} className="col-span-2" />
          <Button onClick={create}><Plus className="h-4 w-4" /> {t('medical.add')}</Button>
        </div>
      </Card>
      <Card>
        <table className="w-full text-sm">
          <thead><tr className="border-b text-left text-gray-500">
            <th className="py-2 px-2">{t('medical.col.type')}</th>
            <th className="py-2 px-2">{t('medical.col.heading')}</th>
            <th className="py-2 px-2">{t('medical.col.date')}</th>
            <th className="py-2 px-2 w-10"></th>
          </tr></thead>
          <tbody>
            {items.length === 0 && <tr><td colSpan={4} className="py-6 text-center text-gray-400">{t('medical.empty')}</td></tr>}
            {items.map((m) => (
              <tr key={m.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2">{typeName(m.journalType)}</td>
                <td className="py-2 px-2">{m.title}</td>
                <td className="py-2 px-2 text-gray-600">{m.journalDate}</td>
                <td className="py-2 px-2">
                  <button onClick={() => remove(m.id)} className="text-gray-400 hover:text-red-600"><Trash2 className="h-4 w-4" /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  )
}
