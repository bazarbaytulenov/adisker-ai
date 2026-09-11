import { useEffect, useState } from 'react'
import { annualPlanApi, branchApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Select, Card } from '@/components/common'
import { Plus, ArrowRightLeft } from 'lucide-react'

export default function AnnualPlanPage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [branches, setBranches] = useState<any[]>([])
  const [branchId, setBranchId] = useState('')
  const [year] = useState('2026-2027')
  const [plan, setPlan] = useState<any>(null)
  const [sections, setSections] = useState<any[]>([])
  const [events, setEvents] = useState<Record<string, any[]>>({})
  const [newSection, setNewSection] = useState('')
  const [monthly, setMonthly] = useState<any[]>([])

  useEffect(() => {
    branchApi.listActive(organizationId).then(({ data }) => {
      const list = data.data || []; setBranches(list)
      if (list[0]) setBranchId(list[0].id)
    })
  }, []) // eslint-disable-line

  const loadPlan = async () => {
    if (!branchId) return
    const { data } = await annualPlanApi.getOrCreate(organizationId, branchId, year)
    setPlan(data.data)
    const s = await annualPlanApi.sections(organizationId, data.data.id)
    setSections(s.data.data || [])
    const m = await annualPlanApi.monthly(organizationId, branchId)
    setMonthly(m.data.data || [])
  }
  useEffect(() => { loadPlan() }, [branchId]) // eslint-disable-line

  const addSection = async () => {
    if (!plan || !newSection.trim()) return
    await annualPlanApi.addSection(organizationId, plan.id, { title: newSection, sortOrder: sections.length })
    setNewSection(''); loadPlan()
  }
  const addEvent = async (sectionId: string, title: string, month: number) => {
    await annualPlanApi.addEvent(organizationId, sectionId, { title, month })
    loadPlan()
  }
  const propagate = async (month: number) => {
    if (!plan) return
    const { data } = await annualPlanApi.propagate(organizationId, plan.id, 2026, month)
    alert(data.message || 'Готово')
    loadPlan()
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">Годовой план</h1>
      <Card>
        <div className="grid grid-cols-2 gap-3 items-end">
          <Select label="Филиал" value={branchId} onChange={(e) => setBranchId(e.target.value)}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          <div className="text-sm text-gray-500">Учебный год: {year}</div>
        </div>
      </Card>

      {plan && (
        <>
          <Card>
            <div className="flex gap-2 items-end mb-4">
              <Input label="Новый раздел" value={newSection} onChange={(e) => setNewSection(e.target.value)} className="flex-1" />
              <Button onClick={addSection}><Plus className="h-4 w-4" /> Добавить раздел</Button>
            </div>
            {sections.length === 0 && <div className="text-gray-400 text-sm">Разделов пока нет</div>}
            {sections.map((s) => (
              <SectionBlock key={s.id} section={s} onAddEvent={addEvent} />
            ))}
          </Card>

          <Card>
            <div className="flex items-center justify-between mb-3">
              <h2 className="font-semibold text-gray-700">Передача в месячные планы</h2>
              <div className="flex gap-1 flex-wrap">
                {[9,10,11,12,1,2,3,4,5].map((m) => (
                  <Button key={m} size="sm" variant="secondary" onClick={() => propagate(m)}>
                    <ArrowRightLeft className="h-3 w-3" /> {m}
                  </Button>
                ))}
              </div>
            </div>
            <div className="text-sm text-gray-500">Месячных планов: {monthly.length}</div>
            <ul className="text-sm mt-2">
              {monthly.map((mp) => <li key={mp.id} className="py-1 border-b">{mp.month}/{mp.year} — {mp.status}</li>)}
            </ul>
          </Card>
        </>
      )}
    </div>
  )
}

function SectionBlock({ section, onAddEvent }: { section: any; onAddEvent: (id: string, title: string, month: number) => void }) {
  const [title, setTitle] = useState('')
  const [month, setMonth] = useState(9)
  return (
    <div className="border rounded-lg p-3 mb-2">
      <div className="font-medium mb-2">{section.title}</div>
      <div className="flex gap-2 items-end">
        <Input placeholder="Мероприятие" value={title} onChange={(e) => setTitle(e.target.value)} className="flex-1" />
        <Select value={String(month)} onChange={(e) => setMonth(+e.target.value)}
          options={[9,10,11,12,1,2,3,4,5].map((m) => ({ value: String(m), label: `Мес. ${m}` }))} />
        <Button size="sm" onClick={() => { if (title.trim()) { onAddEvent(section.id, title, month); setTitle('') } }}>
          <Plus className="h-4 w-4" />
        </Button>
      </div>
    </div>
  )
}
