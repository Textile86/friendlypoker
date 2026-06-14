import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { CardView, GameStateView } from '../api/game'

function wsLog(url: string, body: string, isError = false) {
  const now = new Date()
  const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
  window.dispatchEvent(new CustomEvent('fp:log', {
    detail: { time, method: 'WS', url, responseBody: body, isError, duration: 0 },
  }))
}

export interface RawGameEvent {
  type: string
  data: any
}

function suitSymbol(s: string): string {
  switch (s?.toUpperCase()) {
    case 'HEARTS':   return '♥'
    case 'DIAMONDS': return '♦'
    case 'CLUBS':    return '♣'
    case 'SPADES':   return '♠'
    default:         return s ?? ''
  }
}

function rankAbbr(r: string): string {
  const MAP: Record<string, string> = {
    ACE: 'A', KING: 'K', QUEEN: 'Q', JACK: 'J', TEN: 'T',
    NINE: '9', EIGHT: '8', SEVEN: '7', SIX: '6', FIVE: '5',
    FOUR: '4', THREE: '3', TWO: '2',
  }
  return MAP[r?.toUpperCase()] ?? r
}

function cardStr(c: any): string {
  return `${rankAbbr(c.rank)}${suitSymbol(c.suit)}`
}

function formatRank(rank: string): string {
  return rank.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase())
}

function formatEvent(ev: RawGameEvent, name: (id: string) => string): string {
  const d = ev.data ?? {}
  const n = (id: string) => name(id) || id
  switch (ev.type) {
    case 'HandStarted': return `✨ Hand #${d.handNumber} started`
    case 'BlindPosted': return `${n(d.playerId)}: ${d.isSmallBlind ? 'SB' : 'BB'} ${d.amount}`
    case 'PlayerActed': return `${n(d.playerId)}: ${(d.actionType ?? '').toLowerCase()} ${d.amount || ''}`
    case 'PlayerFolded': return `${n(d.playerId)}: fold`
    case 'PhaseChanged': return `→ ${d.newPhase ?? ''}`
    case 'PotAwarded': return `🏆 ${n(d.winnerId)} wins ${d.amount}${d.winningRank ? ` (${formatRank(d.winningRank)})` : ''}`
    case 'HandFinished': return `--- Hand finished ---`
    case 'PlayerLeft': return `🚪 ${n(d.playerId ?? '')} left`
    case 'TableClosed': return `⏹ Table closed`
    case 'CardsShown': {
      const cards = (d.cards ?? []).map((c: { rank: string; suit: string }) => cardStr(c)).join(' ')
      return `👁 ${d.displayName ?? n(d.playerId)} shows: ${cards}`
    }
    default: return ev.type
  }
}

function formatShowdownLines(
  ev: RawGameEvent,
  name: (id: string) => string,
  community: CardView[],
): string[] {
  const d = ev.data ?? {}
  const hands: Record<string, any[]> = d.revealedHands ?? {}
  const ranks: Record<string, string> = d.handRanks ?? {}
  const lines: string[] = ['🃏 Showdown']
  if (community.length > 0) {
    lines.push(`Board: ${community.map(cardStr).join(' ')}`)
  }
  for (const pid of Object.keys(ranks)) {
    const cards = (hands[pid] ?? []).map(cardStr).join(' ')
    lines.push(`${name(pid) || pid}: ${cards} → ${formatRank(ranks[pid])}`)
  }
  return lines
}

export function useGameSocket(
  tableId: number,
  onState: (s: GameStateView) => void,
  onEvent?: (ev: RawGameEvent) => void,
  nameOf?: (id: string) => string,
  communityCardsRef?: { current: CardView[] },
) {
  const [connected, setConnected] = useState(false)
  const [events, setEvents] = useState<string[]>([])
  const onStateRef = useRef(onState)
  const onEventRef = useRef(onEvent)
  const nameOfRef = useRef(nameOf)

  useEffect(() => {
    onStateRef.current = onState
    onEventRef.current = onEvent
    nameOfRef.current = nameOf
  })

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws') as any,
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true)
        wsLog(`/ws/table/${tableId}`, 'connected ✓')
        client.subscribe(`/topic/tables/${tableId}/state`, (msg) => {
          const parsed = JSON.parse(msg.body) as GameStateView
          wsLog(
            `/topic/tables/${tableId}/state`,
            `phase=${parsed.phase} turn=${parsed.currentPlayerIndex} hand=${parsed.handNumber} players=${parsed.players.length}`,
          )
          onStateRef.current(parsed)
        })
        client.subscribe(`/topic/tables/${tableId}/events`, (msg) => {
          const ev = JSON.parse(msg.body) as RawGameEvent
          wsLog(`/topic/tables/${tableId}/events`, `${ev.type} ${JSON.stringify(ev.data ?? {})}`)
          onEventRef.current?.(ev)
          const resolve = nameOfRef.current ?? ((id: string) => id)

          if (ev.type === 'Showdown') {
            const community = communityCardsRef?.current ?? []
            const lines = formatShowdownLines(ev, resolve, community)
            setEvents((prev) => [...lines, ...prev].slice(0, 80))
          } else {
            const text = formatEvent(ev, resolve)
            setEvents((prev) => [text, ...prev].slice(0, 80))
          }
        })
      },
      onDisconnect: () => {
        setConnected(false)
        wsLog(`/ws/table/${tableId}`, 'disconnected', true)
      },
    })
    client.activate()
    return () => { client.deactivate() }
  }, [tableId])

  return { connected, events }
}
