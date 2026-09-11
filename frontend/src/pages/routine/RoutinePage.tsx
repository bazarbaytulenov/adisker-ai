import { useEffect, useState } from 'react'
import { routineApi, branchApi, groupApi } from '@/api'
import { downloadFile } from '@/api/download'
import { useAuthStore } from '@/store/authStore'
import { Button, Select, Card } from '@/components/common'
import { Wand2, Send, Plus, Trash2, FileDown } from 'lucide-react'

interface RoutineItem { time: string; activity: string }

export default function RoutinePage() {
  const organizationId = useAuthStore((s) => s.organizationId)!
  const [branches, setBranches] = useState<any[]>([])
  const [groups, setGroups] = useState<any[]>([])
  const [branchId, setBranchId] = useState('')
  const [groupId, setGroupId] = useState('')
  const [year] = useState('2026-2027')
  const [language, setLanguage] = useState('ru')
  const [routine, setRoutine] = useState<any>(null)
  const [items, setItems] = useState<RoutineItem[]>([])

  useEffect(() => {
    branchApi.listActive(organizationId).then(({ data }) => {
      const list = data.data || []
      setBranches(list)
      if (list[0]) setBranchId(list[0].id)
    })
  }, []) // eslint-disable-line

  useEffect(() => {
    if (!branchId) return
    groupApi.byBranch(branchId).then(({ data }: any) => {
      const list = data.data || []
      setGroups(list)
      if (list[0]) setGroupId(list[0].id)
    })
  }, [branchId]) // eslint-disable-line

  const load = async () => {
    if (!branchId || !groupId) return
    const { data } = await routineApi.getOrCreate(organizationId, branchId, groupId, year, language)
    setRoutine(data.data)
    try { setItems(JSON.parse(data.data.items || '[]')) } catch { setItems([]) }
  }
  useEffect(() => { load() }, [groupId, language]) // eslint-disable-line

  const save = async () => {
    if (!routine) return
    await routineApi.save(organizationId, routine.id, { items: JSON.stringify(items) })
    load()
  }
  const applyTemplate = async () => {
    if (!routine) return
    await routineApi.applyTemplate(organizationId, routine.id)
    load()
  }
  const publish = async () => {
    if (!routine) return
    await routineApi.publish(organizationId, routine.id)
    load()
  }

  const setItem = (i: number, patch: Partial<RoutineItem>) =>
    setItems(items.map((it, idx) => idx === i ? { ...it, ...patch } : it))
  const addItem = () => setItems([...items, { time: '', activity: '' }])
  const delItem = (i: number) => setItems(items.filter((_, idx) => idx !== i))

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800">Режим дня</h1>

      <Card>
        <div className="grid grid-cols-3 gap-3">
          <Select label="Филиал" value={branchId} onChange={(e) => setBranchId(e.target.value)}
            options={branches.map((b) => ({ value: b.id, label: b.name }))} />
          <Select label="Группа" value={groupId} onChange={(e) => setGroupId(e.target.value)}
            options={groups.map((g) => ({ value: g.id, label: g.name }))} />
          <Select label="Язык" value={language} onChange={(e) => setLanguage(e.target.value)}
            options={[{ value: 'ru', label: 'Русский' }, { value: 'kk', label: 'Қазақша' }]} />
        </div>
      </Card>

      {routine && (
        <Card>
          <div className="flex gap-2 mb-3">
            <Button variant="secondary" onClick={applyTemplate}><Wand2 className="h-4 w-4" /> По шаблону</Button>
            <Button variant="secondary" onClick={addItem}><Plus className="h-4 w-4" /> Строка</Button>
            <Button onClick={save} disabled={routine.published}>Сохранить</Button>
            <Button variant="secondary" onClick={publish} disabled={routine.published}>
              <Send className="h-4 w-4" /> {routine.published ? 'Опубликовано' : 'Опубликовать'}
            </Button>
            <Button variant="secondary"
              onClick={() => downloadFile(`/routines/${routine.id}/export/word`, `routine-${routine.id}.docx`, { organizationId })}>
              <FileDown className="h-4 w-4" /> Word
            </Button>
          </div>
          <table className="w-full text-sm">
            <thead><tr className="border-b text-left text-gray-500">
              <th className="py-2 px-2 w-40">Время</th><th className="py-2 px-2">Режимный момент</th><th className="w-10"></th>
            </tr></thead>
            <tbody>
              {items.map((it, i) => (
                <tr key={i} className="border-b">
                  <td className="py-1 px-2">
                    <input className="input py-1" value={it.time} disabled={routine.published}
                      onChange={(e) => setItem(i, { time: e.target.value })} />
                  </td>
                  <td className="py-1 px-2">
                    <input className="input py-1 w-full" value={it.activity} disabled={routine.published}
                      onChange={(e) => setItem(i, { activity: e.target.value })} />
                  </td>
                  <td className="py-1 px-2">
                    {!routine.published &&
                      <button onClick={() => delItem(i)} className="text-gray-400 hover:text-red-600"><Trash2 className="h-4 w-4" /></button>}
                  </td>
                </tr>
              ))}
              {items.length === 0 && <tr><td colSpan={3} className="py-6 text-center text-gray-400">Пусто — заполните по шаблону</td></tr>}
            </tbody>
          </table>
        </Card>
      )}
    </div>
  )
}
