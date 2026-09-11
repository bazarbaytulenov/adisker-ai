import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Lang = 'ru' | 'kk'

// Словарь интерфейса (ТЗ п.8). Дополняется по мере необходимости.
const dict: Record<Lang, Record<string, string>> = {
  ru: {
    'app.name': 'Әдіскер-AI',
    'nav.dashboard': 'Главная',
    'nav.branches': 'Филиалы',
    'nav.groups': 'Группы',
    'nav.children': 'Контингент',
    'nav.attendance': 'Посещаемость',
    'nav.cyclogram': 'Циклограмма',
    'nav.plans': 'Планы',
    'nav.annualPlans': 'Годовой план',
    'nav.schedule': 'Расписание',
    'nav.observation': 'Наблюдение',
    'nav.dailyInfo': 'Информация дня',
    'nav.protocols': 'Протоколы',
    'nav.routine': 'Режим дня',
    'nav.chat': 'Чат',
    'nav.medical': 'Медицина',
    'nav.assets': 'МТБ',
    'nav.library': 'Библиотека',
    'nav.payments': 'Оплата',
    'nav.ai': 'AI-ассистент',
    'nav.orders': 'Приказы',
    'nav.janitor': 'Завхоз',
    'nav.nomenclature': 'Номенклатура',
    'nav.users': 'Пользователи',
    'nav.organizations': 'Организации',
    'nav.settings': 'Настройки',
    'nav.invitations': 'Приглашения',
    'common.logout': 'Выйти',
    'common.create': 'Создать',
    'common.save': 'Сохранить',
    'common.delete': 'Удалить',
    'common.search': 'Поиск',
    'common.confirm': 'Подтвердить',
    'common.loading': 'Загрузка...',
    'common.empty': 'Нет данных',
    'payments.charges': 'Начисления',
    'payments.debt': 'Задолженность',
    'payments.paid': 'Оплачено',
    'payments.confirm': 'Подтвердить платёж',
    'protocols.number': 'Номер',
    'protocols.date': 'Дата',
    'protocols.type': 'Тип',
  },
  kk: {
    'app.name': 'Әдіскер-AI',
    'nav.dashboard': 'Басты бет',
    'nav.branches': 'Филиалдар',
    'nav.groups': 'Топтар',
    'nav.children': 'Контингент',
    'nav.attendance': 'Сабаққа қатысу',
    'nav.cyclogram': 'Циклограмма',
    'nav.plans': 'Жоспарлар',
    'nav.annualPlans': 'Жылдық жоспар',
    'nav.schedule': 'Кесте',
    'nav.observation': 'Бақылау',
    'nav.dailyInfo': 'Күн ақпараты',
    'nav.protocols': 'Хаттамалар',
    'nav.routine': 'Күн тәртібі',
    'nav.chat': 'Чат',
    'nav.medical': 'Медицина',
    'nav.assets': 'МТБ',
    'nav.library': 'Кітапхана',
    'nav.payments': 'Төлем',
    'nav.ai': 'AI-көмекші',
    'nav.orders': 'Бұйрықтар',
    'nav.janitor': 'Шаруашылық',
    'nav.nomenclature': 'Номенклатура',
    'nav.users': 'Пайдаланушылар',
    'nav.organizations': 'Ұйымдар',
    'nav.settings': 'Баптаулар',
    'nav.invitations': 'Шақырулар',
    'common.logout': 'Шығу',
    'common.create': 'Құру',
    'common.save': 'Сақтау',
    'common.delete': 'Жою',
    'common.search': 'Іздеу',
    'common.confirm': 'Растау',
    'common.loading': 'Жүктелуде...',
    'common.empty': 'Деректер жоқ',
    'payments.charges': 'Есептеулер',
    'payments.debt': 'Қарыз',
    'payments.paid': 'Төленген',
    'payments.confirm': 'Төлемді растау',
    'protocols.number': 'Нөмір',
    'protocols.date': 'Күні',
    'protocols.type': 'Түрі',
  },
}

interface I18nState {
  lang: Lang
  setLang: (lang: Lang) => void
}

export const useI18n = create<I18nState>()(
  persist(
    (set) => ({
      lang: 'ru',
      setLang: (lang) => set({ lang }),
    }),
    { name: 'adisker-lang' }
  )
)

/** Хук перевода: const t = useT(); t('nav.payments') */
export function useT() {
  const lang = useI18n((s) => s.lang)
  return (key: string) => dict[lang][key] ?? key
}
