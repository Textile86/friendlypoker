import api from './client'

export interface SeatInfo {
  seatIndex: number
  username: string
  chips: number
  totalBuyIn: number
}

export interface TableResponse {
  id: number
  clubId?: number
  name: string
  smallBlind: number
  bigBlind: number
  minPlayers: number
  maxPlayers: number
  startingChips: number
  actionTimeoutSecs: number
  variant: string
  status: string
  pausedUntil?: string | null
  myRole?: string
  seats: SeatInfo[]
}

export interface CreateTableRequest {
  name: string
  smallBlind: number
  bigBlind: number
  maxPlayers: number
  startingChips: number
  actionTimeoutSecs: number
}

export interface PlayerStats {
  username: string
  chips: number
  totalBuyIn: number
  net: number
}

export const getClubTables = (clubId: number) =>
  api.get<TableResponse[]>(`/clubs/${clubId}/tables`).then((r) => r.data)

export const createTable = (clubId: number, data: CreateTableRequest) =>
  api.post<TableResponse>(`/clubs/${clubId}/tables`, data).then((r) => r.data)

export const sitDown = (tableId: number, chips: number, seatIndex: number) =>
  api.post<TableResponse>(`/tables/${tableId}/sit`, { chips, seatIndex }).then((r) => r.data)

export const standUp = (tableId: number) =>
  api.delete(`/tables/${tableId}/sit`)

export const rebuy = (tableId: number, chips: number) =>
  api.post<TableResponse>(`/tables/${tableId}/rebuy`, { chips }).then((r) => r.data)

export const getTable = (tableId: number) =>
  api.get<TableResponse>(`/tables/${tableId}`).then((r) => r.data)

export const getTableStatistics = (tableId: number) =>
  api.get<{ players: PlayerStats[] }>(`/tables/${tableId}/statistics`).then((r) => r.data.players)

export const closeTable = (tableId: number) =>
  api.post(`/tables/${tableId}/close`)

export const pauseTable = (tableId: number, minutes: number) =>
  api.post<TableResponse>(`/tables/${tableId}/pause`, { minutes }).then((r) => r.data)

export const sitOut = (tableId: number) =>
  api.post(`/tables/${tableId}/sit-out`)

export const imBack = (tableId: number) =>
  api.post(`/tables/${tableId}/im-back`)

export const showCards = (tableId: number) =>
  api.post(`/tables/${tableId}/show-cards`)
