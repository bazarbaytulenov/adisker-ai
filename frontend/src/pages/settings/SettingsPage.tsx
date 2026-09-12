import { useEffect, useState } from 'react'
import { orgApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Card } from '@/components/common'
import { Save } from 'lucide-react'
import { useT } from '@/i18n'

export default function SettingsPage() {
  const t = useT()
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

  if (!org) return <div className="text-gray-400 py-8">{t('common.loading')}</div>

  return (
    <div className="space-y-4 max-w-2xl">
      <h1 className="text-2xl font-bold text-gray-800">{t('settings.title')}</h1>

      <Card>
        <div className="grid grid-cols-2 gap-3">
          <Input label={t('settings.field.name')} value={org.name || ''}
            onChange={(e) => setOrg({ ...org, name: e.target.value })} />
          <Input label={t('settings.field.legalName')} value={org.legalName || ''}
            onChange={(e) => setOrg({ ...org, legalName: e.target.value })} />
          <Input label={t('settings.field.bin')} value={org.bin || ''}
            onChange={(e) => setOrg({ ...org, bin: e.target.value })} />
          <Input label={t('settings.field.phone')} value={org.phone || ''}
            onChange={(e) => setOrg({ ...org, phone: e.target.value })} />
          <Input label={t('settings.field.email')} value={org.email || ''}
            onChange={(e) => setOrg({ ...org, email: e.target.value })} />
          <Input label={t('settings.field.address')} value={org.address || ''}
            onChange={(e) => setOrg({ ...org, address: e.target.value })} />
        </div>
      </Card>

      <Card>
        <h2 className="font-semibold text-gray-700 mb-3">{t('settings.payment.title')}</h2>
        <div className="max-w-xs">
          <Input label={t('settings.payment.rate')} type="number" value={org.dailyRate ?? 0}
            onChange={(e) => setOrg({ ...org, dailyRate: e.target.value })} />
        </div>
        <p className="text-xs text-gray-400 mt-2">{t('settings.payment.hint')}</p>
      </Card>

      <div className="flex items-center gap-3">
        <Button onClick={save} loading={loading}><Save className="h-4 w-4" /> {t('common.save')}</Button>
        {saved && <span className="text-green-600 text-sm">{t('common.saved')}</span>}
      </div>
    </div>
  )
}
