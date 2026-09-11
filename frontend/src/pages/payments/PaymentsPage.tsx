import { useEffect, useState } from 'react'
import { paymentApi, branchApi, groupApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Card, Select, Input } from '@/components/common'
import { Check, X, FileStack } from 'lucide-react'

export default function PaymentsPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [status, setStatus] = useState('pending')
  const [items, setItems] = useState<any[]>([])
  const [loading, setLoading] = useState(false)

  // генерация начислений из табеля
  const [branches, setBranches] = useState<any[]>([])
  const [groups, setGroups] = useState<any[]>([])
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')
  const now = new Date()
  const [year, setYear] = useState(now.getFullYear())
  const [month, setMonth] = useState(now.getMonth() + 1)
  const [genMsg, setGenMsg] = useState('')
  const [genLoading, setGenLoading] = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      const { data } = await paymentApi.registry(organizationId, status)
      setItems(data.data || [])
    } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [status]) // eslint-disable-line

  useEffect(() => {
    branchApi.listActive(organizationId).then(({ data }) => {
      const list = data.data || []; setBranches(list)
      if (list[0]) setBranchId(list[0].id)
    })
  }, []) // eslint-disable-line
  useEffect(() => {
    if (!branchId) return
    groupApi.byBranch(branchId).then(({ data }: any) => {
      const list = data.data || []; setGroups(list)
      if (list[0]) setGroupId(list[0].id)
    })
  }, [branchId]) // eslint-disable-line

  const confirm = async (id: string) => { await paymentApi.confirm(organizationId, id); load() }
  const cancel = async (id: string) => { await paymentApi.cancel(organizationId, id); load() }

  const generate = async () => {
    if (!groupId) return
    setGenLoading(true); setGenMsg('')
    try {
      const { data } = await paymentApi.generate(organizationId, groupId, year, month)
      const d = data.data || {}
      setGenMsg(`Создано начислений: ${d.created}, пропущено (уже были): ${d.skipped}. Ставка: ${d.dailyRate} ₸/день`)
    } catch (e: any) {
      setGenMsg('Ошибка: ' + (e?.response?.data?.message || 'не удалось сформировать'))
    } finally { setGenLoading(false) }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">Оплата</h1>

      {/* Автоформирование начислений из табеля */}
      <Card>
        <h2 className="font-semibold text-gray-700 mb-3 flex items-center gap-2">
          <FileStack className="h-5 w-5 text-primary-600" /> Сформировать начисления за месяц
        </h2>
        <div className="grid grid-cols-4 gap-3 items-end">
          <Select label="Филиал" value={branchId} onChange={(e) => setBranchId(e.target.value)}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          <Select label="Группа" value={groupId} onChange={(e) => setGroupId(e.target.value)}
            options={groups.map((g) => ({ value: g.id, label: g.name }))} />
          <Input label="Год" type="number" value={year} onChange={(e) => setYear(+e.target.value)} />
          <Select label="Месяц" value={String(month)} onChange={(e) => setMonth(+e.target.value)}
            options={Array.from({ length: 12 }, (_, i) => ({ value: String(i + 1), label: String(i + 1) }))} />
        </div>
        <div className="mt-3 flex items-center gap-3">
          <Button onClick={generate} loading={genLoading}>
            <FileStack className="h-4 w-4" /> Сформировать
          </Button>
          {genMsg && <span className="text-sm text-gray-600">{genMsg}</span>}
        </div>
        <p className="text-xs text-gray-400 mt-2">
          Начисления считаются по закрытому табелю: посещённые дни × дневной тариф организации.
          Табель за месяц должен быть закрыт, а тариф — задан в настройках организации.
        </p>
      </Card>

      <div className="w-56">
        <Select label="Статус" value={status} onChange={(e) => setStatus(e.target.value)}
          options={[
            { value: 'pending', label: 'Ожидают подтверждения' },
            { value: 'confirmed', label: 'Подтверждённые' },
            { value: 'cancelled', label: 'Отменённые' },
          ]} />
      </div>

      <Card>
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b text-left text-gray-500">
              <th className="py-2 px-2">Сумма</th><th className="py-2 px-2">Способ</th>
              <th className="py-2 px-2">Статус</th><th className="py-2 px-2 w-40"></th>
            </tr>
          </thead>
          <tbody>
            {loading && <tr><td colSpan={4} className="py-6 text-center text-gray-400">Загрузка...</td></tr>}
            {!loading && items.length === 0 &&
              <tr><td colSpan={4} className="py-6 text-center text-gray-400">Нет платежей</td></tr>}
            {items.map((p) => (
              <tr key={p.id} className="border-b hover:bg-gray-50">
                <td className="py-2 px-2 font-medium">{Number(p.amount).toLocaleString()} ₸</td>
                <td className="py-2 px-2 text-gray-600">{p.paymentMethod || '—'}</td>
                <td className="py-2 px-2">{p.status}</td>
                <td className="py-2 px-2">
                  {p.status === 'pending' && (
                    <div className="flex gap-2">
                      <Button size="sm" onClick={() => confirm(p.id)}><Check className="h-4 w-4" /> Подтвердить</Button>
                      <Button size="sm" variant="danger" onClick={() => cancel(p.id)}><X className="h-4 w-4" /></Button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  )
}
