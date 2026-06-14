import SockJS from 'sockjs-client/dist/sockjs'
import { Client } from '@stomp/stompjs'

let stompClient = null

/**
 * 连接 WebSocket
 * @param {string} url - WebSocket 地址
 * @param {function} onConnect - 连接成功回调
 * @param {function} onError - 错误回调
 */
export function connect(url = '/ws/exam', onConnect, onError) {
  // 如果是相对路径，拼接 baseURL
  let wsUrl = url
  if (url.startsWith('/')) {
    wsUrl = 'http://localhost:8080' + url
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS(wsUrl),
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
    onConnect: () => {
      console.log('WebSocket 连接成功')
      if (onConnect) onConnect()
    },
    onStompError: (frame) => {
      console.error('WebSocket 错误:', frame.headers?.message)
      if (onError) onError(frame)
    },
    onDisconnect: () => {
      console.log('WebSocket 断开连接')
    }
  })

  stompClient.activate()
  return stompClient
}

/**
 * 订阅主题
 * @param {string} topic - 订阅地址
 * @param {function} callback - 消息回调
 */
export function subscribe(topic, callback) {
  if (!stompClient || !stompClient.connected) {
    console.error('WebSocket 未连接')
    return null
  }
  return stompClient.subscribe(topic, (message) => {
    try {
      const data = JSON.parse(message.body)
      callback(data)
    } catch (e) {
      console.error('消息解析失败:', e)
    }
  })
}

/**
 * 发送消息
 * @param {string} destination - 目的地
 * @param {object} data - 消息数据
 */
export function send(destination, data) {
  if (!stompClient || !stompClient.connected) {
    console.error('WebSocket 未连接')
    return
  }
  stompClient.publish({
    destination,
    body: JSON.stringify(data)
  })
}

/**
 * 断开连接
 */
export function disconnect() {
  if (stompClient) {
    stompClient.deactivate()
    stompClient = null
    console.log('WebSocket 已断开')
  }
}

/**
 * 获取连接状态
 */
export function isConnected() {
  return stompClient && stompClient.connected
}
