import { createContext, useContext, useState, useCallback, useEffect, ReactNode } from 'react'

export interface LogEntry {
  id: number
  time: string
  method: string
  url: string
  requestBody?: string
  status?: number
  responseBody?: string
  isError: boolean
  duration: number
}

interface LogContextValue {
  entries: LogEntry[]
  clear: () => void
}

const LogContext = createContext<LogContextValue | null>(null)

let nextId = 1

export function LogProvider({ children }: { children: ReactNode }) {
  const [entries, setEntries] = useState<LogEntry[]>([])

  useEffect(() => {
    const handler = (e: Event) => {
      const detail = (e as CustomEvent).detail as Omit<LogEntry, 'id'>
      setEntries((prev) => [{ ...detail, id: nextId++ }, ...prev].slice(0, 80))
    }
    window.addEventListener('fp:log', handler)
    return () => window.removeEventListener('fp:log', handler)
  }, [])

  const clear = useCallback(() => setEntries([]), [])

  return (
    <LogContext.Provider value={{ entries, clear }}>
      {children}
    </LogContext.Provider>
  )
}

export function useLog() {
  const ctx = useContext(LogContext)
  if (!ctx) throw new Error('useLog must be used inside LogProvider')
  return ctx
}
