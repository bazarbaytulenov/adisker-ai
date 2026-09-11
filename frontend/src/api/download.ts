import api from './client'

/**
 * Скачивание файла через axios с JWT-заголовком (blob).
 * Обычный <a href> не передаёт Authorization, поэтому защищённые
 * endpoint экспорта (Word/PDF/Excel) скачиваем так.
 */
export async function downloadFile(url: string, filename: string, params?: Record<string, any>) {
  const res = await api.get(url, { params, responseType: 'blob' })
  const blob = new Blob([res.data])
  const link = document.createElement('a')
  const objectUrl = URL.createObjectURL(blob)
  link.href = objectUrl
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(objectUrl)
}
