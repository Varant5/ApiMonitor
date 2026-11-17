import { useState, type FormEvent } from 'react'
import { ApiError, api } from '../api'

export function AddMonitorForm({ onCreated }: { onCreated: () => void }) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [url, setUrl] = useState('https://')
  const [intervalSeconds, setIntervalSeconds] = useState(60)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (!open) {
    return (
      <button className="button button--primary" onClick={() => setOpen(true)}>
        + Add monitor
      </button>
    )
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await api.createMonitor({ name, url, checkIntervalSeconds: intervalSeconds })
      setName('')
      setUrl('https://')
      setIntervalSeconds(60)
      setOpen(false)
      onCreated()
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Could not reach the server.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card form form--inline" onSubmit={handleSubmit}>
      <h2 className="card__title">New monitor</h2>
      <div className="form__row">
        <label className="field">
          <span>Name</span>
          <input required value={name} onChange={(e) => setName(e.target.value)} placeholder="GitHub API" />
        </label>
        <label className="field field--wide">
          <span>URL</span>
          <input type="url" required value={url} onChange={(e) => setUrl(e.target.value)} />
        </label>
        <label className="field field--narrow">
          <span>Every (seconds)</span>
          <input
            type="number"
            min={10}
            required
            value={intervalSeconds}
            onChange={(e) => setIntervalSeconds(Number(e.target.value))}
          />
        </label>
      </div>
      {error && (
        <p className="alert" role="alert">
          {error}
        </p>
      )}
      <div className="form__actions">
        <button type="button" className="button" onClick={() => setOpen(false)}>
          Cancel
        </button>
        <button type="submit" className="button button--primary" disabled={busy}>
          {busy ? 'Saving…' : 'Create monitor'}
        </button>
      </div>
    </form>
  )
}
