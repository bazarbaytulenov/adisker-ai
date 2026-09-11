import { Client, IMessage } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useAuthStore } from '@/store/authStore'

/**
 * STOMP/SockJS клиент чата (ТЗ 5.17). Real-time приём сообщений
 * по подписке /topic/chat/{threadId}. Аутентификация — JWT в CONNECT-заголовке.
 */
export interface ChatSocketHandlers {
  onMessage: (event: { threadId: string; message: any }) => void
  onConnect?: () => void
}

export function createChatClient(): Client {
  const wsBase = (import.meta.env.VITE_API_URL || '/api').replace(/\/api$/, '')
  const token = useAuthStore.getState().accessToken

  return new Client({
    // SockJS-фабрика (endpoint /ws с withSockJS на сервере)
    webSocketFactory: () => new SockJS(`${wsBase}/api/ws`) as any,
    connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
  })
}

/** Подключение + подписка на тред. Возвращает функцию отключения. */
export function connectToThread(threadId: string, handlers: ChatSocketHandlers): () => void {
  const client = createChatClient()

  client.onConnect = () => {
    handlers.onConnect?.()
    client.subscribe(`/topic/chat/${threadId}`, (msg: IMessage) => {
      try {
        handlers.onMessage(JSON.parse(msg.body))
      } catch { /* ignore malformed */ }
    })
  }

  client.activate()

  return () => { client.deactivate() }
}

/** Отправка сообщения через STOMP (/app/chat.send). */
export function sendViaSocket(client: Client, threadId: string, content: string) {
  if (client.connected) {
    client.publish({ destination: '/app/chat.send', body: JSON.stringify({ threadId, content }) })
    return true
  }
  return false
}
