import { Link } from 'react-router-dom'
import { FileQuestion } from 'lucide-react'
import { Button } from '@/components/common'

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <FileQuestion className="h-20 w-20 text-gray-300 mx-auto mb-4" />
        <h1 className="text-4xl font-bold text-gray-900 mb-2">404</h1>
        <p className="text-gray-500 mb-6">Страница не найдена</p>
        <Link to="/dashboard">
          <Button>На главную</Button>
        </Link>
      </div>
    </div>
  )
}
