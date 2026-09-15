import { useEffect, useState } from 'react'
import { aiApi } from '@/api'
import { Button, Input, Select, Card } from '@/components/common'
import { Sparkles, Copy } from 'lucide-react'
import { useT } from '@/i18n'

type Mode = 'cyclogram' | 'recommendations' | 'text'

export default function AiPage() {
  const t = useT()
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
      setError(e?.response?.data?.message || t('common.loading'))
    } finally { setLoading(false) }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
        <Sparkles className="h-6 w-6 text-primary-600" /> {t('ai.title')}
      </h1>

      {available === false && (
        <Card>
          <p className="text-amber-600 text-sm">{t('ai.notConfigured')}</p>
        </Card>
      )}

      <Card>
        <div className="grid grid-cols-2 gap-3">
          <Select label={t('ai.mode')} value={mode} onChange={(e) => setMode(e.target.value as Mode)}
            options={[
              { value: 'cyclogram', label: t('ai.mode.cyclogram') },
              { value: 'recommendations', label: t('ai.mode.recommendations') },
              { value: 'text', label: t('ai.mode.text') },
            ]} />
          <Select label={t('ai.lang')} value={language} onChange={(e) => setLanguage(e.target.value)}
            options={[{ value: 'ru', label: t('lang.ru') }, { value: 'kk', label: t('lang.kk') }]} />

          {mode === 'cyclogram' && <>
            <Input label={t('ai.ageGroup')} value={ageGroup} placeholder={t('ai.ageGroupPlaceholder')}
              onChange={(e) => setAgeGroup(e.target.value)} />
            <Input label={t('ai.theme')} value={theme} onChange={(e) => setTheme(e.target.value)} />
          </>}
          {mode === 'recommendations' && <>
            <Input label={t('ai.childAge')} value={childAge} onChange={(e) => setChildAge(e.target.value)} />
            <Input label={t('ai.summary')} value={summary} onChange={(e) => setSummary(e.target.value)} />
          </>}
          {mode === 'text' && (
            <div className="col-span-2">
              <label className="label">{t('ai.prompt')}</label>
              <textarea className="input w-full h-24" value={prompt} onChange={(e) => setPrompt(e.target.value)} />
            </div>
          )}
        </div>
        <div className="mt-3">
          <Button onClick={generate} loading={loading} disabled={available === false}>
            <Sparkles className="h-4 w-4" /> {t('ai.generate')}
          </Button>
        </div>
        {error && <p className="text-red-600 text-sm mt-2">{error}</p>}
      </Card>

      {result && (
        <Card>
          <div className="flex items-center justify-between mb-2">
            <span className="font-medium text-gray-700">{t('ai.draft')}</span>
            <button onClick={() => navigator.clipboard.writeText(result)}
              className="text-primary-600 inline-flex items-center gap-1 text-sm">
              <Copy className="h-4 w-4" /> {t('ai.copy')}
            </button>
          </div>
          <pre className="whitespace-pre-wrap text-sm text-gray-800 bg-gray-50 rounded-lg p-3">{result}</pre>
        </Card>
      )}
    </div>
  )
}
