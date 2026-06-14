import api from './client'

export interface SeatInfo {
  seatIndex: number
  username: string
  chips: number
}

export interface TableResponse {
  id: number
  // clubId and myRole appear after the backend TableResponse update;
  // optional so the UI works against the old backend too
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

export const getClubTables = (clubId: number) =>
  api.get<TableResponse[]>(`/clubs/${clubId}/tables`).then((r) => r.data)

export const createTable = (clubId: number, data: CreateTableRequest) =>
  api.post<TableResponse>(`/clubs/${clubId}/tables`, data).then((r) => r.data)

export const sitDown = (tableId: number) =>
  api.post<TableResponse>(`/tables/${tableId}/sit`).then((r) => r.data)

export const standUp = (tableId: number) =>
  api.delete(`/tables/${tableId}/sit`)

export const getTable = (tableId: number) =>
  api.get<TableResponse>(`/tables/${tableId}`).then((r) => r.data)

export const closeTable = (tableId: number) =>
  api.post(`/tables/${tableId}/close`)

export const sitOut = (tableId: number) =>
  api.post(`/tables/${tableId}/sit-out`)

export const imBack = (tableId: number) =>
  api.post(`/tables/${tableId}/im-back`)

export const showCards = (tableId: number) =>
  api.post(`/tables/${tableId}/show-cards`)
