import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { KeyRound, User as UserIcon } from 'lucide-react'
import { userApi } from '@/api'
import { useAuthStore } from '@/store/authStore'
import { Button, Input, Card } from '@/components/common'

export default function ProfilePage() {
  const { fullName, roleCode } = useAuthStore()

  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [repeat, setRepeat] = useState('')
  const [ok, setOk] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () => userApi.changeMyPassword(current, next),
    onSuccess: () => {
      setOk(true); setErr(null)
      setCurrent(''); setNext(''); setRepeat('')
    },
    onError: (e: any) => {
      setOk(false)
      setErr(e?.response?.data?.message ?? 'Не удалось сменить пароль')
    },
  })

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    setOk(false); setErr(null)
    if (next.length < 6) { setErr('Новый пароль должен быть не короче 6 символов'); return }
    if (next !== repeat) { setErr('Пароли не совпадают'); return }
    mutation.mutate()
  }

  return (
    <div className="space-y-4 max-w-lg">
      <h1 className="text-2xl font-bold text-gray-800">Профиль</h1>

      <Card>
        <div className="flex items-center gap-3">
          <div className="h-12 w-12 rounded-full bg-primary-100 flex items-center justify-center">
            <UserIcon className="h-6 w-6 text-primary-600" />
          </div>
          <div>
            <div className="font-medium text-gray-900">{fullName ?? '—'}</div>
            <div className="text-sm text-gray-500">{roleCode}</div>
          </div>
        </div>
      </Card>

      <Card>
        <h2 className="font-semibold text-gray-700 mb-3 flex items-center gap-2">
          <KeyRound className="h-4 w-4 text-primary-600" /> Смена пароля
        </h2>
        <form onSubmit={submit} className="space-y-3">
          <Input
            label="Текущий пароль"
            type="password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
            required
            autoComplete="current-password"
          />
          <Input
            label="Новый пароль"
            type="password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
            required
            minLength={6}
            placeholder="Минимум 6 символов"
            autoComplete="new-password"
          />
          <Input
            label="Повторите новый пароль"
            type="password"
            value={repeat}
            onChange={(e) => setRepeat(e.target.value)}
            required
            autoComplete="new-password"
          />

          {ok && <p className="text-sm text-green-600">Пароль изменён</p>}
          {err && <p className="text-sm text-red-600">{err}</p>}

          <div className="pt-1">
            <Button type="submit" loading={mutation.isPending}>Сменить пароль</Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
