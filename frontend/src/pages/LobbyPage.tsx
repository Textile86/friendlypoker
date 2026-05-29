import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { getMyClubs, createClub, joinByInvite, ClubResponse } from '../api/clubs'

export default function LobbyPage() {
  const [clubs, setClubs] = useState<ClubResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [newName, setNewName] = useState('')
  const [newDesc, setNewDesc] = useState('')
  const [creating, setCreating] = useState(false)

  const [inviteToken, setInviteToken] = useState('')
  const [joining, setJoining] = useState(false)
  const [joinError, setJoinError] = useState('')

  const navigate = useNavigate()

  useEffect(() => {
    getMyClubs()
      .then(setClubs)
      .catch(() => setError('Failed to load clubs'))
      .finally(() => setLoading(false))
  }, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    setCreating(true)
    try {
      const club = await createClub({ name: newName, description: newDesc })
      setClubs((prev) => [...prev, club])
      setShowCreate(false)
      setNewName('')
      setNewDesc('')
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Failed to create club')
    } finally {
      setCreating(false)
    }
  }

  async function handleJoin(e: React.FormEvent) {
    e.preventDefault()
    setJoining(true)
    setJoinError('')
    try {
      const club = await joinByInvite(inviteToken.trim())
      setClubs((prev) => {
        if (prev.find((c) => c.id === club.id)) return prev
        return [...prev, club]
      })
      setInviteToken('')
    } catch (err: any) {
      setJoinError(err.response?.data?.error ?? 'Invalid invite')
    } finally {
      setJoining(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <Navbar />
      <main className="max-w-4xl mx-auto p-6 space-y-8">

        {/* My clubs */}
        <section>
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xl font-semibold">My Clubs</h2>
            <button
              onClick={() => setShowCreate(!showCreate)}
              className="bg-blue-600 hover:bg-blue-700 text-sm px-4 py-1.5 rounded-lg transition"
            >
              + New Club
            </button>
          </div>

          {showCreate && (
            <form onSubmit={handleCreate} className="bg-gray-800 rounded-xl p-4 mb-4 space-y-3">
              <input
                className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Club name"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                required
              />
              <input
                className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                placeholder="Description (optional)"
                value={newDesc}
                onChange={(e) => setNewDesc(e.target.value)}
              />
              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={creating}
                  className="bg-green-600 hover:bg-green-700 disabled:opacity-50 px-4 py-1.5 rounded-lg text-sm transition"
                >
                  {creating ? 'Creating...' : 'Create'}
                </button>
                <button
                  type="button"
                  onClick={() => setShowCreate(false)}
                  className="bg-gray-600 hover:bg-gray-500 px-4 py-1.5 rounded-lg text-sm transition"
                >
                  Cancel
                </button>
              </div>
            </form>
          )}

          {error && <p className="text-red-400 text-sm mb-2">{error}</p>}

          {loading ? (
            <p className="text-gray-400">Loading...</p>
          ) : clubs.length === 0 ? (
            <p className="text-gray-400">You are not in any clubs yet.</p>
          ) : (
            <div className="grid gap-3">
              {clubs.map((club) => (
                <div
                  key={club.id}
                  onClick={() => navigate(`/clubs/${club.id}`)}
                  className="bg-gray-800 rounded-xl p-4 cursor-pointer hover:bg-gray-700 transition flex justify-between items-center"
                >
                  <div>
                    <p className="font-semibold">{club.name}</p>
                    {club.description && (
                      <p className="text-gray-400 text-sm">{club.description}</p>
                    )}
                  </div>
                  <span className="text-gray-500 text-xs">Owner: {club.ownerUsername}</span>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Join by invite */}
        <section>
          <h2 className="text-xl font-semibold mb-4">Join by Invite</h2>
          <form onSubmit={handleJoin} className="flex gap-2">
            <input
              className="flex-1 bg-gray-800 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="Paste invite code..."
              value={inviteToken}
              onChange={(e) => setInviteToken(e.target.value)}
            />
            <button
              type="submit"
              disabled={joining || !inviteToken.trim()}
              className="bg-blue-600 hover:bg-blue-700 disabled:opacity-50 px-4 py-2 rounded-lg text-sm transition"
            >
              {joining ? 'Joining...' : 'Join'}
            </button>
          </form>
          {joinError && <p className="text-red-400 text-sm mt-1">{joinError}</p>}
        </section>

      </main>
    </div>
  )
}
