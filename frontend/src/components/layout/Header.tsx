import { LogOut, User } from 'lucide-react'
import { useAuthStore } from '@/store/authStore'
import { useNavigate } from 'react-router-dom'
import { authApi } from '@/api'
import { useI18n, useT } from '@/i18n'

export default function Header() {
  const { fullName, roleCode, logout } = useAuthStore()
  const { lang, setLang } = useI18n()
  const t = useT()
  const navigate = useNavigate()

  const handleLogout = async () => {
    try { await authApi.logout() } catch { /* ignore */ }
    logout()
    navigate('/login')
  }

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 shrink-0">
      <div />
      <div className="flex items-center gap-4">
        {/* Переключатель языка */}
        <div className="flex items-center rounded-lg border border-gray-200 overflow-hidden text-xs">
          <button
            onClick={() => setLang('ru')}
            className={lang === 'ru' ? 'px-2 py-1 bg-primary-600 text-white' : 'px-2 py-1 text-gray-600 hover:bg-gray-100'}
          >РУ</button>
          <button
            onClick={() => setLang('kk')}
            className={lang === 'kk' ? 'px-2 py-1 bg-primary-600 text-white' : 'px-2 py-1 text-gray-600 hover:bg-gray-100'}
          >ҚАЗ</button>
        </div>
        <button
          onClick={() => navigate('/profile')}
          className="flex items-center gap-2 text-sm text-gray-600 hover:text-primary-700 transition-colors"
          title="Профиль и смена пароля"
        >
          <User className="h-4 w-4" />
          <span className="font-medium">{fullName}</span>
          <span className="badge-gray">{roleCode}</span>
        </button>
        <button
          onClick={handleLogout}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-red-600 transition-colors"
        >
          <LogOut className="h-4 w-4" />
          {t('common.logout')}
        </button>
      </div>
    </header>
  )
}
