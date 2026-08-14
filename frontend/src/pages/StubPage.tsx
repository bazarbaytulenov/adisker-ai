import {
  BookMarked, MessageSquare, Stethoscope, Wrench, DollarSign, Layers,
} from 'lucide-react'

const iconMap: Record<string, React.ElementType> = {
  book:        BookMarked,
  message:     MessageSquare,
  stethoscope: Stethoscope,
  wrench:      Wrench,
  dollar:      DollarSign,
  default:     Layers,
}

interface StubPageProps {
  title: string
  description?: string
  icon?: string
}

export default function StubPage({ title, description, icon = 'default' }: StubPageProps) {
  const Icon = iconMap[icon] ?? iconMap.default
  return (
    <div className="flex flex-col items-center justify-center py-24 text-gray-400">
      <Icon className="h-16 w-16 mb-4 text-gray-300" />
      <h2 className="text-xl font-semibold text-gray-600 mb-2">{title}</h2>
      {description && <p className="text-sm text-gray-400">{description}</p>}
      <p className="text-xs text-gray-300 mt-3">Раздел в разработке</p>
    </div>
  )
}
