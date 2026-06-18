import api from './client'

export interface CardView {
  rank: string
  suit: string
}

export interface PlayerView {
  id: string
  displayName: string
  chips: number
  status: string
  currentBet: number
  seatIndex: number
  holeCards: CardView[]
}

export interface PotView {
  label: string
  amount: number
  sidePot: boolean
  index: number
}

export interface GameStateView {
  tableId: number
  handNumber: number
  phase: string
  players: PlayerView[]
  potTotal: number
  currentBet: number
  pots: PotView[]
  communityCards: CardView[]
  dealerIndex: number
  currentPlayerIndex: number
}

export interface ActionOption {
  type: string
  amount: number
  description: string
}

export interface AvailableActions {
  yourTurn: boolean
  currentPlayerId: string | null
  actions: ActionOption[]
}

export const getGameState = (tableId: number) =>
  api.get<GameStateView>(`/tables/${tableId}/state`).then((r) => r.data)

export const getAvailableActions = (tableId: number) =>
  api.get<AvailableActions>(`/tables/${tableId}/actions`).then((r) => r.data)

export const startHand = (tableId: number) =>
  api.post<GameStateView>(`/tables/${tableId}/start-hand`).then((r) => r.data)

export const submitAction = (tableId: number, type: string, amount: number) =>
  api.post<GameStateView>(`/tables/${tableId}/action`, { type, amount }).then((r) => r.data)
