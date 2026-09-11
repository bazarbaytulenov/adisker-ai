import { useEffect, useState } from 'react'
import { aiApi } from '@/api'
import { Button, Input, Select, Card } from '@/components/common'
import { Sparkles, Copy } from 'lucide-react'

type Mode = 'cyclogram' | 'recommendations' | 'text'

export default function AiPage() {
  const [available, setAvailable] = useState<boolean | null>(null)
  const [mode, setMode] = useState<Mode>('cyclogram')
  const [ageGroup, setAgeGroup] = useState('')
  const [theme, setTheme] = useState('')
  const [childAge, setChildAge] = useState('')
  const [summary, setSummary] = useState('')
  const [prompt, setPrompt] = useState('')
  const [language, setLanguage] = useState('ru')
  const [result, setResult] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    aiApi.status().then(({ data }) => setAvailable(!!data.data?.available)).catch(() => setAvailable(false))
  }, [])

  const generate = async () => {
    setLoading(true); setError(''); setResult('')
    try {
      let resp
      if (mode === 'cyclogram') resp = await aiApi.cyclogram({ ageGroup, theme, language })
      else if (mode === 'recommendations') resp = await aiApi.recommendations({ childAge, summary, language })
      else resp = await aiApi.text(prompt)
      setResult(resp.data.data?.draft || '')
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Ошибка генерации')
    } finally { setLoading(false) }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
        <Sparkles className="h-6 w-6 text-primary-600" /> AI-ассистент
      </h1>

      {available === false && (
        <Card>
          <p className="text-amber-600 text-sm">
            AI-интеграция не настроена на сервере (не задан ключ GEMINI_API_KEY).
            Обратитесь к администратору, чтобы включить генерацию.
          </p>
        </Card>
      )}

      <Card>
        <div className="grid grid-cols-2 gap-3">
          <Select label="Что сгенерировать" value={mode} onChange={(e) => setMode(e.target.value as Mode)}
            options={[
              { value: 'cyclogram', label: 'Циклограмма' },
              { value: 'recommendations', label: 'Рекомендации родителям' },
              { value: 'text', label: 'Произвольный текст' },
            ]} />
          <Select label="Язык" value={language} onChange={(e) => setLanguage(e.target.value)}
            options={[{ value: 'ru', label: 'Русский' }, { value: 'kk', label: 'Қазақша' }]} />

          {mode === 'cyclogram' && <>
            <Input label="Возрастная группа" value={ageGroup} placeholder="например, 4-5 лет"
              onChange={(e) => setAgeGroup(e.target.value)} />
            <Input label="Тема недели" value={theme} onChange={(e) => setTheme(e.target.value)} />
          </>}
          {mode === 'recommendations' && <>
            <Input label="Возраст ребёнка" value={childAge} onChange={(e) => setChildAge(e.target.value)} />
            <Input label="Итоги наблюдения" value={summary} onChange={(e) => setSummary(e.target.value)} />
          </>}
          {mode === 'text' && (
            <div className="col-span-2">
              <label className="label">Запрос</label>
              <textarea className="input w-full h-24" value={prompt} onChange={(e) => setPrompt(e.target.value)} />
            </div>
          )}
        </div>
        <div className="mt-3">
          <Button onClick={generate} loading={loading} disabled={available === false}>
            <Sparkles className="h-4 w-4" /> Сгенерировать
          </Button>
        </div>
        {error && <p className="text-red-600 text-sm mt-2">{error}</p>}
      </Card>

      {result && (
        <Card>
          <div className="flex items-center justify-between mb-2">
            <span className="font-medium text-gray-700">Черновик (проверьте перед использованием)</span>
            <button onClick={() => navigator.clipboard.writeText(result)}
              className="text-primary-600 inline-flex items-center gap-1 text-sm">
              <Copy className="h-4 w-4" /> Копировать
            </button>
          </div>
          <pre className="whitespace-pre-wrap text-sm text-gray-800 bg-gray-50 rounded-lg p-3">{result}</pre>
        </Card>
      )}
    </div>
  )
}
