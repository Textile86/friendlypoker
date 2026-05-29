import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Navbar() {
  const { username, signOut } = useAuth()
  const navigate = useNavigate()

  return (
    <nav className="bg-gray-800 px-6 py-4 flex justify-between items-center shadow">
      <button
        onClick={() => navigate('/')}
        className="font-bold text-lg text-white hover:text-blue-400 transition"
      >
        FriendlyPoker
      </button>
      <div className="flex items-center gap-4">
        <span className="text-gray-400 text-sm">{username}</span>
        <button
          onClick={() => { signOut(); navigate('/login') }}
          className="text-sm text-red-400 hover:text-red-300 transition"
        >
          Logout
        </button>
      </div>
    </nav>
  )
}
