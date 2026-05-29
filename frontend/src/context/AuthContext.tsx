import { createContext, useContext, useState, ReactNode } from 'react'

interface AuthState {
  token: string | null
  username: string | null
}

interface AuthContextValue extends AuthState {
  signIn: (token: string, username: string) => void
  signOut: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({
    token: localStorage.getItem('token'),
    username: localStorage.getItem('username'),
  })

  const signIn = (token: string, username: string) => {
    localStorage.setItem('token', token)
    localStorage.setItem('username', username)
    setState({ token, username })
  }

  const signOut = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    setState({ token: null, username: null })
  }

  return (
    <AuthContext.Provider value={{ ...state, signIn, signOut }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider')
  return ctx
}
