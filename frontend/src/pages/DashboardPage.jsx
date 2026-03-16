import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { api } from '../api'
import { ListingPreview } from '../components/ListingPreview'

export function DashboardPage({ user, communities }) {
  const [discover, setDiscover] = useState([])
  const [listings, setListings] = useState([])
  const [inviteToken, setInviteToken] = useState('')
  const [error, setError] = useState('')
  const location = useLocation()

  const searchQuery = useMemo(() => {
    const params = new URLSearchParams(location.search)
    return params.get('q') || ''
  }, [location.search])

  const joinedCommunityIds = new Set(communities.map(community => String(community.communityId)))

  useEffect(() => {
    api('/communities/discover').then(setDiscover).catch(() => {})
  }, [])

  useEffect(() => {
    if (!user) return
    const endpoint = searchQuery ? `/listings?query=${encodeURIComponent(searchQuery)}` : '/listings'
    api(endpoint)
      .then(setListings)
      .catch(err => setError(err.message))
  }, [user, searchQuery])

  async function joinByDomain(communityId) {
    try {
      await api(`/communities/${communityId}/join-by-domain`, { method: 'POST' })
      window.location.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  async function requestMembership(communityId) {
    try {
      await api(`/communities/${communityId}/request`, { method: 'POST' })
      setError('Membership request sent.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function joinByInvite(event) {
    event.preventDefault()
    try {
      await api('/communities/join-by-invite', {
        method: 'POST',
        body: JSON.stringify({ token: inviteToken })
      })
      window.location.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  if (!user) {
    return (
      <section className="panel">
        <p className="eyebrow">Marketplace preview</p>
        <h1>Private community marketplace</h1>
        <p>Browse the directory, then sign in to unlock your community feed.</p>
        <div className="button-row wrap-row">
          <Link className="button-link dark" to="/communities">Browse communities</Link>
          <Link className="button-link ghost-link" to="/auth">Sign in</Link>
        </div>
      </section>
    )
  }

  return (
    <div className="social-layout">
      <section className="feed-column">
        <div className="feed-intro panel">
          <div>
            <p className="eyebrow">Marketplace feed</p>
            <h2>{searchQuery ? `Results for "${searchQuery}"` : 'Latest posts from your communities'}</h2>
            <p className="feed-intro-copy">
              Scan fresh listings, jump into trusted spaces, and post quickly when you are ready to sell.
            </p>
          </div>
          <div className="feed-intro-stats">
            <div className="stat-pill">
              <strong>{communities.length}</strong>
              <span>Joined communities</span>
            </div>
            <div className="stat-pill">
              <strong>{listings.length}</strong>
              <span>Visible listings</span>
            </div>
          </div>
        </div>

        <div className="feed-header">
          <div>
            <p className="eyebrow">For you</p>
            <h2>Fresh activity</h2>
          </div>
          <Link className="button-link dark" to="/sell">Create post</Link>
        </div>

        {error && <p className="error">{error}</p>}

        <div className="feed-scroll">
          {listings.map(listing => (
            <article key={listing.id} className="listing-card feed-card">
              <ListingPreview listing={listing} />
              <div className="feed-card-body">
                <div className="feed-card-header">
                  <div>
                    <h3>{listing.title}</h3>
                    <p className="feed-meta">{listing.sellerName} • {listing.category} • {listing.itemCondition}</p>
                  </div>
                  <p className="price">${listing.price}</p>
                </div>
                <p>{listing.description}</p>
              </div>
            </article>
          ))}
          {listings.length === 0 && (
            <article className="panel">
              <p>No visible listings yet. Join a community or create the first post.</p>
            </article>
          )}
        </div>
      </section>

      <aside className="panel sidebar-panel sticky-panel">
        <div className="sidebar-section">
          <p className="eyebrow">Your spaces</p>
          <h2>Communities</h2>
        </div>
        <div className="stack">
          {communities.length === 0 && <p>No active memberships yet.</p>}
          {communities.map(community => (
            <Link key={community.communityId} className="community-link" to={`/communities/${community.communityId}`}>
              <strong>{community.name}</strong>
              <span>{community.role}</span>
            </Link>
          ))}
        </div>

        <div className="sidebar-section">
          <p className="eyebrow">Discover</p>
          <h2>Communities</h2>
        </div>

        <form className="stack invite-stack" onSubmit={joinByInvite}>
          <input value={inviteToken} onChange={event => setInviteToken(event.target.value)} placeholder="Paste invite token" />
          <button type="submit">Join with invite</button>
        </form>

        <div className="stack">
          {discover.map(community => (
            <article key={community.id} className="community-card compact-card">
              <div className="community-card-top">
                <span className="badge neutral">{community.privateCommunity ? 'Closed' : 'Open'}</span>
                {joinedCommunityIds.has(String(community.id)) && <span className="badge success">Joined</span>}
              </div>
              <h3>{community.name}</h3>
              <p>{community.description}</p>
              <div className="button-row wrap-row">
                <Link className="button-link dark" to={`/communities/${community.id}`}>View</Link>
                {!joinedCommunityIds.has(String(community.id)) && (
                  <>
                    <button onClick={() => joinByDomain(community.id)}>Join by email</button>
                    <button className="ghost" onClick={() => requestMembership(community.id)}>Request access</button>
                  </>
                )}
              </div>
            </article>
          ))}
        </div>
      </aside>
    </div>
  )
}
