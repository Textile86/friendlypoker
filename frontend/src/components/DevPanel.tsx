import { useState } from 'react'
import { useLog, LogEntry } from '../context/LogContext'

const METHOD_COLOR: Record<string, string> = {
  GET: 'bg-blue-700 text-blue-200',
  POST: 'bg-green-700 text-green-200',
  PUT: 'bg-yellow-700 text-yellow-200',
  PATCH: 'bg-yellow-700 text-yellow-200',
  DELETE: 'bg-red-800 text-red-200',
  WS: 'bg-purple-700 text-purple-200',
}

type Filter = 'all' | 'http' | 'ws' | 'errors'

function statusColor(status?: number, isError?: boolean) {
  if (isError && !status) return 'text-red-400'
  if (!status) return 'text-gray-500'
  if (status < 300) return 'text-green-400'
  if (status < 400) return 'text-yellow-400'
  return 'text-red-400'
}

function shortUrl(url: string) {
  return url.replace('/api', '').substring(0, 52)
}

function formatBody(raw?: string) {
  if (!raw) return null
  try {
    return JSON.stringify(JSON.parse(raw), null, 2)
  } catch {
    return raw
  }
}

function EntryRow({ entry }: { entry: LogEntry }) {
  const [open, setOpen] = useState(entry.isError)
  const isWs = entry.method === 'WS'

  return (
    <div
      className={`border-b border-gray-800 text-xs cursor-pointer select-none ${entry.isError ? 'bg-red-950' : ''}`}
      onClick={() => setOpen((v) => !v)}
    >
      {/* Summary row */}
      <div className="flex items-center gap-2 px-2 py-1.5">
        <span className="text-gray-600 w-14 shrink-0">{entry.time}</span>
        <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold shrink-0 ${METHOD_COLOR[entry.method] ?? 'bg-gray-700 text-gray-300'}`}>
          {entry.method}
        </span>
        <span className="text-gray-300 flex-1 truncate font-mono">{shortUrl(entry.url)}</span>
        {isWs ? (
          <span className="text-purple-500 shrink-0 text-[10px]">
            {entry.isError ? 'DISC' : '●'}
          </span>
        ) : (
          <span className={`shrink-0 font-bold ${statusColor(entry.status, entry.isError)}`}>
            {entry.status ?? (entry.isError ? 'ERR' : '…')}
          </span>
        )}
        {!isWs && (
          <span className="text-gray-600 shrink-0 w-12 text-right">{entry.duration}ms</span>
        )}
      </div>

      {/* Expandable detail */}
      {open && (
        <div className="px-2 pb-2 space-y-1.5">
          {entry.requestBody && (
            <div>
              <p className="text-gray-500 text-[10px] mb-0.5">REQUEST</p>
              <pre className="bg-gray-900 rounded p-1.5 text-gray-300 text-[10px] overflow-x-auto whitespace-pre-wrap break-all">
                {formatBody(entry.requestBody)}
              </pre>
            </div>
          )}
          {entry.responseBody && (
            <div>
              <p className={`text-[10px] mb-0.5 ${entry.isError ? 'text-red-400' : isWs ? 'text-purple-400' : 'text-gray-500'}`}>
                {isWs ? 'WS MESSAGE' : 'RESPONSE'}
              </p>
              <pre className={`rounded p-1.5 text-[10px] overflow-x-auto whitespace-pre-wrap break-all ${
                entry.isError ? 'bg-red-900 text-red-200' : isWs ? 'bg-purple-950 text-purple-200' : 'bg-gray-900 text-gray-300'
              }`}>
                {isWs ? entry.responseBody : formatBody(entry.responseBody)}
              </pre>
            </div>
          )}
          {!entry.requestBody && !entry.responseBody && (
            <p className="text-gray-600 text-[10px]">No body</p>
          )}
        </div>
      )}
    </div>
  )
}

const FILTER_LABELS: Record<Filter, string> = {
  all: 'All',
  http: 'HTTP',
  ws: 'WS',
  errors: 'Errors',
}

export default function DevPanel() {
  const { entries, clear } = useLog()
  const [open, setOpen] = useState(false)
  const [filter, setFilter] = useState<Filter>('all')

  const errorCount = entries.filter((e) => e.isError).length
  const wsCount = entries.filter((e) => e.method === 'WS').length

  const visible = entries.filter((e) => {
    if (filter === 'errors') return e.isError
    if (filter === 'ws') return e.method === 'WS'
    if (filter === 'http') return e.method !== 'WS'
    return true
  })

  return (
    <div className="fixed bottom-0 right-0 z-50 flex flex-col items-end">
      {/* Toggle button */}
      <button
        onClick={() => setOpen((v) => !v)}
        className={`mb-1 mr-2 px-3 py-1.5 rounded-t-lg text-xs font-semibold shadow-lg border border-gray-700 transition ${
          errorCount > 0
            ? 'bg-red-900 text-red-300 border-red-700'
            : 'bg-gray-800 text-gray-400 hover:text-white'
        }`}
      >
        {open ? '▼' : '▲'} Log
        {errorCount > 0 && <span className="ml-1 text-red-400">{errorCount} err</span>}
        {wsCount > 0 && <span className="ml-1 text-purple-400">· {wsCount} ws</span>}
      </button>

      {/* Panel */}
      {open && (
        <div className="w-[600px] h-96 bg-gray-950 border border-gray-700 rounded-tl-lg flex flex-col shadow-2xl">
          {/* Header */}
          <div className="flex items-center gap-1.5 px-3 py-1.5 border-b border-gray-800 shrink-0">
            <span className="text-gray-400 text-xs font-semibold mr-1">Dev Log</span>
            {(['all', 'http', 'ws', 'errors'] as Filter[]).map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`text-[10px] px-2 py-0.5 rounded transition ${
                  filter === f
                    ? f === 'errors' ? 'bg-red-800 text-red-200'
                      : f === 'ws' ? 'bg-purple-800 text-purple-200'
                      : 'bg-blue-800 text-blue-200'
                    : 'bg-gray-800 text-gray-400 hover:text-white'
                }`}
              >
                {FILTER_LABELS[f]}
              </button>
            ))}
            <button
              onClick={clear}
              className="text-[10px] px-2 py-0.5 rounded bg-gray-800 text-gray-400 hover:text-white transition ml-auto"
            >
              Clear
            </button>
          </div>

          {/* Entries */}
          <div className="flex-1 overflow-y-auto">
            {visible.length === 0 ? (
              <p className="text-gray-700 text-xs text-center mt-6">No entries</p>
            ) : (
              visible.map((e) => <EntryRow key={e.id} entry={e} />)
            )}
          </div>
        </div>
      )}
    </div>
  )
}
