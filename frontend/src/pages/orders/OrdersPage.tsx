import { useEffect, useState } from 'react'
import { managerApi, branchApi } from '@/api'
import { downloadFile } from '@/api/download'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Select, Card } from '@/components/common'
import { Plus, PenLine, FileDown } from 'lucide-react'

const TYPES = [
  { value: 'child_admission', label: 'Приём ребёнка' },
  { value: 'child_transfer', label: 'Перевод ребёнка' },
  { value: 'child_discharge', label: 'Выбытие ребёнка' },
  { value: 'child_graduation', label: 'Выпуск ребёнка' },
  { value: 'staff_hire', label: 'Приём сотрудника' },
  { value: 'staff_transfer', label: 'Перевод сотрудника' },
  { value: 'staff_dismiss', label: 'Увольнение сотрудника' },
]

export default function OrdersPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [items, setItems] = useState<any[]>([])
  const [branches, setBranches] = useState<any[]>([])
  const [form, setForm] = useState({ branchId: '', orderType: 'child_admission', orderNumber: '', subject: '', content: '' })

  const load = async () => { const { data } = await managerApi.list(organizationId); setItems(data.data || []) }
  useEffect(() => {
    branchApi.listActive(organizationId).then(({ data }) => {
      const list = data.data || []; setBranches(list)
      if (list[0]) setForm((f) => ({ ...f, branchId: list[0].id }))
    })
    load()
  }, []) // eslint-disable-line

  const create = async () => {
    if (!form.branchId || !form.subject) return
    await managerApi.create(organizationId, form)
    setForm({ ...form, orderNumber: '', subject: '', content: '' })
    load()
  }
  const sign = async (id: string) => { await managerApi.sign(organizationId, id); load() }
  const typeName = (t: string) => TYPES.find((x) => x.value === t)?.label ?? t

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">Приказы</h1>
      <Card>
        <div className="grid grid-cols-2 gap-3">
          <Select label="Филиал" value={form.branchId} onChange={(e) => setForm({ ...form, branchId: e.target.value })}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          <Select label="Тип приказа" value={form.orderType} onChange={(e) => setForm({ ...form, orderType: e.target.value })} options={TYPES} />
          <Input label="Номер" value={form.orderNumber} onChange={(e) => setForm({ ...form, orderNumber: e.target.value })} />
          <Input label="Тема" value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} />
          <Input label="Содержание" value={form.content} onChange={(e) => setForm({ ...form, content: e.target.value })} className="col-span-2" />
        </div>
        <div className="mt-3"><Button onClick={create}><Plus className="h-4 w-4" /> Создать приказ</Button></div>
      </Card>
      <Card>
        <table className="w-full text-sm">
          <thead><tr className="border-b text-left text-gray-500">
            <th className="py-2 px-2">Тип</th><th className="py-2 px-2">Номер</th>
            <th className="py-2 px-2">Тема</th><th className="py-2 px-2">Статус</th><th className="py-2 px-2 w-28"></th>
          </tr></thead>
          <tbody>
            {items.length === 0 && <tr><td colSpan={5} className="py-6 text-center text-gray-400">Нет приказов</td></tr>}
            {items.map((o) => (
              <tr key={o.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2">{typeName(o.orderType)}</td>
                <td className="py-2 px-2">{o.orderNumber}</td>
                <td className="py-2 px-2">{o.subject}</td>
                <td className="py-2 px-2">{o.status === 'signed' ? '✅ подписан' : 'черновик'}</td>
                <td className="py-2 px-2">
                  {o.status !== 'signed' &&
                    <Button size="sm" variant="secondary" onClick={() => sign(o.id)}><PenLine className="h-4 w-4" /> Подписать</Button>}
                  <button
                    onClick={() => downloadFile(`/manager-orders/${o.id}/export/word`, `order-${o.orderNumber || o.id}.docx`, { organizationId })}
                    className="ml-2 text-primary-600 hover:underline inline-flex items-center gap-1">
                    <FileDown className="h-4 w-4" /> Word
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
