import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { GameStateView } from '../api/game'

function formatEvent(ev: { type: string; data: any }): string {
  const d = ev.data ?? {}
  switch (ev.type) {
    case 'HandStarted': return `✨ Hand #${d.handNumber} started`
    case 'BlindPosted': return `${d.playerId}: ${d.isSmallBlind ? 'SB' : 'BB'} ${d.amount}`
    case 'PlayerActed': return `${d.playerId}: ${(d.actionType ?? '').toLowerCase()} ${d.amount || ''}`
    case 'PhaseChanged': return `→ ${d.newPhase ?? ''}`
    case 'PotAwarded': return `🏆 ${d.playerId} wins ${d.amount}`
    case 'HandFinished': return `--- Hand finished ---`
    default: return ev.type
  }
}

export function useGameSocket(tableId: number, onState: (s: GameStateView) => void) {
  const [connected, setConnected] = useState(false)
  const [events, setEvents] = useState<string[]>([])
  const onStateRef = useRef(onState)

  useEffect(() => { onStateRef.current = onState })

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws') as any,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        client.subscribe(`/topic/tables/${tableId}/state`, (msg) => {
          onStateRef.current(JSON.parse(msg.body))
        })
        client.subscribe(`/topic/tables/${tableId}/events`, (msg) => {
          const ev = JSON.parse(msg.body)
          const text = formatEvent(ev)
          setEvents((prev) => [text, ...prev].slice(0, 60))
        })
      },
      onDisconnect: () => setConnected(false),
    })
    client.activate()
    return () => { client.deactivate() }
  }, [tableId])

  return { connected, events }
}
