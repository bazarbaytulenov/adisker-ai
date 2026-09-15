import { useEffect, useState } from 'react'
import { protocolApi, branchApi } from '@/api'
import { downloadFile } from '@/api/download'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Select, Card } from '@/components/common'
import { Plus, FileDown } from 'lucide-react'
import { useT } from '@/i18n'

export default function ProtocolsPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const t = useT()

  const TYPES = [
    { value: 'pedagogical', label: t('protocol.type.pedagogical') },
    { value: 'methodical',  label: t('protocol.type.methodical') },
    { value: 'parents',     label: t('protocol.type.parents') },
    { value: 'guardian',    label: t('protocol.type.guardian') },
    { value: 'ethics',      label: t('protocol.type.ethics') },
  ]

  const [items, setItems] = useState<any[]>([])
  const [branches, setBranches] = useState<any[]>([])
  const [form, setForm] = useState({ branchId: '', protocolType: 'pedagogical', number: '', protocolDate: '', chairman: '', secretary: '', attendees: '', agenda: '' })

  const load = async () => { const { data } = await protocolApi.list(organizationId); setItems(data.data || []) }

  useEffect(() => {
    branchApi.listActive(organizationId).then(({ data }) => setBranches(data.data || []))
    load()
  }, []) // eslint-disable-line

  const create = async () => {
    if (!form.number) return
    await protocolApi.create(organizationId, form)
    setForm({ ...form, number: '', agenda: '', chairman: '', secretary: '', attendees: '' })
    load()
  }

  const typeName = (v: string) => TYPES.find((x) => x.value === v)?.label ?? v

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">{t('protocol.title')}</h1>
      <Card>
        <div className="grid grid-cols-2 gap-3">
          {branches.length > 0 && (
            <Select label={t('protocol.branch')} value={form.branchId}
              onChange={(e) => setForm({ ...form, branchId: e.target.value })}
              options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          )}
          <Select label={t('protocol.type')} value={form.protocolType}
            onChange={(e) => setForm({ ...form, protocolType: e.target.value })} options={TYPES} />
          <Input label={t('protocol.number')} value={form.number} onChange={(e) => setForm({ ...form, number: e.target.value })} />
          <Input label={t('protocol.date')} type="date" value={form.protocolDate} onChange={(e) => setForm({ ...form, protocolDate: e.target.value })} />
          <Input label={t('protocol.chairman')} value={form.chairman} onChange={(e) => setForm({ ...form, chairman: e.target.value })} />
          <Input label={t('protocol.secretary')} value={form.secretary} onChange={(e) => setForm({ ...form, secretary: e.target.value })} />
          <Input label={t('protocol.attendees')} value={form.attendees} onChange={(e) => setForm({ ...form, attendees: e.target.value })} className="col-span-2" />
          <Input label={t('protocol.agenda')} value={form.agenda} onChange={(e) => setForm({ ...form, agenda: e.target.value })} className="col-span-2" />
        </div>
        <div className="mt-3">
          <Button onClick={create}><Plus className="h-4 w-4" /> {t('protocol.create')}</Button>
        </div>
      </Card>
      <Card>
        <table className="w-full text-sm">
          <thead><tr className="border-b text-left text-gray-500">
            <th className="py-2 px-2">{t('protocol.col.type')}</th>
            <th className="py-2 px-2">{t('protocol.col.number')}</th>
            <th className="py-2 px-2">{t('protocol.col.date')}</th>
            <th className="py-2 px-2 w-24"></th>
          </tr></thead>
          <tbody>
            {items.length === 0 && <tr><td colSpan={4} className="py-6 text-center text-gray-400">{t('protocol.empty')}</td></tr>}
            {items.map((p) => (
              <tr key={p.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2">{typeName(p.protocolType)}</td>
                <td className="py-2 px-2">{p.number}</td>
                <td className="py-2 px-2 text-gray-600">{p.protocolDate}</td>
                <td className="py-2 px-2">
                  <button onClick={() => downloadFile(`/protocols/${p.id}/export/word`, `protocol-${p.number || p.id}.docx`, { organizationId })}
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
