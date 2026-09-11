import { useEffect, useState } from 'react'
import { orgApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Card } from '@/components/common'
import { Save } from 'lucide-react'

export default function SettingsPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [org, setOrg] = useState<any>(null)
  const [saved, setSaved] = useState(false)
  const [loading, setLoading] = useState(false)

  const load = async () => {
    const { data } = await orgApi.get(organizationId)
    setOrg(data.data)
  }
  useEffect(() => { load() }, []) // eslint-disable-line

  const save = async () => {
    if (!org) return
    setLoading(true); setSaved(false)
    try {
      await orgApi.update(organizationId, {
        name: org.name,
        legalName: org.legalName,
        bin: org.bin,
        address: org.address,
        phone: org.phone,
        email: org.email,
        dailyRate: Number(org.dailyRate) || 0,
      })
      setSaved(true)
      load()
    } finally { setLoading(false) }
  }

  if (!org) return <div className="text-gray-400 py-8">Загрузка...</div>

  return (
    <div className="space-y-4 max-w-2xl">
      <h1 className="text-2xl font-bold text-gray-800">Настройки организации</h1>

      <Card>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Название" value={org.name || ''}
            onChange={(e) => setOrg({ ...org, name: e.target.value })} />
          <Input label="Юр. название" value={org.legalName || ''}
            onChange={(e) => setOrg({ ...org, legalName: e.target.value })} />
          <Input label="БИН" value={org.bin || ''}
            onChange={(e) => setOrg({ ...org, bin: e.target.value })} />
          <Input label="Телефон" value={org.phone || ''}
            onChange={(e) => setOrg({ ...org, phone: e.target.value })} />
          <Input label="Email" value={org.email || ''}
            onChange={(e) => setOrg({ ...org, email: e.target.value })} />
          <Input label="Адрес" value={org.address || ''}
            onChange={(e) => setOrg({ ...org, address: e.target.value })} />
        </div>
      </Card>

      <Card>
        <h2 className="font-semibold text-gray-700 mb-3">Оплата</h2>
        <div className="max-w-xs">
          <Input label="Тариф (₸ за день посещения)" type="number" value={org.dailyRate ?? 0}
            onChange={(e) => setOrg({ ...org, dailyRate: e.target.value })} />
        </div>
        <p className="text-xs text-gray-400 mt-2">
          Используется при автоматическом формировании начислений из табеля:
          посещённые дни × тариф.
        </p>
      </Card>

      <div className="flex items-center gap-3">
        <Button onClick={save} loading={loading}><Save className="h-4 w-4" /> Сохранить</Button>
        {saved && <span className="text-green-600 text-sm">Сохранено</span>}
      </div>
    </div>
  )
}
