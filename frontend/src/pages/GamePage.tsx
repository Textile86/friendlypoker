import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { useAuth } from '../context/AuthContext'
import { useGameSocket } from '../hooks/useGameSocket'
import {
  CardView, PlayerView, GameStateView, AvailableActions,
  getGameState, getAvailableActions, startHand, submitAction,
} from '../api/game'
import { getTable, TableResponse } from '../api/tables'

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
      className={`w-9 h-13 bg-white rounded-md shadow-md flex flex-col items-center justify-center font-bold leading-tight select-none ${isRed(suit) ? 'text-red-600' : 'text-gray-900'}`}
      style={{ width: 36, height: 52 }}
    >
      <span className="text-sm">{RANK_DISPLAY[rank] ?? rank}</span>
      <span className="text-base">{SUIT_SYMBOL[suit] ?? suit}</span>
    </div>
  )
}

function CardBack() {
  return (
    <div
      className="rounded-md shadow-md border-2 border-blue-500 bg-blue-700 flex items-center justify-center"
      style={{ width: 36, height: 52 }}
    >
      <span className="text-blue-300 text-lg">★</span>
    </div>
  )
}

// ─── Seat positions ───────────────────────────────────────────────────────────

type Pos = { left: string; top: string }

const POSITIONS: Record<number, Pos[]> = {
  2: [
    { left: '50%', top: '88%' },
    { left: '50%', top: '4%' },
  ],
  4: [
    { left: '50%', top: '88%' },
    { left: '90%', top: '44%' },
    { left: '50%', top: '4%' },
    { left: '10%', top: '44%' },
  ],
  6: [
    { left: '50%', top: '88%' },
    { left: '85%', top: '74%' },
    { left: '93%', top: '30%' },
    { left: '67%', top: '2%' },
    { left: '33%', top: '2%' },
    { left: '7%', top: '30%' },
  ],
  9: [
    { left: '50%', top: '90%' },
    { left: '76%', top: '80%' },
    { left: '93%', top: '55%' },
    { left: '93%', top: '25%' },
    { left: '75%', top: '4%' },
    { left: '50%', top: '0%' },
    { left: '25%', top: '4%' },
    { left: '7%', top: '25%' },
    { left: '7%', top: '55%' },
  ],
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
}

function PlayerSeat({ player, isMe, isDealer, isActive, maxPlayers }: PlayerSeatProps) {
  const pos = getSeatPos(player.seatIndex, maxPlayers)
  const folded = player.status === 'FOLDED' || player.status === 'SITTING_OUT'

  return (
    <div
      className="absolute"
      style={{ left: pos.left, top: pos.top, transform: 'translate(-50%, -50%)', zIndex: 10 }}
    >
      <div className={`flex flex-col items-center gap-1 transition-opacity ${folded ? 'opacity-40' : ''}`}>
        {/* Hole cards */}
        <div className="flex gap-0.5">
          {player.holeCards.length > 0
            ? player.holeCards.map((c, i) => <PlayingCard key={i} {...c} />)
            : (player.status === 'ACTIVE' || player.status === 'ALL_IN')
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
        }`}>
          <div className="font-semibold truncate max-w-[76px]">{player.displayName}</div>
          <div className="opacity-80">{player.chips} chips</div>
          {player.currentBet > 0 && (
            <div className="text-yellow-300">bet: {player.currentBet}</div>
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
  const [sittingOut, setSittingOut] = useState(false)

  // Initial load
  useEffect(() => {
    Promise.all([
      getTable(tableId),
      getGameState(tableId).catch(() => null),
    ])
      .then(([t, s]) => { setTable(t); if (s) setGameState(s) })
      .catch(() => setError('Could not load table'))
      .finally(() => setLoading(false))
  }, [tableId])

  // WebSocket: updates gameState live, preserving local player's hole cards
  const { connected, events } = useGameSocket(tableId, (wsState: GameStateView) => {
    setGameState((prev) => {
      if (!prev) return wsState
      // WebSocket broadcast doesn't include hole cards (null viewer).
      // Preserve hole cards from previous state for the player who had them.
      const prevMe = prev.players.find((p) => p.holeCards.length > 0)
      if (!prevMe) return wsState
      return {
        ...wsState,
        players: wsState.players.map((p) =>
          p.seatIndex === prevMe.seatIndex
            ? { ...p, holeCards: prevMe.holeCards }
            : p
        ),
      }
    })
  })

  // Fetch available actions whenever state changes
  const prevPhaseRef = useRef('')
  const prevPlayerIdxRef = useRef(-1)

  useEffect(() => {
    if (!gameState) return
    const phaseChanged = gameState.phase !== prevPhaseRef.current
    const turnChanged = gameState.currentPlayerIndex !== prevPlayerIdxRef.current
    const prevPhase = prevPhaseRef.current
    prevPhaseRef.current = gameState.phase
    prevPlayerIdxRef.current = gameState.currentPlayerIndex

    // When game just started (WAITING→PRE_FLOP), fetch state with viewer context
    // to get hole cards for the non-starter player (bob)
    if (prevPhase === 'WAITING' && BETTING_PHASES.has(gameState.phase)) {
      getGameState(tableId).then((s) => { if (s) setGameState(s) }).catch(() => {})
    }

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

  async function handleAction(type: string, amount: number) {
    setSubmitting(true)
    setError('')
    try {
      const next = await submitAction(tableId, type, amount)
      setGameState(next)
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Action failed')
    } finally {
      setSubmitting(false)
    }
  }

  async function handleStartHand() {
    setSubmitting(true)
    setError('')
    try {
      const next = await startHand(tableId)
      setGameState(next)
      // Fetch state again with viewer context to get hole cards
      try {
        const withCards = await getGameState(tableId)
        if (withCards) setGameState(withCards)
      } catch {}
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Could not start hand')
    } finally {
      setSubmitting(false)
    }
  }

  // ── Derived values ──
  const maxPlayers = table?.maxPlayers ?? 6
  const hasGameState = !!gameState
  const players = gameState?.players ?? []
  const phase = gameState?.phase ?? 'WAITING'
  const community = gameState?.communityCards ?? []
  const pot = gameState?.potTotal ?? 0
  const dealerIdx = gameState?.dealerIndex ?? -1
  const currentIdx = gameState?.currentPlayerIndex ?? -1

  // Match player by seatIndex (player.id is DB numeric ID, username is login string)
  const mySeatIdx = hasGameState
    ? (table?.seats ?? []).find((s) => s.username === username)?.seatIndex
    : (table?.seats ?? []).find((s) => s.username === username)?.seatIndex
  const myPlayer = hasGameState
    ? players.find((p) => p.seatIndex === mySeatIdx) ?? undefined
    : undefined
  const isSeated = hasGameState
    ? !!myPlayer
    : (table?.seats ?? []).some((s) => s.username === username)

  // When game hasn't started yet, use table.seats
  const seatedCount = hasGameState
    ? players.filter((p) => p.chips > 0).length
    : (table?.seats ?? []).length
  const canStartHand =
    isSeated &&
    (phase === 'WAITING' || phase === 'FINISHED') &&
    seatedCount >= 2

  const raiseAction = actions?.actions.find((a) => a.type === 'RAISE')
  const myChips = myPlayer?.chips ?? table?.seats?.find((s) => s.username === username)?.chips ?? 0

  // ── Turn timer ──
  const timeoutSecs = table?.actionTimeoutSecs ?? 30
  const [timeLeft, setTimeLeft] = useState(timeoutSecs)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  // Reset timer when turn changes (skip if player is all-in)
  useEffect(() => {
    if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null }
    if (!actions?.yourTurn) { setTimeLeft(timeoutSecs); return }
    // Don't start timer for all-in players — game should auto-resolve
    if (myPlayer?.status === 'ALL_IN') { setTimeLeft(timeoutSecs); return }

    setTimeLeft(timeoutSecs)
    timerRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          // Auto-fold when time runs out
          if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null }
          // Only send FOLD if still in a betting phase (not already FINISHED)
          if (BETTING_PHASES.has(phase)) {
            setSittingOut(true)
            handleAction('FOLD', 0)
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

  return (
    <div className="min-h-screen bg-gray-950 text-white flex flex-col">
      <Navbar />

      <div className="flex-1 flex overflow-hidden">
        {/* ── Main game area ── */}
        <div className="flex-1 flex flex-col items-center justify-center p-4 gap-4">

          {/* Top bar */}
          <div className="flex items-center gap-4 text-sm self-start">
            <button
              onClick={() => navigate(-1)}
              className="text-gray-500 hover:text-gray-300 transition"
            >
              ← Back
            </button>
            <span className="text-gray-400 font-medium">{table?.name}</span>
            <span className="text-gray-500">
              {table?.smallBlind}/{table?.bigBlind} blinds
            </span>
            <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
              connected ? 'bg-green-900 text-green-400' : 'bg-red-900 text-red-400'
            }`}>
              {connected ? 'Live' : 'Connecting…'}
            </span>
          </div>

          {/* Poker table */}
          <div
            className="relative w-full"
            style={{ maxWidth: 760, aspectRatio: '16/7' }}
          >
            {/* Oval felt */}
            <div
              className="absolute flex flex-col items-center justify-center gap-2"
              style={{
                left: '10%', top: '8%', width: '80%', height: '84%',
                borderRadius: '50%',
                background: 'radial-gradient(ellipse at center, #15803d 0%, #166534 55%, #14532d 100%)',
                border: '10px solid #7c3d12',
                boxShadow: '0 4px 32px rgba(0,0,0,.8), inset 0 0 40px rgba(0,0,0,.4)',
              }}
            >
              {/* Phase label */}
              <span className="text-green-300 text-xs font-semibold tracking-widest uppercase opacity-60">
                {phase.replace('_', ' ')}
              </span>

              {/* Community cards */}
              <div className="flex gap-1 min-h-[52px] items-center">
                {community.length > 0
                  ? community.map((c, i) => <PlayingCard key={i} {...c} />)
                  : <span className="text-green-800 text-xs">Waiting for cards…</span>}
              </div>

              {/* Pot */}
              {pot > 0 && (
                <div className="bg-black bg-opacity-40 rounded-full px-3 py-0.5">
                  <span className="text-yellow-300 text-sm font-bold">Pot: {pot}</span>
                </div>
              )}
            </div>

            {/* Empty seat markers & seated players before game start */}
            {!hasGameState && (table?.seats ?? []).length > 0
              ? // Before game starts: show seated players from table.seats + empty markers
                Array.from({ length: maxPlayers }).map((_, i) => {
                  const seat = (table?.seats ?? []).find((s) => s.seatIndex === i)
                  const pos = getSeatPos(i, maxPlayers)
                  const isMe = seat?.username === username
                  return (
                    <div
                      key={`seat-${i}`}
                      className="absolute"
                      style={{ left: pos.left, top: pos.top, transform: 'translate(-50%,-50%)', zIndex: 10 }}
                    >
                      {seat ? (
                        <div className={`flex flex-col items-center gap-1`}>
                          <div className={`px-2 py-1 rounded-lg text-center text-xs shadow-lg border min-w-[76px] ${
                            isMe
                              ? 'bg-blue-700 border-blue-400 text-white'
                              : 'bg-gray-800 border-gray-600 text-gray-200'
                          }`}>
                            <div className="font-semibold truncate max-w-[76px]">{seat.username}</div>
                            <div className="opacity-80">{seat.chips} chips</div>
                          </div>
                        </div>
                      ) : (
                        <div className="w-16 h-7 border border-dashed border-gray-700 rounded-lg flex items-center justify-center">
                          <span className="text-gray-700 text-xs">empty</span>
                        </div>
                      )}
                    </div>
                  )
                })
              : // During game: show empty markers + player seats from gameState
                <>
                  {Array.from({ length: maxPlayers }).map((_, i) => {
                    if (players.some((p) => p.seatIndex === i)) return null
                    const pos = getSeatPos(i, maxPlayers)
                    return (
                      <div
                        key={`empty-${i}`}
                        className="absolute"
                        style={{ left: pos.left, top: pos.top, transform: 'translate(-50%,-50%)' }}
                      >
                        <div className="w-16 h-7 border border-dashed border-gray-700 rounded-lg flex items-center justify-center">
                          <span className="text-gray-700 text-xs">empty</span>
                        </div>
                      </div>
                    )
                  })}
                  {players.map((player) => (
                    <PlayerSeat
                      key={player.id}
                      player={player}
                      isMe={player.seatIndex === mySeatIdx}
                      isDealer={player.seatIndex === dealerIdx && phase !== 'WAITING'}
                      isActive={player.seatIndex === currentIdx && BETTING_PHASES.has(phase)}
                      maxPlayers={maxPlayers}
                    />
                  ))}
                </>
            }
          </div>

          {/* Error */}
          {error && <p className="text-red-400 text-sm">{error}</p>}

          {/* Action panel */}
          <div className="w-full max-w-lg">
            {canStartHand && (
              <div className="flex justify-center">
                <button
                  onClick={handleStartHand}
                  disabled={submitting}
                  className="bg-green-600 hover:bg-green-700 disabled:opacity-50 px-10 py-3 rounded-xl font-semibold text-lg shadow-lg transition"
                >
                  {submitting ? 'Starting…' : '▶ Start Hand'}
                </button>
              </div>
            )}

            {actions?.yourTurn && myPlayer?.status !== 'ALL_IN' && (
              <div className="bg-gray-800 rounded-xl p-4 space-y-3 shadow-lg">
                <div className="flex items-center justify-between">
                  <p className="text-sm font-semibold text-yellow-400">Your turn</p>
                  <span className={`text-sm font-bold tabular-nums ${
                    timeLeft <= 5 ? 'text-red-400 animate-pulse' : 'text-gray-300'
                  }`}>
                    ⏱ {timeLeft}s
                  </span>
                </div>

                {/* Raise slider */}
                {raiseAction && (
                  <div className="space-y-1">
                    <div className="flex justify-between text-xs text-gray-400">
                      <span>Raise to: <span className="text-white font-semibold">{raiseAmount}</span></span>
                      <span>All-in: {myChips}</span>
                    </div>
                    <input
                      type="range"
                      min={raiseAction.amount}
                      max={myChips}
                      value={raiseAmount}
                      onChange={(e) => setRaiseAmount(Number(e.target.value))}
                      className="w-full accent-green-500"
                    />
                  </div>
                )}

                {/* Buttons */}
                <div className="flex gap-2 justify-center flex-wrap">
                  {actions.actions.map((a) => (
                    <button
                      key={a.type}
                      disabled={submitting}
                      onClick={() =>
                        handleAction(a.type, a.type === 'RAISE' ? raiseAmount : a.amount)
                      }
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
                  onClick={() => setSittingOut(false)}
                  className="bg-yellow-600 hover:bg-yellow-700 px-6 py-2 rounded-lg font-semibold text-sm transition"
                >
                  🪑 I'm back
                </button>
              </div>
            )}

            {!actions?.yourTurn && !sittingOut && BETTING_PHASES.has(phase) && myPlayer && (
              <p className="text-center text-gray-600 text-sm">Waiting for opponent…</p>
            )}

            {!isSeated && phase !== 'WAITING' && (
              <p className="text-center text-gray-600 text-sm">You are spectating</p>
            )}
          </div>
        </div>

        {/* ── Events sidebar ── */}
        <div className="w-52 bg-gray-900 border-l border-gray-800 flex flex-col text-xs">
          <div className="px-3 py-2 border-b border-gray-800">
            <span className="font-semibold text-gray-500 uppercase tracking-wide">Events</span>
          </div>
          <div className="flex-1 overflow-y-auto p-2 space-y-1">
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
    </div>
  )
}
