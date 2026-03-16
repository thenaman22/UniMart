import { useState } from 'react'
import { api } from '../api'

export function AuthPage({ onLogin }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [code, setCode] = useState('')
  const [generatedCode, setGeneratedCode] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  async function signUp(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    try {
      const response = await api('/auth/sign-up', {
        method: 'POST',
        body: JSON.stringify({ email, displayName })
      })
      setGeneratedCode(response.code)
      setMessage('Account created. Use the code below to verify and sign in.')
      setMode('login')
    } catch (err) {
      setError(err.message)
    }
  }

  async function requestCode(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    try {
      const response = await api('/auth/request-code', {
        method: 'POST',
        body: JSON.stringify({ email })
      })
      setGeneratedCode(response.code)
      setMessage('Login code sent.')
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
        <div className="demo-accounts">
          <strong>Demo accounts</strong>
          <span>`admin@school.edu`, `ava@school.edu`, `noah@school.edu`</span>
          <span>`lead@makers.org`, `mia@makers.org`, `ethan@makers.org`, `sofia@makers.org`</span>
          <span>`liam@makers.org`, `grace@makers.org`, `jack@makers.org`</span>
        </div>
      </div>

      <div className="panel">
        <div className="button-row auth-toggle">
          <button type="button" className={mode === 'login' ? '' : 'ghost'} onClick={() => setMode('login')}>Login</button>
          <button type="button" className={mode === 'signup' ? '' : 'ghost'} onClick={() => setMode('signup')}>Sign up</button>
        </div>

        {mode === 'login' ? (
          <>
            <h2>Log in with your organization email</h2>
            <form onSubmit={requestCode} className="stack">
              <input value={email} onChange={event => setEmail(event.target.value)} placeholder="Email" />
              <button type="submit">Send magic code</button>
            </form>
          </>
        ) : (
          <>
            <h2>Create your account</h2>
            <form onSubmit={signUp} className="stack">
              <input value={displayName} onChange={event => setDisplayName(event.target.value)} placeholder="Display name" />
              <input value={email} onChange={event => setEmail(event.target.value)} placeholder="Email" />
              <button type="submit">Sign up and get code</button>
            </form>
          </>
        )}

        {generatedCode && (
          <div className="code-block">
            <strong>Dev demo code:</strong> {generatedCode}
          </div>
        )}

        {message && <p className="success">{message}</p>}

        <form onSubmit={verifyCode} className="stack">
          <input value={code} onChange={event => setCode(event.target.value)} placeholder="Enter code" />
          <button type="submit">Verify and enter</button>
        </form>

        {error && <p className="error">{error}</p>}
      </div>
    </section>
  )
}
