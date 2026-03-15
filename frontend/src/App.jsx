import { useEffect, useState } from 'react'
import { Link, Route, Routes, useNavigate } from 'react-router-dom'
import { api } from './api'
import { AuthPage } from './pages/AuthPage'
import { DashboardPage } from './pages/DashboardPage'
import { CommunityPage } from './pages/CommunityPage'
import { CreateListingPage } from './pages/CreateListingPage'
import { ModerationPage } from './pages/ModerationPage'

export default function App() {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('unimart-user')
    return raw ? JSON.parse(raw) : null
  })
  const [communities, setCommunities] = useState([])
  const navigate = useNavigate()

  useEffect(() => {
    if (!user) return
    api('/communities')
      .then(setCommunities)
      .catch(() => {
        localStorage.removeItem('unimart-token')
        localStorage.removeItem('unimart-user')
        setUser(null)
      })
  }, [user])

  function onLogin(payload) {
    localStorage.setItem('unimart-token', payload.token)
    localStorage.setItem('unimart-user', JSON.stringify(payload.user))
    setUser(payload.user)
    navigate('/')
  }

  function logout() {
    localStorage.removeItem('unimart-token')
    localStorage.removeItem('unimart-user')
    setUser(null)
    setCommunities([])
    navigate('/auth')
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link className="brand" to="/">UniMart</Link>
        <nav>
          {user && <Link to="/">Feed</Link>}
          {user && <Link to="/sell">Sell</Link>}
          {user && <Link to="/moderation">Moderation</Link>}
        </nav>
        <div className="topbar-user">
          {user ? (
            <>
              <span>{user.displayName}</span>
              <button onClick={logout}>Log out</button>
            </>
          ) : (
            <Link to="/auth">Sign in</Link>
          )}
        </div>
      </header>

      <main className="page">
        <Routes>
          <Route path="/auth" element={<AuthPage onLogin={onLogin} />} />
          <Route path="/" element={<DashboardPage user={user} communities={communities} />} />
          <Route path="/communities/:communityId" element={<CommunityPage user={user} communities={communities} />} />
          <Route path="/sell" element={<CreateListingPage user={user} communities={communities} />} />
          <Route path="/moderation" element={<ModerationPage user={user} communities={communities} />} />
        </Routes>
      </main>
    </div>
  )
}
