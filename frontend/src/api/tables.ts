import api from './client'

export interface SeatInfo {
  seatIndex: number
  username: string
  chips: number
}

export interface TableResponse {
  id: number
  name: string
  smallBlind: number
  bigBlind: number
  minPlayers: number
  maxPlayers: number
  startingChips: number
  actionTimeoutSecs: number
  variant: string
  status: string
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

export const getClubTables = (clubId: number) =>
  api.get<TableResponse[]>(`/clubs/${clubId}/tables`).then((r) => r.data)

export const createTable = (clubId: number, data: CreateTableRequest) =>
  api.post<TableResponse>(`/clubs/${clubId}/tables`, data).then((r) => r.data)

export const sitDown = (tableId: number) =>
  api.post<TableResponse>(`/tables/${tableId}/sit`).then((r) => r.data)

export const standUp = (tableId: number) =>
  api.delete(`/tables/${tableId}/sit`)
