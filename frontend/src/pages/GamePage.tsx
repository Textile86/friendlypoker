import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { useAuth } from '../context/AuthContext'
import { useGameSocket } from '../hooks/useGameSocket'
import {
  CardView, PlayerView, GameStateView, AvailableActions,
  getGameState, getAvailableActions, startHand, submitAction,
} from '../api/game'
import { getTable, sitDown, standUp, rebuy, getTableStatistics, closeTable, pauseTable, sitOut as sitOutAPI, imBack as imBackAPI, showCards as showCardsAPI, TableResponse, PlayerStats } from '../api/tables'

// ─── Card helpers ────────────────────────────────────────────────────────────

const SUIT_SYMBOL: Record<string, string> = {
  HEARTS: '♥', DIAMONDS: '♦', CLUBS: '♣', SPADES: '♠',
}
const RANK_DISPLAY: Record<string, string> = {
  TWO: '2', THREE: '3', FOUR: '4', FIVE: '5', SIX: '6',
  SEVEN: '7', EIGHT: '8', NINE: '9', TEN: '10',
  JACK: 'J', QUEEN: 'Q', KING: 'K', ACE: 'A',
}
const isRed = (suit: string) => suit === 'HEARTS' || suit === 'DIAMONDS'

function PlayingCard({ rank, suit }: CardView) {
  return (
    <div
      className={`bg-white rounded-md shadow-md flex flex-col items-center justify-center font-bold select-none ${isRed(suit) ? 'text-red-600' : 'text-gray-900'}`}
      style={{ width: 52, height: 72 }}
    >
      <span style={{ fontSize: (RANK_DISPLAY[rank] ?? rank).length > 1 ? 16 : 22, lineHeight: '1.1' }}>{RANK_DISPLAY[rank] ?? rank}</span>
      <span style={{ fontSize: 26, lineHeight: '1.1' }}>{SUIT_SYMBOL[suit] ?? suit}</span>
    </div>
  )
}

function CardBack() {
  return (
    <div
      className="rounded-md shadow-md border-2 border-blue-500 bg-blue-700 flex items-center justify-center"
      style={{ width: 52, height: 72 }}
    >
      <span className="text-blue-300 text-xl">★</span>
    </div>
  )
}

// ─── Chip helpers ────────────────────────────────────────────────────────────

// Ascending by value — stacks rendered left-to-right
const CHIP_DENOMS = [
  { value: 1,   base: '#92400e', hi: '#f59e0b', border: '#fcd34d', edge: '#78350f' },
  { value: 5,   base: '#b91c1c', hi: '#ef4444', border: '#fca5a5', edge: '#991b1b' },
  { value: 25,  base: '#15803d', hi: '#22c55e', border: '#86efac', edge: '#14532d' },
  { value: 50,  base: '#1d4ed8', hi: '#3b82f6', border: '#93c5fd', edge: '#1e40af' },
  { value: 100, base: '#1f2937', hi: '#4b5563', border: '#6b7280', edge: '#111827' },
]

function decompose(amount: number): { denom: typeof CHIP_DENOMS[number]; count: number }[] {
  const result: { denom: typeof CHIP_DENOMS[number]; count: number }[] = []
  let rem = amount
  for (let i = CHIP_DENOMS.length - 1; i >= 0; i--) {
    const d = CHIP_DENOMS[i]
    if (rem >= d.value) {
      const count = Math.min(Math.floor(rem / d.value), 8)
      result.unshift({ denom: d, count })
      rem -= count * d.value
    }
  }
  return result
}

type ChipStack = { denom: typeof CHIP_DENOMS[number]; count: number }

// Overlapping oval chips — each chip's bottom arc visible below the one above.
// Pass `stacks` for pre-computed accumulation (pot), or `amount` for auto-decompose (bets).
function ChipPile({ amount, stacks: stacksProp, chipW = 26 }: {
  amount?: number; stacks?: ChipStack[]; chipW?: number
}) {
  const stacks = stacksProp ?? (amount && amount > 0 ? decompose(amount) : [])
  if (stacks.length === 0) return null
  const faceH = Math.round(chipW * 0.58)
  const step = Math.max(2, Math.round(chipW * 0.12))
  const showLabel = chipW >= 20
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap: 4, filter: 'drop-shadow(0 2px 4px rgba(0,0,0,.28))' }}>
      {stacks.map(({ denom: d, count }, stackIndex) => {
        const stackH = faceH + (count - 1) * step
        return (
          <div key={d.value} style={{ position: 'relative', width: chipW, height: stackH }}>
            {Array.from({ length: count }).map((_, i) => {
              const isTop = i === count - 1
              const tilt = (stackIndex % 2 === 0 ? -1 : 1) * (i - (count - 1) / 2) * 0.35
              return (
                <div
                  key={i}
                  style={{
                    position: 'absolute',
                    bottom: i * step,
                    left: 0,
                    width: chipW,
                    height: faceH,
                    borderRadius: '50%',
                    transform: `rotate(${tilt}deg) scale(${1 - i * 0.015})`,
                    background: `radial-gradient(circle at 32% 28%, ${d.hi} 0%, ${d.base} 52%, ${d.edge} 100%)`,
                    border: `2px solid ${d.border}`,
                    boxShadow: isTop
                      ? '0 3px 8px rgba(0,0,0,.45), inset 0 1px 0 rgba(255,255,255,.35)'
                      : 'inset 0 1px 0 rgba(255,255,255,.12)',
                    overflow: 'hidden',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}
                >
                  <div
                    style={{
                      position: 'absolute',
                      inset: '14%',
                      borderRadius: '50%',
                      border: '2px solid rgba(255,255,255,.78)',
                      opacity: 0.88,
                    }}
                  />
                  <div
                    style={{
                      position: 'absolute',
                      inset: '34% 20%',
                      borderRadius: '999px',
                      background: 'rgba(255,255,255,.16)',
                    }}
                  />
                  {showLabel && isTop && (
                    <span
                      style={{
                        position: 'relative',
                        zIndex: 1,
                        fontSize: Math.max(9, Math.round(chipW * 0.24)),
                        fontWeight: 900,
                        color: d.text,
                        textShadow: '0 1px 2px rgba(0,0,0,.55)',
                        lineHeight: 1,
                      }}
                    >
                      {d.value}
                    </span>
                  )}
                </div>
              )
            })}
          </div>
        )
      })}
    </div>
  )
}

// ─── Seat positions ───────────────────────────────────────────────────────────

type Pos = { left: string; top: string }

// Seats go CLOCKWISE from seat 0 (bottom/hero). Viewed from above: 6→9→12→3 o'clock.
const POSITIONS: Record<number, Pos[]> = {
  2: [
    { left: '50%', top: '88%' },
    { left: '50%', top: '4%' },
  ],
  4: [
    { left: '50%', top: '88%' }, // 6 o'clock (bottom)
    { left: '10%', top: '44%' }, // 9 o'clock (left)
    { left: '50%', top: '4%' },  // 12 o'clock (top)
    { left: '90%', top: '44%' }, // 3 o'clock (right)
  ],
  6: [
    { left: '50%', top: '88%' }, // 6 o'clock
    { left: '7%',  top: '30%' }, // 9 o'clock (left)
    { left: '33%', top: '2%' },  // 10-11 o'clock (upper-left)
    { left: '67%', top: '2%' },  // 1-2 o'clock (upper-right)
    { left: '93%', top: '30%' }, // 3 o'clock (right)
    { left: '85%', top: '74%' }, // 4-5 o'clock (lower-right)
  ],
  9: [
    { left: '50%', top: '90%' }, // 6 o'clock
    { left: '7%',  top: '55%' }, // 7-8 o'clock (lower-left)
    { left: '7%',  top: '25%' }, // 9-10 o'clock (upper-left)
    { left: '25%', top: '4%' },  // 10-11 o'clock
    { left: '50%', top: '0%' },  // 12 o'clock (top)
    { left: '75%', top: '4%' },  // 1-2 o'clock
    { left: '93%', top: '25%' }, // 2-3 o'clock (upper-right)
    { left: '93%', top: '55%' }, // 4-5 o'clock (lower-right)
    { left: '76%', top: '80%' }, // 5 o'clock
  ],
}

// Returns the "inward" position for bet chips — between seat and table center.
// Bottom players (top > 65%) get a larger factor so bets clear the card area above them.
function getBetChipPos(seatPos: Pos): Pos {
  const px = parseFloat(seatPos.left)
  const py = parseFloat(seatPos.top)
  // Bottom-center seat: shift chips left of hole cards instead of up into board area
  if (py > 75 && Math.abs(px - 50) < 15) {
    return { left: `${(px - 14).toFixed(1)}%`, top: `${(py - 8).toFixed(1)}%` }
  }
  const factor = 0.42
  const bx = px + (50 - px) * factor
  const by = py + (50 - py) * factor
  return { left: `${bx.toFixed(1)}%`, top: `${by.toFixed(1)}%` }
}

function getSeatPos(seatIndex: number, maxPlayers: number): Pos {
  const positions = POSITIONS[maxPlayers] ?? POSITIONS[6]
  return positions[seatIndex % positions.length]
}

// ─── Player seat ─────────────────────────────────────────────────────────────

interface PlayerSeatProps {
  player: PlayerView
  isMe: boolean
  isDealer: boolean
  isActive: boolean
  maxPlayers: number
  receivingPot: boolean
  phase: string
}

function PlayerSeat({ player, isMe, isDealer, isActive, maxPlayers, receivingPot, phase }: PlayerSeatProps) {
  const pos = getSeatPos(player.seatIndex, maxPlayers)
  // In FINISHED phase show everyone at full opacity (showdown reveal)
  const folded = (player.status === 'FOLDED' || player.status === 'SITTING_OUT') && phase !== 'FINISHED'
  // Show hole cards if server sent them; in FINISHED only show cards for players with chips (or if server revealed)
  const showCards = player.holeCards.length > 0 && (phase !== 'FINISHED' || player.chips > 0 || player.holeCards.length > 0)
  const showCardBacks = !showCards && phase !== 'FINISHED' && ((player.status === 'ACTIVE' || player.status === 'ALL_IN') || isMe)

  return (
    <div
      className="absolute"
      style={{ left: pos.left, top: pos.top, transform: 'translate(-50%, -50%)', zIndex: 10 }}
    >
      <div className={`flex flex-col items-center gap-1 transition-opacity ${folded ? 'opacity-40' : ''}`}>
        {/* Hole cards */}
        <div className="flex gap-0.5">
          {showCards
            ? player.holeCards.map((c, i) => <PlayingCard key={i} {...c} />)
            : showCardBacks
              ? <><CardBack /><CardBack /></>
              : null}
        </div>

        {/* Info box */}
        <div className={`relative px-2 py-1 rounded-lg text-center text-xs shadow-lg border min-w-[76px] ${
          isActive
            ? 'bg-yellow-600 border-yellow-400 text-white animate-pulse'
            : isMe
              ? 'bg-blue-700 border-blue-400 text-white'
              : 'bg-gray-800 border-gray-600 text-gray-200'
        } ${receivingPot ? 'ring-2 ring-yellow-400 ring-offset-1 ring-offset-black' : ''}`}>
          <div className="font-semibold truncate max-w-[76px]">{player.displayName}</div>
          <div className="opacity-80">{player.chips} chips</div>
          {player.status === 'ALL_IN' && (
            <div className="text-purple-300 text-[10px] font-bold">ALL-IN</div>
          )}

          {/* Dealer button */}
          {isDealer && (
            <div className="absolute -top-2 -right-2 w-5 h-5 bg-white text-gray-900 rounded-full text-xs font-bold flex items-center justify-center shadow border border-gray-300">
              D
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

// ─── Main page ────────────────────────────────────────────────────────────────

const BETTING_PHASES = new Set(['PRE_FLOP', 'FLOP', 'TURN', 'RIVER'])

export default function GamePage() {
  const { tableId: raw } = useParams<{ tableId: string }>()
  const tableId = Number(raw)
  const { username } = useAuth()
  const navigate = useNavigate()

  const [table, setTable] = useState<TableResponse | null>(null)
  const [gameState, setGameState] = useState<GameStateView | null>(null)
  const [actions, setActions] = useState<AvailableActions | null>(null)
  const [raiseAmount, setRaiseAmount] = useState(0)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [loading, setLoading] = useState(true)
  const [animCards, setAnimCards] = useState<(CardView | null)[]>([null, null, null, null, null])
  const animCardsTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const [sittingOut, setSittingOut] = useState(false)
  const consecutiveTimeoutsRef = useRef(0)
  const [showCardsLeft, setShowCardsLeft] = useState(0)
  const showCardsTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const [closedByOwner, setClosedByOwner] = useState(false)
  const [lastWinner, setLastWinner] = useState<{ name: string; amount: number; rank: string | null } | null>(null)
  const [sweepingToPot, setSweepingToPot] = useState(false)
  const [potToWinner, setPotToWinner] = useState<number | null>(null) // seatIndex of winner
  const [potBreakdown, setPotBreakdown] = useState<ChipStack[]>([])
  const prevPotRef = useRef(0)
  const potUpdateDelayRef = useRef(0) // timestamp after which pot chips visually update
  const [awardAnim, setAwardAnim] = useState<{ seatIndex: number; amount: number; label: string; isSidePot: boolean; stage: 'hold' | 'fly' | 'fade' } | null>(null)
  const awardAnimQueueRef = useRef<Array<{ seatIndex: number; amount: number; label: string; isSidePot: boolean }>>([])
  const awardAnimTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const [pendingBetAnim, setPendingBetAnim] = useState<{ seatIndex: number; amount: number; stage: 'hold' | 'sweep' } | null>(null)
  const pendingBetAnimRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Buy-in / rebuy / stats modals
  const [buyInModal, setBuyInModal] = useState<number | null>(null)  // seatIndex
  const [buyInAmount, setBuyInAmount] = useState(0)
  const [rebuyModal, setRebuyModal] = useState(false)
  const [rebuyAmount, setRebuyAmount] = useState(0)
  const [pauseModal, setPauseModal] = useState(false)
  const [pauseMinutes, setPauseMinutes] = useState(1)
  const [pauseRemaining, setPauseRemaining] = useState(0)
  const [statsModal, setStatsModal] = useState(false)
  const [stats, setStats] = useState<PlayerStats[] | null>(null)

  const gameStateRef = useRef<GameStateView | null>(null)
  const communityCardsRef = useRef<CardView[]>([])
  const eventsScrollRef = useRef<HTMLDivElement | null>(null)
  const pauseTimerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // My cards (from personalized REST response), keyed by hand number
  const myCardsRef = useRef<{ hand: number; seat: number; cards: CardView[] } | null>(null)
  const fetchedHandRef = useRef(-1)
  const viewerCardsRetryRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastHandStartedRef = useRef(-1)
  const lastHandRef = useRef(-1)
  const prevPhaseRef = useRef('')
  const prevPlayerIdxRef = useRef(-1)

  function clearViewerCardsRetry() {
    if (viewerCardsRetryRef.current) {
      clearTimeout(viewerCardsRetryRef.current)
      viewerCardsRetryRef.current = null
    }
  }

  function stashViewerCards(state: GameStateView | null | undefined) {
    if (!state) return
    const me = state.players.find((p) => p.holeCards.length > 0)
    if (!me) return
    myCardsRef.current = { hand: state.handNumber, seat: me.seatIndex, cards: me.holeCards }
    fetchedHandRef.current = state.handNumber
  }

  function syncViewerCards(handNumber: number, attempt = 0) {
    if (gameStateRef.current?.handNumber !== handNumber) return
    getGameState(tableId)
      .then((s) => {
        if (gameStateRef.current?.handNumber !== handNumber) return
        const me = s.players.find((p) => p.holeCards.length > 0)
        if (me) {
          stashViewerCards(s)
          setGameState((prev) => {
            if (!prev || prev.handNumber !== s.handNumber) return prev
            return {
              ...prev,
              players: prev.players.map((p) =>
                p.seatIndex === me.seatIndex ? { ...p, holeCards: me.holeCards } : p
              ),
            }
          })
          clearViewerCardsRetry()
          return
        }
        if (attempt < 20 && gameStateRef.current?.handNumber === handNumber && BETTING_PHASES.has(gameStateRef.current.phase)) {
          viewerCardsRetryRef.current = setTimeout(() => syncViewerCards(handNumber, attempt + 1), 250)
        }
      })
      .catch(() => {
        if (attempt < 20 && gameStateRef.current?.handNumber === handNumber && BETTING_PHASES.has(gameStateRef.current.phase)) {
          viewerCardsRetryRef.current = setTimeout(() => syncViewerCards(handNumber, attempt + 1), 250)
        }
      })
  }


  function clearAwardAnimTimer() {
    if (awardAnimTimerRef.current) {
      clearTimeout(awardAnimTimerRef.current)
      awardAnimTimerRef.current = null
    }
  }

  function playNextAwardAnim() {
    if (awardAnimTimerRef.current || awardAnimQueueRef.current.length === 0) return
    const next = awardAnimQueueRef.current.shift()!
    setAwardAnim({ ...next, stage: 'hold' })
    awardAnimTimerRef.current = setTimeout(() => {
      setAwardAnim((prev) => (prev ? { ...prev, stage: 'fly' } : prev))
      awardAnimTimerRef.current = setTimeout(() => {
        setAwardAnim((prev) => (prev ? { ...prev, stage: 'fade' } : prev))
        awardAnimTimerRef.current = setTimeout(() => {
          setAwardAnim(null)
          awardAnimTimerRef.current = null
          playNextAwardAnim()
        }, 350)
      }, 800)
    }, 600)
  }

  // Initial load
  useEffect(() => {
    Promise.all([
      getTable(tableId),
      getGameState(tableId).catch(() => null),
    ])
      .then(([t, s]) => { setTable(t); if (s) { stashViewerCards(s); setGameState(s) } })
      .catch(() => setError('Could not load table'))
      .finally(() => setLoading(false))
  }, [tableId])

  // WS update — use WS state directly (NEVER call getGameState here — avoids HTTP storm)
  const { connected, events } = useGameSocket(
    tableId,
    (wsState: GameStateView) => {
      gameStateRef.current = wsState
      communityCardsRef.current = wsState.communityCards ?? []
      setGameState((_prev) => {
        const mc = myCardsRef.current
        // Don't overlay my cards in FINISHED (server already reveals all in showdown broadcast)
        if (wsState.phase === 'FINISHED') return wsState
        if (mc && mc.hand === wsState.handNumber) {
          return {
            ...wsState,
            players: wsState.players.map((p) =>
              p.seatIndex === mc.seat && p.holeCards.length === 0
                ? { ...p, holeCards: mc.cards }
                : p
            ),
          }
        }
        return wsState
      })
    },
    (ev) => {
      if (ev.type === 'TableClosed') setClosedByOwner(true)
      if (ev.type === 'SeatsChanged') getTable(tableId).then(setTable).catch(() => {})
      if (ev.type === 'PlayerLeft') getTable(tableId).then(setTable).catch(() => {})
      if (ev.type === 'HandStarted') {
        const handNumber = Number(ev.data?.handNumber ?? -1)
        if (handNumber !== -1 && handNumber === lastHandStartedRef.current) return
        if (handNumber !== -1) lastHandStartedRef.current = handNumber
        myCardsRef.current = null
        clearViewerCardsRetry()
        setLastWinner(null)
        setSweepingToPot(false)
        setPotToWinner(null)
        setPotBreakdown([])
        clearAwardAnimTimer()
        awardAnimQueueRef.current = []
        setAwardAnim(null)
        prevPotRef.current = 0
        if (handNumber !== -1) {
          setTimeout(() => syncViewerCards(handNumber), 100)
        }
      }
      if (ev.type === 'BettingRoundCompleted') {
        potUpdateDelayRef.current = Date.now() + 850
        setSweepingToPot(true)
        setTimeout(() => {
          setSweepingToPot(false)
          setPotBreakdown([])
        }, 900)
      }
      if (ev.type === 'PotAwarded') {
        const d = ev.data ?? {}
        const gs = gameStateRef.current
        const winnerPlayer = gs?.players.find(p => p.id === d.winnerId)
        if (winnerPlayer != null) {
          setPotToWinner(winnerPlayer.seatIndex)
          setTimeout(() => setPotToWinner(null), 1200)
          awardAnimQueueRef.current.push({
            seatIndex: winnerPlayer.seatIndex,
            amount: d.amount ?? 0,
            label: d.isSidePot ? 'Side Pot' : 'Pot',
            isSidePot: Boolean(d.isSidePot),
          })
          playNextAwardAnim()
        }
        const playerName = (id: string) => gs?.players.find(p => p.id === id)?.displayName ?? id
        // Save name+rank now; correct net amount will arrive via HandFinished
        setLastWinner(w => w ?? { name: playerName(d.winnerId), amount: 0, rank: d.winningRank ?? null })
      }
      if (ev.type === 'HandFinished') {
        const d = ev.data ?? {}
        const gs = gameStateRef.current
        const deltas: Record<string, number> = d.chipDeltas ?? {}
        const positive = Object.entries(deltas).filter(([, v]) => Number(v) > 0)
        if (positive.length > 0) {
          const [winnerId, amount] = positive.sort((a, b) => Number(b[1]) - Number(a[1]))[0]
          const playerName = (id: string) => gs?.players.find(p => p.id === id)?.displayName ?? id
          setLastWinner(w =>
            w ? { ...w, amount: Number(amount) }
              : { name: playerName(winnerId), amount: Number(amount), rank: null }
          )
        }
      }
    },
    (id) => gameStateRef.current?.players.find(p => p.id === id)?.displayName ?? id,
    communityCardsRef,
  )

  // Fetch my hole cards ONCE per hand — personalized REST call
  useEffect(() => {
    if (!gameState || !BETTING_PHASES.has(gameState.phase)) return
    if (myCardsRef.current?.hand === gameState.handNumber) return
    if (fetchedHandRef.current === gameState.handNumber) return

    clearViewerCardsRetry()
    syncViewerCards(gameState.handNumber)
    return clearViewerCardsRetry
  }, [tableId, gameState?.handNumber, gameState?.phase])

  // Refresh table info when hand changes
  useEffect(() => {
    if (!gameState) return
    if (gameState.handNumber !== lastHandRef.current || gameState.phase === 'FINISHED') {
      lastHandRef.current = gameState.handNumber
      getTable(tableId).then(setTable).catch(() => {})
    }
  }, [tableId, gameState?.handNumber, gameState?.phase])

  // Fetch available actions when phase or active player changes
  useEffect(() => {
    if (!gameState) return
    const phaseChanged = gameState.phase !== prevPhaseRef.current
    const turnChanged = gameState.currentPlayerIndex !== prevPlayerIdxRef.current
    prevPhaseRef.current = gameState.phase
    prevPlayerIdxRef.current = gameState.currentPlayerIndex

    if (BETTING_PHASES.has(gameState.phase) && (phaseChanged || turnChanged)) {
      getAvailableActions(tableId)
        .then(setActions)
        .catch(() => setActions(null))
    } else if (!BETTING_PHASES.has(gameState.phase)) {
      setActions(null)
    }
  }, [tableId, gameState])

  // Update raise default when actions change
  useEffect(() => {
    const raise = actions?.actions.find((a) => a.type === 'RAISE')
    if (raise) setRaiseAmount(raise.amount)
  }, [actions])

  // Accumulate pot chips — add the delta when pot grows, never recalculate from scratch
  useEffect(() => {
    const currentPot = gameState?.potTotal ?? 0
    if (currentPot <= 0) { prevPotRef.current = 0; return }
    if (currentPot <= prevPotRef.current) return
    const delta = currentPot - prevPotRef.current
    prevPotRef.current = currentPot
    const delay = Math.max(0, potUpdateDelayRef.current - Date.now())
    setTimeout(() => {
      const added = decompose(delta)
      setPotBreakdown(prev => {
        const map = new Map(prev.map(s => [s.denom.value, s.count]))
        for (const { denom, count } of added) {
          map.set(denom.value, Math.min((map.get(denom.value) ?? 0) + count, 12))
        }
        return CHIP_DENOMS
          .map(d => ({ denom: d, count: map.get(d.value) ?? 0 }))
          .filter(s => s.count > 0)
      })
    }, delay)
  }, [gameState?.potTotal]) // eslint-disable-line react-hooks/exhaustive-deps

  async function handleAction(type: string, amount: number, silent = false) {
    if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null }
    setSubmitting(true)
    if (!silent) setError('')
    try {
      const next = await submitAction(tableId, type, amount)
      stashViewerCards(next)
      setGameState(next)
    } catch (err: any) {
      if (!silent) setError(err.response?.data?.error ?? 'Action failed')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleStartHand(silent = false) {
    try {
      const next = await startHand(tableId)
      stashViewerCards(next)
      setGameState(next)
    } catch (err: any) {
      if (!silent) setError(err.response?.data?.error ?? 'Could not start hand')
    }
  }

  function handleSit(seatIndex: number) {
    setBuyInAmount(table?.startingChips ?? minBuyIn)
    setBuyInModal(seatIndex)
  }

  async function handleBuyIn() {
    const seatIndex = buyInModal!
    setBuyInModal(null)
    setError('')
    try {
      setTable(await sitDown(tableId, buyInAmount, seatIndex))
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Could not sit down')
    }
  }

  async function handleRebuy() {
    setRebuyModal(false)
    setError('')
    try {
      const result = await rebuy(tableId, rebuyAmount)
      setTable(result)
      if (phase === 'FINISHED' && result.seats.filter((s) => s.chips > 0).length >= 2) {
        setTimeout(() => handleStartHand(true), 2000)
      }
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Re-buy failed')
    }
  }

  async function openStats() {
    try {
      setStats(await getTableStatistics(tableId))
      setStatsModal(true)
    } catch {
      setError('Could not load statistics')
    }
  }

  async function handlePause() {
    setPauseModal(false)
    setError('')
    try {
      setTable(await pauseTable(tableId, pauseMinutes))
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Could not pause table')
    }
  }

  async function handleLeave() {
    if (!window.confirm('Leave the table? If a hand is running you will be folded.')) return
    setError('')
    try {
      await standUp(tableId)
      navigate(table?.clubId ? `/clubs/${table.clubId}` : '/')
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Could not leave the table')
    }
  }

  async function handleCloseTable() {
    if (!window.confirm('Close this table for everyone? The current hand will be cancelled.')) return
    setError('')
    try {
      await closeTable(tableId)
      navigate(table?.clubId ? `/clubs/${table.clubId}` : '/')
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Could not close the table')
    }
  }

  // ── Derived values ──
  const maxPlayers = table?.maxPlayers ?? 6
  const hasGameState = !!gameState
  const players = gameState?.players ?? []
  const phase = gameState?.phase ?? 'WAITING'
  const community = gameState?.communityCards ?? []
  const communityKey = community.length === 0 ? '' : community.map(c => `${c.rank}-${c.suit}`).join(',')
  const pot = gameState?.potTotal ?? 0
  const dealerIdx = gameState?.dealerIndex ?? -1
  const currentIdx = gameState?.currentPlayerIndex ?? -1

  const mySeatIdx = (table?.seats ?? []).find((s) => s.username === username)?.seatIndex
  const myPlayer = hasGameState
    ? players.find((p) => p.seatIndex === mySeatIdx) ?? undefined
    : undefined
  const isSeated = (table?.seats ?? []).some((s) => s.username === username)

  const isStaff = table?.myRole === 'OWNER' || table?.myRole === 'ADMIN'
  const canSitDown =
    !isSeated &&
    table?.status !== 'CLOSED' &&
    (table?.seats?.length ?? 0) < maxPlayers

  const minBuyIn = (table?.bigBlind ?? 10) * 20
  const maxBuyIn = (table?.bigBlind ?? 10) * 1000

  useEffect(() => {
    if (pauseTimerRef.current) {
      clearInterval(pauseTimerRef.current)
      pauseTimerRef.current = null
    }
    const until = table?.pausedUntil ? new Date(table.pausedUntil).getTime() : 0
    if (!until) {
      setPauseRemaining(0)
      return
    }
    const update = () => setPauseRemaining(Math.max(0, Math.ceil((until - Date.now()) / 1000)))
    update()
    pauseTimerRef.current = setInterval(update, 1000)
    return () => {
      if (pauseTimerRef.current) {
        clearInterval(pauseTimerRef.current)
        pauseTimerRef.current = null
      }
    }
  }, [table?.pausedUntil])

  const isPauseActive = pauseRemaining > 0
  const formatDuration = (totalSeconds: number) => {
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    return minutes > 0 ? `${minutes}m ${seconds.toString().padStart(2, '0')}s` : `${seconds}s`
  }

  const seatedCount = hasGameState
    ? players.filter((p) => p.chips > 0).length
    : (table?.seats ?? []).length
  const dbPlayersWithChips = (table?.seats ?? []).filter(s => s.chips > 0).length
  const isGameOver = phase === 'FINISHED' && dbPlayersWithChips < 2
  const potParts = gameState?.pots ?? []
  const hasSidePots = potParts.length > 1

  const raiseAction = actions?.actions.find((a) => a.type === 'RAISE')
  const myChips = myPlayer?.chips ?? table?.seats?.find((s) => s.username === username)?.chips ?? 0

  // ── Turn timer ──
  const timeoutSecs = table?.actionTimeoutSecs ?? 30
  const [timeLeft, setTimeLeft] = useState(timeoutSecs)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null }
    if (actions?.yourTurn && sittingOut) setSittingOut(false)
    if (!actions?.yourTurn) { setTimeLeft(timeoutSecs); return }
    if (myPlayer?.status === 'ALL_IN') { setTimeLeft(timeoutSecs); return }

    setTimeLeft(timeoutSecs)
    timerRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null }
          if (BETTING_PHASES.has(phase)) {
            consecutiveTimeoutsRef.current += 1
            if (consecutiveTimeoutsRef.current >= 2) {
              setSittingOut(true)
              sitOutAPI(tableId).catch(() => {})
            }
            handleAction('FOLD', 0, true)
          }
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => {
      if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null }
    }
  }, [actions?.yourTurn, currentIdx])

  // Show-cards countdown when player is FOLDED at FINISHED
  useEffect(() => {
    if (showCardsTimerRef.current) { clearInterval(showCardsTimerRef.current); showCardsTimerRef.current = null }
    if (phase === 'FINISHED' && myPlayer?.status === 'FOLDED' && (myPlayer?.holeCards?.length ?? 0) > 0) {
      setShowCardsLeft(5)
      showCardsTimerRef.current = setInterval(() => {
        setShowCardsLeft((prev) => {
          if (prev <= 1) { clearInterval(showCardsTimerRef.current!); showCardsTimerRef.current = null; return 0 }
          return prev - 1
        })
      }, 1000)
    } else {
      setShowCardsLeft(0)
    }
    return () => { if (showCardsTimerRef.current) { clearInterval(showCardsTimerRef.current); showCardsTimerRef.current = null } }
  }, [phase]) // eslint-disable-line react-hooks/exhaustive-deps

  // Auto-start next hand after FINISHED (5-second delay so winner banner is visible).
  // Count from table.seats (DB) — session players may not include late joiners.
  useEffect(() => {
    if (phase !== 'FINISHED' || !isSeated) return
    const playersWithChips = (table?.seats ?? []).filter(s => s.chips > 0).length
    if (playersWithChips < 2) return
    const timer = setTimeout(() => handleStartHand(true), 5000)
    return () => clearTimeout(timer)
  }, [phase, table?.seats]) // eslint-disable-line react-hooks/exhaustive-deps

  // ── Events sidebar scroll preservation ────────────────────────────────
  // When new events are prepended, keep the user's scroll position stable.
  // If the user was at the bottom, auto-scroll to the new bottom.
  useEffect(() => {
    const el = eventsScrollRef.current
    if (!el) return
    const wasAtBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 40
    // Use rAF to measure after React has painted the new DOM
    const raf = requestAnimationFrame(() => {
      if (wasAtBottom) {
        el.scrollTop = el.scrollHeight
      }
    })
    return () => cancelAnimationFrame(raf)
  }, [events])

  // Re-buy prompt when player busts
  useEffect(() => {
    if (myPlayer?.chips === 0 && isSeated && phase === 'FINISHED') {
      setRebuyAmount(table?.startingChips ?? minBuyIn)
      setRebuyModal(true)
    }
  }, [myPlayer?.chips, phase]) // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-950 text-white flex flex-col">
        <Navbar />
        <div className="flex-1 flex items-center justify-center">
          <p className="text-gray-400">Loading table...</p>
        </div>
      </div>
    )
  }

  const tableClosed = closedByOwner || table?.status === 'CLOSED'

  return (
    <div className="h-screen bg-gray-950 text-white flex flex-col overflow-hidden">
      <Navbar />

      {tableClosed && (
        <div className="fixed inset-0 z-50 bg-black/70 flex items-center justify-center">
          <div className="bg-gray-900 border border-gray-700 rounded-xl p-8 text-center space-y-4 shadow-2xl">
            <p className="text-2xl">⏹</p>
            <p className="text-xl font-semibold">Table closed</p>
            <p className="text-gray-400 text-sm">The owner has closed this table. The game is over.</p>
            <button
              onClick={() => navigate(table?.clubId ? `/clubs/${table.clubId}` : '/')}
              className="bg-blue-600 hover:bg-blue-700 px-6 py-2 rounded-lg font-semibold transition"
            >
              ← Back to club
            </button>
          </div>
        </div>
      )}

      <div className="flex-1 flex overflow-hidden">
        <div className="flex-1 flex flex-col items-center justify-center p-4 gap-4">

          {/* Top bar */}
          <div className="flex items-center gap-4 text-sm w-full">
            <button onClick={() => navigate(-1)} className="text-gray-500 hover:text-gray-300 transition">
              ← Back
            </button>
            <span className="text-gray-400 font-medium">{table?.name}</span>
            <span className="text-gray-500">{table?.smallBlind}/{table?.bigBlind} blinds</span>
            <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${connected ? 'bg-green-900 text-green-400' : 'bg-red-900 text-red-400'}`}>
              {connected ? 'Live' : 'Connecting…'}
            </span>
            <div className="ml-auto flex items-center gap-2">
              <button onClick={openStats} className="bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded-lg text-xs font-semibold transition">
                Stats
              </button>
              {isSeated && (
                <button onClick={() => { setRebuyAmount(table?.rebuyMin || minBuyIn); setRebuyModal(true) }} className="bg-emerald-700 hover:bg-emerald-600 px-3 py-1 rounded-lg text-xs font-semibold transition">
                  Rebuy
                </button>
              )}
              {isSeated && (
                <button onClick={handleLeave} className="bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded-lg text-xs font-semibold transition">
                  Leave table
                </button>
              )}
              {isStaff && (
                <button
                  onClick={() => setPauseModal(true)}
                  className={`px-3 py-1 rounded-lg text-xs font-semibold transition ${isPauseActive ? 'bg-indigo-700 hover:bg-indigo-600' : 'bg-indigo-800 hover:bg-indigo-700'}`}
                >
                  ? {isPauseActive ? `Paused ${formatDuration(pauseRemaining)}` : 'Pause'}
                </button>
              )}
              {isStaff && (
                <button onClick={handleCloseTable} className="bg-red-800 hover:bg-red-700 px-3 py-1 rounded-lg text-xs font-semibold transition">
                  ? Close table
                </button>
              )}
            </div>
          </div>

          {/* Poker table */}
          <div className="relative w-full" style={{ maxWidth: 760, aspectRatio: '16/9' }}>
            <div
              className="absolute"
              style={{
                left: '10%', top: '8%', width: '80%', height: '84%',
                borderRadius: '50%',
                background: 'radial-gradient(ellipse at center, #15803d 0%, #166534 55%, #14532d 100%)',
                border: '10px solid #7c3d12',
                boxShadow: '0 4px 32px rgba(0,0,0,.8), inset 0 0 40px rgba(0,0,0,.4)',
                pointerEvents: 'none',
                overflow: 'hidden',
              }}
            >
              {/* Phase label */}
              <span
                className="absolute text-green-300 text-xs font-semibold tracking-widest uppercase opacity-60"
                style={{ top: '14%', left: '50%', transform: 'translateX(-50%)', whiteSpace: 'nowrap' }}
              >
                {phase.replace('_', ' ')}
              </span>
              {/* Total pot — above cards */}
              {pot > 0 && (
                <div
                  className="absolute bg-black/50 rounded-full px-3 py-0.5"
                  style={{ top: '26%', left: '50%', transform: 'translateX(-50%)', whiteSpace: 'nowrap' }}
                >
                  <span className="text-yellow-300 text-sm font-bold">Pot: {pot}</span>
                </div>
              )}
              {/* Community cards — always centered */}
              <div
                className="absolute flex gap-1 items-center"
                style={{ left: '50%', top: '50%', transform: 'translate(-50%, -50%)' }}
              >
                {community.map((c, i) => <PlayingCard key={i} {...c} />)}
              </div>
              {/* Pot chip pile sweep animation */}
              {potBreakdown.length > 0 && (
                <div
                  className="absolute flex items-center gap-2"
                  style={{
                    bottom: '22%', left: '50%',
                    transform: sweepingToPot ? 'translateX(-50%) scale(0.8)' : 'translateX(-50%) scale(1)',
                    transition: sweepingToPot ? 'opacity 0.6s ease-in, transform 0.6s ease-in' : 'none',
                    opacity: sweepingToPot ? 0 : 1,
                  }}
                >
                  <ChipPile stacks={potBreakdown} chipW={26} />
                </div>
              )}
              {/* Side pots — only shown when someone is ALL_IN */}
              {hasSidePots && (
                <div
                  className="absolute flex flex-wrap items-center justify-center gap-1"
                  style={{ bottom: '12%', left: '50%', transform: 'translateX(-50%)', maxWidth: '60%' }}
                >
                  {potParts.slice(1).map((part) => (
                    <span
                      key={part.index}
                      className="bg-black/50 rounded-full px-2 py-0.5 text-yellow-200 text-[10px] font-bold whitespace-nowrap"
                    >
                      {part.label}: {part.amount}
                    </span>
                  ))}
                </div>
              )}
            </div>

            {!hasGameState && (table?.seats ?? []).length > 0
              ? Array.from({ length: maxPlayers }).map((_, i) => {
                  const seat = (table?.seats ?? []).find((s) => s.seatIndex === i)
                  const pos = getSeatPos(i, maxPlayers)
                  const isMe = seat?.username === username
                  return (
                    <div key={`seat-${i}`} className="absolute" style={{ left: pos.left, top: pos.top, transform: 'translate(-50%,-50%)', zIndex: 10 }}>
                      {seat ? (
                        <div className="flex flex-col items-center gap-1">
                          <div className={`px-2 py-1 rounded-lg text-center text-xs shadow-lg border min-w-[76px] ${isMe ? 'bg-blue-700 border-blue-400 text-white' : 'bg-gray-800 border-gray-600 text-gray-200'}`}>
                            <div className="font-semibold truncate max-w-[76px]">{seat.username}</div>
                            <div className="opacity-80">{seat.chips} chips</div>
                          </div>
                        </div>
                      ) : canSitDown ? (
                        <button
                          type="button"
                          onMouseDown={() => handleSit(i)}
                          className="w-16 h-8 border border-dashed border-green-600 rounded-lg flex items-center justify-center hover:bg-green-900/30 transition"
                        >
                          <span className="text-green-500 text-xs font-semibold">+ Sit</span>
                        </button>
                      ) : (
                        <div className="w-16 h-7 border border-dashed border-gray-700 rounded-lg flex items-center justify-center">
                          <span className="text-gray-700 text-xs">empty</span>
                        </div>
                      )}
                    </div>
                  )
                })
              : <>
                  {Array.from({ length: maxPlayers }).map((_, i) => {
                    if (players.some((p) => p.seatIndex === i)) return null
                    const pos = getSeatPos(i, maxPlayers)
                    const waitingSeat = (table?.seats ?? []).find((s) => s.seatIndex === i)
                    return (
                      <div key={`empty-${i}`} className="absolute" style={{ left: pos.left, top: pos.top, transform: 'translate(-50%,-50%)' }}>
                        {waitingSeat ? (
                          <div className={`px-2 py-1 rounded-lg text-center text-xs shadow-lg border min-w-[76px] ${waitingSeat.username === username ? 'bg-blue-700/50 border-blue-400 text-blue-200' : 'bg-gray-800/50 border-gray-600 text-gray-400'}`}>
                            <div className="font-semibold truncate max-w-[76px]">{waitingSeat.username}</div>
                            <div className="text-[9px] opacity-60">waiting…</div>
                          </div>
                        ) : canSitDown ? (
                          <button
                            type="button"
                            onMouseDown={() => handleSit(i)}
                            className="w-16 h-8 border border-dashed border-green-600 rounded-lg flex items-center justify-center hover:bg-green-900/30 transition"
                          >
                            <span className="text-green-500 text-xs font-semibold">+ Sit</span>
                          </button>
                        ) : (
                          <div className="w-16 h-7 border border-dashed border-gray-700 rounded-lg flex items-center justify-center">
                            <span className="text-gray-700 text-xs">empty</span>
                          </div>
                        )}
                      </div>
                    )
                  })}
                  {players.map((player) => (
                    <PlayerSeat
                      key={player.id}
                      player={player}
                      isMe={player.seatIndex === mySeatIdx}
                      isDealer={player.id === players[dealerIdx]?.id && phase !== 'WAITING'}
                      isActive={player.id === players[currentIdx]?.id && BETTING_PHASES.has(phase)}
                      maxPlayers={maxPlayers}
                      receivingPot={potToWinner === player.seatIndex}
                      phase={phase}
                    />
                  ))}

                  {/* Bet chips inside table, between each seat and center */}
                  {players.map((player) => {
                    if (player.currentBet <= 0) return null
                    const seatPos = getSeatPos(player.seatIndex, maxPlayers)
                    const betPos = getBetChipPos(seatPos)
                    return (
                      <div
                        key={`bet-${player.id}`}
                        className="absolute flex flex-col items-center gap-0.5"
                        style={{
                          left: betPos.left,
                          top: betPos.top,
                          transform: 'translate(-50%, -50%)',
                          transition: sweepingToPot ? 'opacity 0.7s ease-in, transform 0.7s ease-in' : 'none',
                          opacity: sweepingToPot ? 0 : 1,
                          zIndex: sweepingToPot ? 0 : 5,
                        }}
                      >
                        <ChipPile amount={player.currentBet} chipW={22} />
                        <span className="text-yellow-300 text-[9px] font-bold bg-black/70 px-1 rounded leading-tight">
                          {player.currentBet}
                        </span>
                      </div>
                    )
                  })}
                </>
            }
            {/* Award chip animation — single motion from center to winner, then fade */}
            {awardAnim && (() => {
              const winnerPos = getSeatPos(awardAnim.seatIndex, maxPlayers)
              const start = { left: '50%', top: '50%' }
              const end = { left: winnerPos.left, top: winnerPos.top }
              const stylePos = awardAnim.stage === 'hold' ? start : end
              const opacity = awardAnim.stage === 'fade' ? 0 : 1
              return (
                <div
                  className="absolute flex flex-col items-center gap-0.5 pointer-events-none"
                  style={{
                    left: stylePos.left,
                    top: stylePos.top,
                    transform: 'translate(-50%, -50%)',
                    transition: 'left 0.8s cubic-bezier(.2,.9,.2,1), top 0.8s cubic-bezier(.2,.9,.2,1), opacity 0.4s ease',
                    opacity,
                    zIndex: 30,
                  }}
                >
                  <ChipPile amount={awardAnim.amount} chipW={24} />
                  <span className="text-yellow-100 text-[9px] font-bold bg-black/75 px-1 rounded leading-tight whitespace-nowrap">
                    {awardAnim.label} {awardAnim.amount}
                  </span>
                </div>
              )
            })()}
          </div>

          {error && <p className="text-red-400 text-sm">{error}</p>}

          {/* Winner banner */}
          {phase === 'FINISHED' && lastWinner && (
            <div className="w-full max-w-lg bg-yellow-900/60 border border-yellow-600 rounded-xl p-4 text-center space-y-1 shadow-lg">
              <div className="text-2xl">🏆</div>
              <div className="text-yellow-300 font-bold text-lg">{lastWinner.name} wins {lastWinner.amount} chips</div>
              {lastWinner.rank && (
                <div className="text-yellow-500 text-sm">{lastWinner.rank.replace(/_/g, ' ')}</div>
              )}
              {isGameOver && (
                <div className="text-gray-400 text-sm pt-1">Game over — no chips left for next hand</div>
              )}
            </div>
          )}

          {isPauseActive && (
            <div className="w-full max-w-lg bg-indigo-900/50 border border-indigo-700 rounded-xl px-4 py-3 text-sm text-indigo-100 shadow-lg text-center">
              Table paused ? resumes in {formatDuration(pauseRemaining)}
            </div>
          )}

          {/* Action panel */}
          <div className="w-full max-w-lg">
            {actions?.yourTurn && myPlayer?.status !== 'ALL_IN' && (
              <div className="bg-gray-800 rounded-xl p-4 space-y-3 shadow-lg">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-semibold text-yellow-400">Your turn</p>
                  <span className={`text-sm font-bold tabular-nums ${timeLeft <= 5 ? 'text-red-400 animate-pulse' : 'text-gray-300'}`}>
                    ⏱ {timeLeft}s
                  </span>
                </div>
                {raiseAction && (
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs text-gray-400">
                      <span>Raise to: <span className="text-white font-semibold">{raiseAmount}</span></span>
                      <span>All-in: {myChips}</span>
                    </div>
                    <input type="range" min={raiseAction.amount} max={myChips} value={raiseAmount} onChange={(e) => setRaiseAmount(Number(e.target.value))} className="w-full accent-green-500" />
                  </div>
                )}
                <div className="flex gap-2 justify-center flex-wrap">
                  {actions.actions.map((a) => (
                    <button
                      key={a.type}
                      disabled={submitting}
                      onClick={() => handleAction(a.type, a.type === 'RAISE' ? raiseAmount : a.amount)}
                      className={`px-5 py-2 rounded-lg font-semibold text-sm disabled:opacity-50 transition text-white ${
                        a.type === 'FOLD'    ? 'bg-red-700 hover:bg-red-600' :
                        a.type === 'RAISE'   ? 'bg-green-700 hover:bg-green-600' :
                        a.type === 'ALL_IN'  ? 'bg-purple-700 hover:bg-purple-600' :
                                               'bg-blue-700 hover:bg-blue-600'
                      }`}
                    >
                      {a.type === 'RAISE'  ? `Raise → ${raiseAmount}` :
                       a.type === 'CALL'   ? `Call ${a.amount}` :
                       a.type === 'ALL_IN' ? `All-In ${a.amount}` :
                       a.type}
                    </button>
                  ))}
                </div>
              </div>
            )}

            {sittingOut && (
              <div className="flex justify-center">
                <button
                  onClick={() => { setSittingOut(false); consecutiveTimeoutsRef.current = 0; imBackAPI(tableId).catch(() => {}) }}
                  className="bg-yellow-600 hover:bg-yellow-700 px-6 py-2 rounded-lg font-semibold text-sm transition"
                >
                  I'm back
                </button>
              </div>
            )}

            {showCardsLeft > 0 && (
              <div className="flex justify-center">
                <button
                  onClick={() => { setShowCardsLeft(0); showCardsAPI(tableId).catch(() => {}) }}
                  className="bg-indigo-700 hover:bg-indigo-600 px-5 py-2 rounded-lg font-semibold text-sm transition"
                >
                  Show Cards ({showCardsLeft}s)
                </button>
              </div>
            )}

            {!actions?.yourTurn && !sittingOut && BETTING_PHASES.has(phase) && myPlayer && (
              <p className="text-center text-gray-600 text-sm">Waiting for opponent…</p>
            )}

            {!isSeated && (
              <p className="text-center text-gray-600 text-sm">You are spectating</p>
            )}
          </div>
        </div>

        {/* Events sidebar */}
        <div className="w-64 bg-gray-900 border-l border-gray-800 flex flex-col min-h-0 text-sm">
          <div className="px-3 py-2 border-b border-gray-800 shrink-0">
            <span className="font-semibold text-gray-500 uppercase tracking-wide">Events</span>
          </div>
          <div
            ref={eventsScrollRef}
            className="flex-1 min-h-0 overflow-y-auto p-2 space-y-1"
          >
            {events.length === 0 ? (
              <p className="text-gray-700 text-center mt-4">No events yet</p>
            ) : (
              events.map((e, i) => (
                <div key={i} className="text-gray-400 border-b border-gray-800 pb-1 break-words">
                  {e}
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Buy-in modal */}
      {buyInModal !== null && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center" style={{ zIndex: 9999 }}>
          <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 w-80 space-y-4 shadow-2xl">
            <h2 className="text-lg font-semibold">Buy In — Seat {buyInModal + 1}</h2>
            <div className="space-y-1">
              <label className="text-xs text-gray-400">Amount (min {minBuyIn} / max {maxBuyIn})</label>
              <input
                type="number"
                min={minBuyIn}
                max={maxBuyIn}
                value={buyInAmount}
                onChange={(e) => setBuyInAmount(Number(e.target.value))}
                className="w-full bg-gray-800 border border-gray-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-green-500"
              />
              <input
                type="range"
                min={minBuyIn}
                max={maxBuyIn}
                step={table?.bigBlind ?? 10}
                value={buyInAmount}
                onChange={(e) => setBuyInAmount(Number(e.target.value))}
                className="w-full accent-green-500"
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={handleBuyIn}
                disabled={buyInAmount < minBuyIn || buyInAmount > maxBuyIn}
                className="flex-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 py-2 rounded-lg font-semibold transition"
              >
                Sit Down
              </button>
              <button
                onClick={() => setBuyInModal(null)}
                className="flex-1 bg-gray-700 hover:bg-gray-600 py-2 rounded-lg font-semibold transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Pause modal */}
      {pauseModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center" style={{ zIndex: 9999 }}>
          <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 w-80 space-y-4 shadow-2xl relative">
            <button
              onClick={() => setPauseModal(false)}
              className="absolute top-3 right-3 text-gray-500 hover:text-gray-300 text-xl leading-none"
              aria-label="Close pause modal"
            >
              ?
            </button>
            <h2 className="text-lg font-semibold pr-6">Pause table</h2>
            <p className="text-sm text-gray-400">Choose how long the table will be paused for everyone.</p>
            <div className="space-y-1">
              <label className="text-xs text-gray-400">Minutes (1?10)</label>
              <input
                type="number"
                min={1}
                max={10}
                value={pauseMinutes}
                onChange={(e) => setPauseMinutes(Math.max(1, Math.min(10, Number(e.target.value) || 1)))}
                className="w-full bg-gray-800 border border-gray-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-indigo-500"
              />
              <input
                type="range"
                min={1}
                max={10}
                step={1}
                value={pauseMinutes}
                onChange={(e) => setPauseMinutes(Number(e.target.value))}
                className="w-full accent-indigo-500"
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={handlePause}
                className="flex-1 bg-indigo-600 hover:bg-indigo-700 py-2 rounded-lg font-semibold transition"
              >
                Pause
              </button>
              <button
                onClick={() => setPauseModal(false)}
                className="flex-1 bg-gray-700 hover:bg-gray-600 py-2 rounded-lg font-semibold transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Re-buy modal */}
      {rebuyModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center" style={{ zIndex: 9999 }}>
          <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 w-80 space-y-4 shadow-2xl relative">
            <button
              onClick={() => setRebuyModal(false)}
              className="absolute top-3 right-3 text-gray-500 hover:text-gray-300 text-xl leading-none"
              aria-label="Close rebuy modal"
            >
              ×
            </button>
            <h2 className="text-lg font-semibold pr-6">Re-Buy</h2>
            {BETTING_PHASES.has(phase) && (
              <p className="text-xs text-yellow-400 bg-yellow-900/30 rounded-lg px-3 py-2">
                ⚠ Hand in progress — chips will be added after this hand finishes.
              </p>
            )}
            <div className="space-y-1">
              <label className="text-xs text-gray-400">
                Amount ({table?.rebuyMin || minBuyIn} — {table?.rebuyMax || maxBuyIn} chips)
              </label>
              <input
                type="number"
                min={table?.rebuyMin || minBuyIn}
                max={table?.rebuyMax || maxBuyIn}
                value={rebuyAmount}
                onChange={(e) => setRebuyAmount(Number(e.target.value))}
                className="w-full bg-gray-800 border border-gray-600 rounded-lg px-3 py-2 text-white focus:outline-none focus:border-green-500"
              />
              <input
                type="range"
                min={table?.rebuyMin || minBuyIn}
                max={table?.rebuyMax || maxBuyIn}
                step={table?.bigBlind ?? 10}
                value={rebuyAmount}
                onChange={(e) => setRebuyAmount(Number(e.target.value))}
                className="w-full accent-green-500"
              />
            </div>
            <div className="flex gap-3">
              <button
                onClick={handleRebuy}
                disabled={rebuyAmount < (table?.rebuyMin || minBuyIn) || rebuyAmount > (table?.rebuyMax || maxBuyIn)}
                className="flex-1 bg-green-600 hover:bg-green-700 disabled:opacity-50 py-2 rounded-lg font-semibold transition"
              >
                Re-Buy
              </button>
              <button
                onClick={() => setRebuyModal(false)}
                className="flex-1 bg-gray-700 hover:bg-gray-600 py-2 rounded-lg font-semibold transition"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Statistics modal */}
      {statsModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center" style={{ zIndex: 9999 }}>
          <div className="bg-gray-900 border border-gray-700 rounded-xl p-6 w-96 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Table Statistics</h2>
              <button onClick={() => setStatsModal(false)} className="text-gray-500 hover:text-gray-300 text-xl leading-none">×</button>
            </div>
            {stats && stats.length > 0 ? (
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-gray-400 text-xs border-b border-gray-700">
                    <th className="text-left pb-2">Player</th>
                    <th className="text-right pb-2">Chips</th>
                    <th className="text-right pb-2">Bought</th>
                    <th className="text-right pb-2">Net</th>
                  </tr>
                </thead>
                <tbody>
                  {stats.map((s) => (
                    <tr key={s.username} className="border-b border-gray-800">
                      <td className="py-1.5 font-medium">{s.username}</td>
                      <td className="py-1.5 text-right">{s.chips}</td>
                      <td className="py-1.5 text-right text-gray-400">{s.totalBuyIn}</td>
                      <td className={`py-1.5 text-right font-semibold ${s.net >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                        {s.net >= 0 ? '+' : ''}{s.net}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            ) : (
              <p className="text-gray-500 text-sm text-center">No players at this table</p>
            )}
          </div>
        </div>
      )}
    </div>
  )
}