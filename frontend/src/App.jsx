import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { api } from './api'
import { AuthPage } from './pages/AuthPage'
import { DashboardPage } from './pages/DashboardPage'
import { CommunityPage } from './pages/CommunityPage'
import { CreateListingPage } from './pages/CreateListingPage'
import { ModerationPage } from './pages/ModerationPage'
import { CommunitiesPage } from './pages/CommunitiesPage'
import { ProfilePage } from './pages/ProfilePage'
import { ProfileEditPage } from './pages/ProfileEditPage'
import { UserProfilePage } from './pages/UserProfilePage'
import { EditListingPage } from './pages/EditListingPage'

function HomeIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M3 10.6L12 3l9 7.6v9.4a1 1 0 0 1-1 1h-5.5v-6h-5v6H4a1 1 0 0 1-1-1z" />
    </svg>
  )
}

function CommunitiesIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M6 10a3 3 0 1 1 0-6 3 3 0 0 1 0 6zm12 0a3 3 0 1 1 0-6 3 3 0 0 1 0 6zM6 12c2.8 0 5 2.2 5 5v2H1v-2c0-2.8 2.2-5 5-5zm12 0c2.8 0 5 2.2 5 5v2H13v-2c0-2.8 2.2-5 5-5zm-6-1a4 4 0 1 1 0-8 4 4 0 0 1 0 8zm0 2c3.3 0 6 2.7 6 6v1H6v-1c0-3.3 2.7-6 6-6z" />
    </svg>
  )
}

function SellIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M11 4h2v7h7v2h-7v7h-2v-7H4v-2h7z" />
    </svg>
  )
}

function ShieldIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 2l7 3v6c0 5-3.4 9.7-7 11-3.6-1.3-7-6-7-11V5zm0 4.2L8 7.8v3.1c0 3.3 1.9 6.6 4 7.9 2.1-1.3 4-4.6 4-7.9V7.8z" />
    </svg>
  )
}

function ProfileIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 12a4.5 4.5 0 1 1 0-9 4.5 4.5 0 0 1 0 9zm0 2c4.4 0 8 2.7 8 6v1H4v-1c0-3.3 3.6-6 8-6z" />
    </svg>
  )
}

function ThemeIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 3.5a1 1 0 0 1 1 1V6a1 1 0 1 1-2 0V4.5a1 1 0 0 1 1-1zm0 14a5.5 5.5 0 1 0 0-11 5.5 5.5 0 0 0 0 11zm7.5-6.5a1 1 0 0 1 1 1 1 1 0 0 1-1 1H18a1 1 0 1 1 0-2zM6 12a1 1 0 1 1 0 2H4.5a1 1 0 1 1 0-2zm10.95 4.54l1.06 1.06a1 1 0 0 1-1.42 1.42l-1.06-1.06a1 1 0 0 1 1.42-1.42zM7.47 7.47a1 1 0 0 1 0 1.41L6.4 9.95A1 1 0 0 1 4.98 8.53L6.05 7.47a1 1 0 0 1 1.42 0zm9.48-1.42l1.06 1.06a1 1 0 0 1-1.42 1.42L15.53 7.47a1 1 0 1 1 1.42-1.42zM7.47 16.53a1 1 0 0 1 1.41 0 1 1 0 0 1 0 1.42l-1.06 1.06A1 1 0 1 1 6.4 17.6z" />
    </svg>
  )
}

function LogoutIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M11 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h6v-2H5V5h6zm6.6 4.4L16.2 8.8 18.4 11H9v2h9.4l-2.2 2.2 1.4 1.4L22 12z" />
    </svg>
  )
}

function pageCopy(pathname) {
  if (pathname === '/communities') {
    return { eyebrow: 'Directory', title: 'Explore communities' }
  }
  if (pathname === '/sell') {
    return { eyebrow: 'Seller studio', title: 'Create a new listing' }
  }
  if (pathname === '/moderation') {
    return { eyebrow: 'Trust and safety', title: 'Moderation queue' }
  }
  if (pathname === '/profile') {
    return { eyebrow: 'Account', title: 'Your profile' }
  }
  if (pathname === '/profile/edit') {
    return { eyebrow: 'Account', title: 'Edit profile' }
  }
  if (pathname.startsWith('/users/')) {
    return { eyebrow: 'Seller', title: 'Seller profile' }
  }
  if (pathname.startsWith('/listings/') && pathname.endsWith('/edit')) {
    return { eyebrow: 'Seller studio', title: 'Edit listing' }
  }
  if (pathname.startsWith('/communities/')) {
    return { eyebrow: 'Community', title: 'Community marketplace' }
  }
  return { eyebrow: 'Marketplace', title: 'Community feed' }
}

export default function App() {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('unimart-user')
    return raw ? JSON.parse(raw) : null
  })
  const [communities, setCommunities] = useState([])
  const [searchText, setSearchText] = useState('')
  const [theme, setTheme] = useState(() => localStorage.getItem('unimart-theme') || 'light')
  const [sidebarExpanded, setSidebarExpanded] = useState(() => window.innerWidth <= 900)
  const collapseTimerRef = useRef(null)
  const navigate = useNavigate()
  const location = useLocation()

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

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('unimart-theme', theme)
  }, [theme])

  useEffect(() => {
    if (location.pathname !== '/') {
      setSearchText('')
      return
    }
    const params = new URLSearchParams(location.search)
    setSearchText(params.get('q') || '')
  }, [location.pathname, location.search])

  function onLogin(payload) {
    localStorage.setItem('unimart-token', payload.token)
    localStorage.setItem('unimart-user', JSON.stringify(payload.user))
    setUser(payload.user)
    navigate('/')
  }

  function onProfileUpdated(profile) {
    const nextUser = {
      id: profile.id,
      displayName: profile.displayName,
      email: profile.email,
      profileImageUrl: profile.profileImageUrl
    }
    localStorage.setItem('unimart-user', JSON.stringify(nextUser))
    setUser(nextUser)
  }

  function logout() {
    localStorage.removeItem('unimart-token')
    localStorage.removeItem('unimart-user')
    setUser(null)
    setCommunities([])
    navigate('/auth')
  }

  function submitSearch(event) {
    event.preventDefault()
    const params = new URLSearchParams()
    if (searchText.trim()) {
      params.set('q', searchText.trim())
    }
    navigate(params.toString() ? `/?${params}` : '/')
  }

  function toggleTheme() {
    setTheme(current => current === 'dark' ? 'light' : 'dark')
  }

  function clearSidebarTimer() {
    if (collapseTimerRef.current) {
      window.clearTimeout(collapseTimerRef.current)
      collapseTimerRef.current = null
    }
  }

  function expandSidebar() {
    clearSidebarTimer()
    setSidebarExpanded(true)
  }

  function queueSidebarCollapse() {
    clearSidebarTimer()
    setSidebarExpanded(false)
  }

  useEffect(() => {
    function syncSidebarMode() {
      if (window.innerWidth <= 900) {
        clearSidebarTimer()
        setSidebarExpanded(true)
      }
    }

    syncSidebarMode()
    window.addEventListener('resize', syncSidebarMode)
    return () => {
      clearSidebarTimer()
      window.removeEventListener('resize', syncSidebarMode)
    }
  }, [])

  const isAuthed = Boolean(user)
  const currentPage = pageCopy(location.pathname)
  const primaryNav = [
    { to: '/', label: 'Home', icon: HomeIcon, end: true },
    { to: '/communities', label: 'Communities', icon: CommunitiesIcon },
    { to: '/sell', label: 'Sell', icon: SellIcon },
    { to: '/moderation', label: 'Moderation', icon: ShieldIcon },
    { to: '/profile', label: 'Profile', icon: ProfileIcon }
  ]

  return (
    <div className={`app-shell ${isAuthed ? 'shell-signed-in' : 'shell-guest'}${isAuthed && !sidebarExpanded ? ' shell-sidebar-collapsed' : ''}`}>
      {isAuthed && (
        <aside
          className={`app-sidebar${sidebarExpanded ? ' expanded' : ' collapsed'}`}
          onMouseEnter={expandSidebar}
          onMouseLeave={queueSidebarCollapse}
          onFocus={expandSidebar}
          onBlur={queueSidebarCollapse}
        >
          <div className="sidebar-brand-block">
            <Link className="brand sidebar-brand" to="/">
              <span className="brand-mark">U</span>
              <span className="sidebar-copy">UniMart</span>
            </Link>
          </div>

          <nav className="sidebar-nav" aria-label="Primary navigation">
            {primaryNav.map(item => {
              const Icon = item.icon
              return (
                <NavLink
                  key={item.to}
                  className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
                  to={item.to}
                  end={item.end}
                >
                  <span className="sidebar-icon"><Icon /></span>
                  <span className="sidebar-copy">{item.label}</span>
                </NavLink>
              )
            })}
          </nav>

          <div className="sidebar-footer">
            <button className="sidebar-utility" onClick={toggleTheme}>
              <span className="sidebar-icon"><ThemeIcon /></span>
              <span className="sidebar-copy">{theme === 'dark' ? 'Light mode' : 'Dark mode'}</span>
            </button>
            <button className="sidebar-utility" onClick={logout}>
              <span className="sidebar-icon"><LogoutIcon /></span>
              <span className="sidebar-copy">Log out</span>
            </button>
          </div>
        </aside>
      )}

      <div className="app-main">
        <header className="topbar social-topbar">
          <div className="topbar-left">
            {isAuthed ? (
              <div className="page-title-block">
                <p className="eyebrow">{currentPage.eyebrow}</p>
                <h1>{currentPage.title}</h1>
              </div>
            ) : (
              <>
                <Link className="brand" to="/">UniMart</Link>
                <Link className="nav-pill" to="/communities">Communities</Link>
              </>
            )}
          </div>

          <div className="topbar-center">
            <form className="top-search" onSubmit={submitSearch}>
              <input
                value={searchText}
                onChange={event => setSearchText(event.target.value)}
                placeholder="Search listings, books, furniture, tech..."
              />
              <button type="submit">Search</button>
            </form>
          </div>

          <div className="topbar-right">
            {isAuthed ? (
              null
            ) : (
              <>
                <button className="ghost theme-toggle" onClick={toggleTheme}>
                  {theme === 'dark' ? 'Light' : 'Dark'}
                </button>
                <Link className="icon-link" to="/auth">Sign in</Link>
              </>
            )}
          </div>
        </header>

        <main className="page social-page">
          <Routes>
            <Route path="/auth" element={<AuthPage onLogin={onLogin} />} />
            <Route path="/" element={<DashboardPage user={user} communities={communities} />} />
            <Route path="/communities" element={<CommunitiesPage user={user} communities={communities} />} />
            <Route path="/communities/:communityId" element={<CommunityPage user={user} communities={communities} />} />
            <Route path="/profile" element={<ProfilePage user={user} />} />
            <Route path="/profile/edit" element={<ProfileEditPage user={user} onProfileUpdated={onProfileUpdated} />} />
            <Route path="/users/:userId" element={<UserProfilePage user={user} />} />
            <Route path="/sell" element={<CreateListingPage user={user} communities={communities} />} />
            <Route path="/listings/:listingId/edit" element={<EditListingPage user={user} communities={communities} />} />
            <Route path="/moderation" element={<ModerationPage user={user} communities={communities} />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}
