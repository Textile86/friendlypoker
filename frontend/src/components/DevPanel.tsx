import { useState } from 'react'
import { useLog, LogEntry } from '../context/LogContext'

const METHOD_COLOR: Record<string, string> = {
  GET: 'bg-blue-700 text-blue-200',
  POST: 'bg-green-700 text-green-200',
  PUT: 'bg-yellow-700 text-yellow-200',
  PATCH: 'bg-yellow-700 text-yellow-200',
  DELETE: 'bg-red-800 text-red-200',
}

function statusColor(status?: number, isError?: boolean) {
  if (isError && !status) return 'text-red-400'
  if (!status) return 'text-gray-500'
  if (status < 300) return 'text-green-400'
  if (status < 400) return 'text-yellow-400'
  return 'text-red-400'
}

function shortUrl(url: string) {
  return url.replace('/api', '').substring(0, 48)
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
        <span className={`shrink-0 font-bold ${statusColor(entry.status, entry.isError)}`}>
          {entry.status ?? (entry.isError ? 'ERR' : '…')}
        </span>
        <span className="text-gray-600 shrink-0 w-12 text-right">{entry.duration}ms</span>
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
              <p className={`text-[10px] mb-0.5 ${entry.isError ? 'text-red-400' : 'text-gray-500'}`}>RESPONSE</p>
              <pre className={`rounded p-1.5 text-[10px] overflow-x-auto whitespace-pre-wrap break-all ${entry.isError ? 'bg-red-900 text-red-200' : 'bg-gray-900 text-gray-300'}`}>
                {formatBody(entry.responseBody)}
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

export default function DevPanel() {
  const { entries, clear } = useLog()
  const [open, setOpen] = useState(false)
  const [errorsOnly, setErrorsOnly] = useState(false)

  const visible = errorsOnly ? entries.filter((e) => e.isError) : entries
  const errorCount = entries.filter((e) => e.isError).length

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
        {open ? '▼ Log' : '▲ Log'}{errorCount > 0 ? ` (${errorCount} err)` : ` (${entries.length})`}
      </button>

      {/* Panel */}
      {open && (
        <div className="w-[560px] h-80 bg-gray-950 border border-gray-700 rounded-tl-lg flex flex-col shadow-2xl">
          {/* Header */}
          <div className="flex items-center gap-2 px-3 py-1.5 border-b border-gray-800 shrink-0">
            <span className="text-gray-400 text-xs font-semibold">Dev Log</span>
            <button
              onClick={() => setErrorsOnly((v) => !v)}
              className={`text-[10px] px-2 py-0.5 rounded ml-auto transition ${
                errorsOnly ? 'bg-red-800 text-red-200' : 'bg-gray-800 text-gray-400 hover:text-white'
              }`}
            >
              {errorsOnly ? 'Errors only' : 'All requests'}
            </button>
            <button
              onClick={clear}
              className="text-[10px] px-2 py-0.5 rounded bg-gray-800 text-gray-400 hover:text-white transition"
            >
              Clear
            </button>
          </div>

          {/* Entries */}
          <div className="flex-1 overflow-y-auto">
            {visible.length === 0 ? (
              <p className="text-gray-700 text-xs text-center mt-6">No requests yet</p>
            ) : (
              visible.map((e) => <EntryRow key={e.id} entry={e} />)
            )}
          </div>
        </div>
      )}
    </div>
  )
}
