import { useEffect, useState } from 'react'
import { assetApi, branchApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Select, Card } from '@/components/common'
import { Plus, Trash2 } from 'lucide-react'

export default function AssetsPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [items, setItems] = useState<any[]>([])
  const [branches, setBranches] = useState<any[]>([])
  const [branchId, setBranchId] = useState('')
  const [form, setForm] = useState<any>({ name: '', room: '', normQty: 0, actualQty: 0 })

  const load = async () => {
    const { data } = await assetApi.list(organizationId, branchId || undefined, 0, 100)
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
    if (!branchId || !form.name) return
    await assetApi.create({ ...form, branchId })
    setForm({ name: '', room: '', normQty: 0, actualQty: 0 })
    load()
  }
  const remove = async (id: string) => { if (confirm('Удалить?')) { await assetApi.delete(id); load() } }
  const pct = (a: any) => a.normQty ? Math.round((a.actualQty / a.normQty) * 100) : 100

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">Материально-техническая база</h1>
      <Card>
        <div className="grid grid-cols-4 gap-3 items-end">
          <Select label="Филиал" value={branchId} onChange={(e) => setBranchId(e.target.value)}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} className="col-span-2" />
          <Input label="Наименование" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} className="col-span-2" />
          <Input label="Помещение" value={form.room} onChange={(e) => setForm({ ...form, room: e.target.value })} />
          <Input label="Норма" type="number" value={form.normQty} onChange={(e) => setForm({ ...form, normQty: +e.target.value })} />
          <Input label="Факт" type="number" value={form.actualQty} onChange={(e) => setForm({ ...form, actualQty: +e.target.value })} />
          <Button onClick={create}><Plus className="h-4 w-4" /> Добавить</Button>
        </div>
      </Card>
      <Card>
        <table className="w-full text-sm">
          <thead><tr className="border-b text-left text-gray-500">
            <th className="py-2 px-2">Наименование</th><th className="py-2 px-2">Помещение</th>
            <th className="py-2 px-2">Норма</th><th className="py-2 px-2">Факт</th>
            <th className="py-2 px-2">Обеспеченность</th><th className="py-2 px-2 w-10"></th>
          </tr></thead>
          <tbody>
            {items.length === 0 && <tr><td colSpan={6} className="py-6 text-center text-gray-400">Нет данных</td></tr>}
            {items.map((a) => (
              <tr key={a.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2">{a.name}</td>
                <td className="py-2 px-2 text-gray-600">{a.room}</td>
                <td className="py-2 px-2">{a.normQty}</td>
                <td className="py-2 px-2">{a.actualQty}</td>
                <td className="py-2 px-2">
                  <span className={pct(a) >= 100 ? 'text-green-600' : pct(a) >= 50 ? 'text-amber-600' : 'text-red-600'}>{pct(a)}%</span>
                </td>
                <td className="py-2 px-2">
                  <button onClick={() => remove(a.id)} className="text-gray-400 hover:text-red-600"><Trash2 className="h-4 w-4" /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  )
}
