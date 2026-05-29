import api from './client'

export interface ClubResponse {
  id: number
  name: string
  description: string
  ownerUsername: string
  createdAt: string
}

export interface InviteResponse {
  token: string
}

export const getMyClubs = () =>
  api.get<ClubResponse[]>('/clubs/my').then((r) => r.data)

export const createClub = (data: { name: string; description: string }) =>
  api.post<ClubResponse>('/clubs', data).then((r) => r.data)

export const createInvite = (clubId: number) =>
  api.post<InviteResponse>(`/clubs/${clubId}/invite`).then((r) => r.data)

export const joinByInvite = (token: string) =>
  api.post<ClubResponse>(`/clubs/join/${token}`).then((r) => r.data)
