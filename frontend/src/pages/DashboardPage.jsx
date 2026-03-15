import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'

export function DashboardPage({ user, communities }) {
  const [discover, setDiscover] = useState([])
  const [listings, setListings] = useState([])
  const [query, setQuery] = useState('')
  const [inviteToken, setInviteToken] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    api('/communities/discover').then(setDiscover).catch(() => {})
  }, [])

  useEffect(() => {
    if (!user) return
    api('/listings')
      .then(setListings)
      .catch(err => setError(err.message))
  }, [user])

  async function search(event) {
    event.preventDefault()
    try {
      const response = await api(`/listings?query=${encodeURIComponent(query)}`)
      setListings(response)
      setError('')
    } catch (err) {
      setError(err.message)
    }
  }

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
        <h1>Private community marketplace</h1>
        <p>Sign in to browse campus communities, search listings, and sell to trusted members.</p>
      </section>
    )
  }

  return (
    <div className="dashboard-grid">
      <section className="panel">
        <h2>Your communities</h2>
        <div className="stack">
          {communities.length === 0 && <p>No active memberships yet.</p>}
          {communities.map(community => (
            <Link key={community.communityId} className="community-link" to={`/communities/${community.communityId}`}>
              <strong>{community.name}</strong>
              <span>{community.role}</span>
            </Link>
          ))}
        </div>
      </section>

      <section className="panel wide">
        <div className="panel-header">
          <div>
            <p className="eyebrow">Marketplace feed</p>
            <h2>Search joined communities</h2>
          </div>
          <form className="searchbar" onSubmit={search}>
            <input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search for bikes, books, furniture..." />
            <button type="submit">Search</button>
          </form>
        </div>
        {error && <p className="error">{error}</p>}
        <div className="listing-grid">
          {listings.map(listing => (
            <article key={listing.id} className="listing-card">
              <p className="price">${listing.price}</p>
              <h3>{listing.title}</h3>
              <p>{listing.description}</p>
              <small>{listing.category} • {listing.itemCondition}</small>
            </article>
          ))}
          {listings.length === 0 && <p>No visible listings yet. Join a community or create the first post.</p>}
        </div>
      </section>

      <section className="panel wide">
        <h2>Discover communities</h2>
        <form className="searchbar invite-form" onSubmit={joinByInvite}>
          <input value={inviteToken} onChange={event => setInviteToken(event.target.value)} placeholder="Have an invite token? Paste it here." />
          <button type="submit">Join with invite</button>
        </form>
        <div className="community-grid">
          {discover.map(community => (
            <article key={community.id} className="community-card">
              <h3>{community.name}</h3>
              <p>{community.description}</p>
              <div className="button-row">
                <button onClick={() => joinByDomain(community.id)}>Join by email</button>
                <button className="ghost" onClick={() => requestMembership(community.id)}>Request access</button>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
