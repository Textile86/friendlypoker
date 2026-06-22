import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { useAuth } from '../context/AuthContext'
import { getMyClubs, createInvite, ClubResponse } from '../api/clubs'
import { getClubTables, createTable, TableResponse } from '../api/tables'

export default function ClubPage() {
  const { id } = useParams<{ id: string }>()
  const clubId = Number(id)
  const { username } = useAuth()
  const navigate = useNavigate()

  const [club, setClub] = useState<ClubResponse | null>(null)
  const [tables, setTables] = useState<TableResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [showCreate, setShowCreate] = useState(false)
  const [form, setForm] = useState({
    name: '',
    smallBlind: 5,
    bigBlind: 10,
    maxPlayers: 6,
    startingChips: 1000,
    actionTimeoutSecs: 30,
    rebuyMin: 0,
    rebuyMax: 0,
    rebuyCountMin: 0,
    rebuyCountMax: 10,
    rebuyUnlimited: false,
  })
  const [creating, setCreating] = useState(false)

  const [inviteLink, setInviteLink] = useState('')
  const [copyLabel, setCopyLabel] = useState('Copy')

  useEffect(() => {
    Promise.all([
      getMyClubs().then((clubs) => clubs.find((c) => c.id === clubId) ?? null),
      getClubTables(clubId),
    ])
      .then(([c, t]) => { setClub(c); setTables(t) })
      .catch(() => setError('Failed to load club data'))
      .finally(() => setLoading(false))
  }, [clubId])

  async function handleCreateTable(e: React.FormEvent) {
    e.preventDefault()
    setCreating(true)
    try {
      const table = await createTable(clubId, form)
      setTables((prev) => [...prev, table])
      setShowCreate(false)
    } catch (err: any) {
      setError(err.response?.data?.error ?? 'Failed to create table')
    } finally {
      setCreating(false)
    }
  }

  async function handleGenerateInvite() {
    try {
      const { token } = await createInvite(clubId)
      setInviteLink(token)
    } catch {
      setError('Failed to generate invite')
    }
  }

  async function handleCopy() {
    await navigator.clipboard.writeText(inviteLink)
    setCopyLabel('Copied!')
    setTimeout(() => setCopyLabel('Copy'), 2000)
  }


  if (loading) {
    return (
      <div className="min-h-screen bg-gray-900 text-white">
        <Navbar />
        <p className="p-6 text-gray-400">Loading...</p>
      </div>
    )
  }

  const isOwner = club?.ownerUsername === username

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <Navbar />
      <main className="max-w-4xl mx-auto p-6 space-y-8">

        {/* Club header */}
        <section className="flex justify-between items-start">
          <div>
            <h2 className="text-2xl font-bold">{club?.name ?? 'Club'}</h2>
            {club?.description && (
              <p className="text-gray-400 mt-1">{club.description}</p>
            )}
          </div>
          <button
            onClick={handleGenerateInvite}
            className="bg-gray-700 hover:bg-gray-600 text-sm px-4 py-1.5 rounded-lg transition"
          >
            Generate Invite
          </button>
        </section>

        {/* Invite link */}
        {inviteLink && (
          <section className="bg-gray-800 rounded-xl p-4 flex gap-2 items-center">
            <input
              readOnly
              value={inviteLink}
              className="flex-1 bg-gray-700 text-white text-sm rounded-lg px-3 py-2 focus:outline-none"
            />
            <button
              onClick={handleCopy}
              className="bg-blue-600 hover:bg-blue-700 text-sm px-4 py-2 rounded-lg transition"
            >
              {copyLabel}
            </button>
          </section>
        )}

        {error && <p className="text-red-400 text-sm">{error}</p>}

        {/* Tables */}
        <section>
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-xl font-semibold">Tables</h3>
            {isOwner && (
              <button
                onClick={() => setShowCreate(!showCreate)}
                className="bg-blue-600 hover:bg-blue-700 text-sm px-4 py-1.5 rounded-lg transition"
              >
                + New Table
              </button>
            )}
          </div>

          {showCreate && (
            <form onSubmit={handleCreateTable} className="bg-gray-800 rounded-xl p-4 mb-4 space-y-3">
              <div className="grid grid-cols-2 gap-3">
                <div className="col-span-2">
                  <label className="block text-xs text-gray-400 mb-1">Table name</label>
                  <input
                    className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    required
                  />
                </div>
                {[
                  { label: 'Small Blind', key: 'smallBlind' },
                  { label: 'Big Blind', key: 'bigBlind' },
                  { label: 'Max Players', key: 'maxPlayers' },
                  { label: 'Starting Chips', key: 'startingChips' },
                  { label: 'Action Timeout (sec)', key: 'actionTimeoutSecs' },
                ].map(({ label, key }) => (
                  <div key={key}>
                    <label className="block text-xs text-gray-400 mb-1">{label}</label>
                    <input
                      type="number"
                      className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={(form as any)[key]}
                      onChange={(e) => setForm({ ...form, [key]: Number(e.target.value) })}
                      required
                    />
                  </div>
                ))}
              </div>

              {/* ── Rebuy Settings ── */}
              <div className="border-t border-gray-700 pt-3 mt-1">
                <p className="text-xs text-gray-500 uppercase tracking-wide mb-2">Rebuy Settings</p>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Min Rebuy (chips)</label>
                    <input
                      type="number"
                      min={0}
                      className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={form.rebuyMin}
                      onChange={(e) => setForm({ ...form, rebuyMin: Number(e.target.value) })}
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-gray-400 mb-1">Max Rebuy (chips)</label>
                    <input
                      type="number"
                      min={0}
                      className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                      value={form.rebuyMax}
                      onChange={(e) => setForm({ ...form, rebuyMax: Number(e.target.value) })}
                    />
                  </div>
                  {!form.rebuyUnlimited && (
                    <>
                      <div>
                        <label className="block text-xs text-gray-400 mb-1">Rebuy Count Min</label>
                        <input
                          type="number"
                          min={0}
                          className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                          value={form.rebuyCountMin}
                          onChange={(e) => setForm({ ...form, rebuyCountMin: Number(e.target.value) })}
                        />
                      </div>
                      <div>
                        <label className="block text-xs text-gray-400 mb-1">Rebuy Count Max</label>
                        <input
                          type="number"
                          min={0}
                          className="w-full bg-gray-700 text-white rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                          value={form.rebuyCountMax}
                          onChange={(e) => setForm({ ...form, rebuyCountMax: Number(e.target.value) })}
                        />
                      </div>
                    </>
                  )}
                  <div className="col-span-2 flex items-center gap-3">
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        name="rebuyLimit"
                        checked={!form.rebuyUnlimited}
                        onChange={() => setForm({ ...form, rebuyUnlimited: false })}
                        className="accent-blue-500"
                      />
                      <span className="text-xs text-gray-300">Limited</span>
                    </label>
                    <label className="flex items-center gap-2 cursor-pointer">
                      <input
                        type="radio"
                        name="rebuyLimit"
                        checked={form.rebuyUnlimited}
                        onChange={() => setForm({ ...form, rebuyUnlimited: true })}
                        className="accent-blue-500"
                      />
                      <span className="text-xs text-gray-300">Unlimited</span>
                    </label>
                  </div>
                </div>
              </div>

              <div className="flex gap-2">
                <button
                  type="submit"
                  disabled={creating}
                  className="bg-green-600 hover:bg-green-700 disabled:opacity-50 px-4 py-1.5 rounded-lg text-sm transition"
                >
                  {creating ? 'Creating...' : 'Create Table'}
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

          {tables.length === 0 ? (
            <p className="text-gray-400">No tables yet.</p>
          ) : (
            <div className="grid gap-3">
              {tables.map((table) => {
                const isSeated = table.seats.some((s) => s.username === username)

                return (
                  <div key={table.id} className="bg-gray-800 rounded-xl p-4">
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="font-semibold">{table.name}</p>
                        <p className="text-gray-400 text-sm">
                          {table.smallBlind}/{table.bigBlind} blinds · {table.seats.length}/{table.maxPlayers} players · {table.startingChips} chips
                        </p>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className={`text-xs px-2 py-0.5 rounded-full ${
                          table.status === 'WAITING' ? 'bg-green-800 text-green-300' :
                          table.status === 'ACTIVE' ? 'bg-blue-800 text-blue-300' :
                          'bg-gray-700 text-gray-300'
                        }`}>
                          {table.status}
                        </span>
                        {table.status !== 'CLOSED' && (
                          <button
                            onClick={() => navigate(`/game/${table.id}`)}
                            className={`text-sm px-3 py-1 rounded-lg transition font-semibold ${
                              isSeated
                                ? 'bg-blue-600 hover:bg-blue-500'
                                : 'bg-green-700 hover:bg-green-600'
                            }`}
                          >
                            {isSeated ? '▶ Play' : 'Join'}
                          </button>
                        )}
                      </div>
                    </div>

                    {table.seats.length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-2">
                        {table.seats.map((seat) => (
                          <span
                            key={seat.seatIndex}
                            className={`text-xs px-2 py-0.5 rounded-full ${
                              seat.username === username
                                ? 'bg-blue-700 text-blue-200'
                                : 'bg-gray-700 text-gray-300'
                            }`}
                          >
                            {seat.username} ({seat.chips})
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </section>
      </main>
    </div>
  )
}
