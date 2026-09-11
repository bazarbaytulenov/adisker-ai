import { useEffect, useRef, useState } from 'react'
import { chatApi } from '@/api'
import { connectToThread } from '@/api/chatSocket'
import { useAuthStore } from '@/store/authStore'
import { Card } from '@/components/common'
import { Send } from 'lucide-react'

export default function ChatPage() {
  const userId = useAuthStore((s) => s.userId)
  const [threads, setThreads] = useState<any[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [messages, setMessages] = useState<any[]>([])
  const [text, setText] = useState('')
  const [live, setLive] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    chatApi.threads().then(({ data }) => {
      const list = data.data || []
      setThreads(list)
      if (list[0]) setActiveId(list[0].id)
    })
  }, [])

  const scrollDown = () =>
    setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50)

  const loadMessages = async (threadId: string) => {
    const { data } = await chatApi.messages(threadId)
    const list = (data.data?.content || []).slice().reverse()
    setMessages(list)
    chatApi.markRead(threadId).catch(() => {})
    scrollDown()
  }
  useEffect(() => { if (activeId) loadMessages(activeId) }, [activeId])

  // Подписка на real-time поток сообщений активного треда
  useEffect(() => {
    if (!activeId) return
    setLive(false)
    const disconnect = connectToThread(activeId, {
      onConnect: () => setLive(true),
      onMessage: (evt) => {
        if (evt.threadId !== activeId) return
        setMessages((prev) =>
          prev.some((m) => m.id === evt.message.id) ? prev : [...prev, evt.message])
        scrollDown()
      },
    })
    return () => { disconnect(); setLive(false) }
  }, [activeId])

  const send = async () => {
    if (!activeId || !text.trim()) return
    await chatApi.send(activeId, text.trim())
    setText('')
    // сообщение придёт обратно по WS-подписке; при отсутствии WS — подстрахуемся
    if (!live) loadMessages(activeId)
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
        Чат с родителями
        <span className={`inline-block w-2 h-2 rounded-full ${live ? 'bg-green-500' : 'bg-gray-300'}`}
              title={live ? 'Real-time подключён' : 'Оффлайн (REST)'} />
      </h1>
      <div className="flex gap-4 h-[70vh]">
        <Card className="w-64 overflow-y-auto shrink-0">
          <div className="font-medium text-gray-500 text-sm mb-2">Диалоги</div>
          {threads.length === 0 && <div className="text-gray-400 text-sm py-4">Нет диалогов</div>}
          {threads.map((t) => (
            <button key={t.id} onClick={() => setActiveId(t.id)}
              className={`w-full text-left px-3 py-2 rounded-lg text-sm ${activeId === t.id ? 'bg-primary-50 text-primary-700' : 'hover:bg-gray-100'}`}>
              {t.childName || t.childFullName || 'Диалог'}
              {t.unreadCount > 0 && <span className="ml-2 badge-gray">{t.unreadCount}</span>}
            </button>
          ))}
        </Card>

        <Card className="flex-1 flex flex-col">
          <div className="flex-1 overflow-y-auto space-y-2 p-2">
            {!activeId && <div className="text-gray-400 text-center py-8">Выберите диалог</div>}
            {messages.map((m) => (
              <div key={m.id} className={`max-w-[70%] rounded-lg px-3 py-2 text-sm ${m.senderId === userId ? 'ml-auto bg-primary-600 text-white' : 'bg-gray-100 text-gray-800'}`}>
                {m.content}
                <div className={`text-[10px] mt-1 ${m.senderId === userId ? 'text-primary-100' : 'text-gray-400'}`}>
                  {m.createdAt ? new Date(m.createdAt).toLocaleString() : ''}
                </div>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>
          {activeId && (
            <div className="flex gap-2 border-t pt-3">
              <input className="input flex-1" value={text} placeholder="Сообщение..."
                onChange={(e) => setText(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && send()} />
              <button onClick={send} className="btn-primary px-4"><Send className="h-4 w-4" /></button>
            </div>
          )}
        </Card>
      </div>
    </div>
  )
}
