import { useEffect, useState } from 'react'
import { parentApi, childApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Card } from '@/components/common'
import { QrCode, Link2, MessageCircle, Ban } from 'lucide-react'

export default function InvitationsPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [children, setChildren] = useState<any[]>([])
  const [childId, setChildId] = useState('')
  const [phone, setPhone] = useState('')
  const [invitations, setInvitations] = useState<any[]>([])
  const [lastQr, setLastQr] = useState<any>(null)

  const load = async () => {
    const { data } = await parentApi.listInvitations()
    setInvitations(data.data || [])
  }
  useEffect(() => {
    childApi.list(organizationId, undefined, undefined, 0, 100).then(({ data }: any) => {
      const list = data.data?.content || []
      setChildren(list)
      if (list[0]) setChildId(list[0].id)
    })
    load()
  }, []) // eslint-disable-line

  const create = async () => {
    if (!childId) return
    const { data } = await parentApi.createInvitation({ childId, phone: phone || undefined })
    setLastQr(data.data)
    setPhone('')
    load()
  }

  const revoke = async (id: string) => {
    if (!confirm('Отозвать приглашение?')) return
    await parentApi.revokeInvitation(id)
    load()
  }

  const childName = (id: string) => {
    const c = children.find((x) => x.id === id)
    return c ? `${c.lastName} ${c.firstName}` : id
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">Приглашения родителей</h1>

      <Card>
        <div className="flex gap-3 items-end flex-wrap">
          <Select label="Ребёнок" value={childId} onChange={(e) => setChildId(e.target.value)}
            options={children.map((c) => ({ value: c.id, label: `${c.lastName} ${c.firstName}` }))}
            className="min-w-64" />
          <div className="space-y-1">
            <label className="label">Телефон (WhatsApp)</label>
            <input className="input" placeholder="+7..." value={phone} onChange={(e) => setPhone(e.target.value)} />
          </div>
          <Button onClick={create}>Создать приглашение</Button>
        </div>
      </Card>

      {lastQr && (
        <Card>
          <div className="flex gap-6 items-center">
            {lastQr.qrUrl && <img src={lastQr.qrUrl} alt="QR" className="w-40 h-40 border rounded" />}
            <div className="space-y-2 text-sm">
              <div><b>Код:</b> {lastQr.inviteCode}</div>
              <div className="flex items-center gap-2"><Link2 className="h-4 w-4 text-gray-400" />
                <a href={lastQr.inviteLink} className="text-primary-600 break-all">{lastQr.inviteLink}</a></div>
              {lastQr.whatsappLink &&
                <a href={lastQr.whatsappLink} target="_blank" rel="noreferrer"
                   className="inline-flex items-center gap-1 text-green-600">
                  <MessageCircle className="h-4 w-4" /> Отправить в WhatsApp</a>}
            </div>
          </div>
        </Card>
      )}

      <Card>
        <table className="w-full text-sm">
          <thead><tr className="border-b text-left text-gray-500">
            <th className="py-2 px-2">Ребёнок</th><th className="py-2 px-2">Код</th>
            <th className="py-2 px-2">Статус</th><th className="py-2 px-2 w-20"></th>
          </tr></thead>
          <tbody>
            {invitations.length === 0 &&
              <tr><td colSpan={4} className="py-6 text-center text-gray-400">Нет приглашений</td></tr>}
            {invitations.map((inv) => (
              <tr key={inv.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2">{inv.childFullName || childName(inv.childId)}</td>
                <td className="py-2 px-2 font-mono">{inv.inviteCode}</td>
                <td className="py-2 px-2">
                  <span className={
                    inv.status === 'active' ? 'text-green-600' :
                    inv.status === 'used' ? 'text-gray-500' : 'text-red-500'
                  }>{inv.status}</span>
                </td>
                <td className="py-2 px-2">
                  {inv.status === 'active' &&
                    <button onClick={() => revoke(inv.id)} className="text-gray-400 hover:text-red-600">
                      <Ban className="h-4 w-4" /></button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  )
}
