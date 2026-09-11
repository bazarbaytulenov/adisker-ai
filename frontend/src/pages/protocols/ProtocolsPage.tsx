import { useEffect, useState } from 'react'
import { protocolApi, branchApi } from '@/api'
import { downloadFile } from '@/api/download'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Select, Card } from '@/components/common'
import { Plus, FileDown } from 'lucide-react'

const TYPES = [
  { value: 'pedagogical', label: 'Педагогический совет' },
  { value: 'methodical', label: 'Методический совет' },
  { value: 'parents', label: 'Родительское собрание' },
  { value: 'guardian', label: 'Попечительский совет' },
  { value: 'ethics', label: 'Совет по педэтике' },
]

export default function ProtocolsPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [items, setItems] = useState<any[]>([])
  const [branches, setBranches] = useState<any[]>([])
  const [form, setForm] = useState({ branchId: '', protocolType: 'pedagogical', number: '', protocolDate: '', chairman: '', secretary: '', attendees: '', agenda: '' })

  const load = async () => {
    const { data } = await protocolApi.list(organizationId)
    setItems(data.data || [])
  }
  useEffect(() => {
    branchApi.listActive(organizationId).then(({ data }) => {
      const list = data.data || []
      setBranches(list)
      if (list[0]) setForm((f) => ({ ...f, branchId: list[0].id }))
    })
    load()
  }, []) // eslint-disable-line

  const create = async () => {
    if (!form.branchId || !form.number) return
    await protocolApi.create(organizationId, form)
    setForm({ ...form, number: '', agenda: '', chairman: '', secretary: '', attendees: '' })
    load()
  }

  const typeName = (t: string) => TYPES.find((x) => x.value === t)?.label ?? t

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">Протоколы</h1>

      <Card>
        <div className="grid grid-cols-2 gap-3">
          <Select label="Филиал" value={form.branchId}
            onChange={(e) => setForm({ ...form, branchId: e.target.value })}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          <Select label="Тип совета" value={form.protocolType}
            onChange={(e) => setForm({ ...form, protocolType: e.target.value })} options={TYPES} />
          <Input label="Номер" value={form.number}
            onChange={(e) => setForm({ ...form, number: e.target.value })} />
          <Input label="Дата" type="date" value={form.protocolDate}
            onChange={(e) => setForm({ ...form, protocolDate: e.target.value })} />
          <Input label="Председатель" value={form.chairman}
            onChange={(e) => setForm({ ...form, chairman: e.target.value })} />
          <Input label="Секретарь" value={form.secretary}
            onChange={(e) => setForm({ ...form, secretary: e.target.value })} />
          <Input label="Присутствовали" value={form.attendees}
            onChange={(e) => setForm({ ...form, attendees: e.target.value })} className="col-span-2" />
          <Input label="Повестка" value={form.agenda}
            onChange={(e) => setForm({ ...form, agenda: e.target.value })} className="col-span-2" />
        </div>
        <div className="mt-3">
          <Button onClick={create}><Plus className="h-4 w-4" /> Создать протокол</Button>
        </div>
      </Card>

      <Card>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-gray-500">
              <th className="py-2 px-2">Тип</th><th className="py-2 px-2">Номер</th>
              <th className="py-2 px-2">Дата</th><th className="py-2 px-2 w-24"></th>
            </tr>
          </thead>
          <tbody>
            {items.length === 0 &&
              <tr><td colSpan={4} className="py-6 text-center text-gray-400">Нет данных</td></tr>}
            {items.map((p) => (
              <tr key={p.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2">{typeName(p.protocolType)}</td>
                <td className="py-2 px-2">{p.number}</td>
                <td className="py-2 px-2 text-gray-600">{p.protocolDate}</td>
                <td className="py-2 px-2">
                  <button
                    onClick={() => downloadFile(`/protocols/${p.id}/export/word`, `protocol-${p.number || p.id}.docx`, { organizationId })}
                    className="text-primary-600 hover:underline inline-flex items-center gap-1">
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
