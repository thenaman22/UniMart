import { useState } from 'react'
import { api } from '../api'

export function AuthPage({ onLogin }) {
  const [email, setEmail] = useState('admin@school.edu')
  const [displayName, setDisplayName] = useState('Campus Admin')
  const [code, setCode] = useState('')
  const [generatedCode, setGeneratedCode] = useState('')
  const [error, setError] = useState('')

  async function requestCode(event) {
    event.preventDefault()
    setError('')
    try {
      const response = await api('/auth/request-code', {
        method: 'POST',
        body: JSON.stringify({ email, displayName })
      })
      setGeneratedCode(response.code)
    } catch (err) {
      setError(err.message)
    }
  }

  async function verifyCode(event) {
    event.preventDefault()
    setError('')
    try {
      const response = await api('/auth/verify', {
        method: 'POST',
        body: JSON.stringify({ email, code })
      })
      onLogin(response)
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="auth-layout">
      <div className="hero-card">
        <p className="eyebrow">Community-first marketplace</p>
        <h1>Buy and sell inside trusted communities.</h1>
        <p>Only approved members can see listings, search the feed, and post items for sale.</p>
      </div>

      <div className="panel">
        <h2>Sign in with your organization email</h2>
        <form onSubmit={requestCode} className="stack">
          <input value={displayName} onChange={event => setDisplayName(event.target.value)} placeholder="Display name" />
          <input value={email} onChange={event => setEmail(event.target.value)} placeholder="Email" />
          <button type="submit">Send magic code</button>
        </form>

        {generatedCode && (
          <div className="code-block">
            <strong>Dev demo code:</strong> {generatedCode}
          </div>
        )}

        <form onSubmit={verifyCode} className="stack">
          <input value={code} onChange={event => setCode(event.target.value)} placeholder="Enter code" />
          <button type="submit">Verify and enter</button>
        </form>

        {error && <p className="error">{error}</p>}
      </div>
    </section>
  )
}
