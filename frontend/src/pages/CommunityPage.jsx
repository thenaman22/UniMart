import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import { ListingPreview } from '../components/ListingPreview'

export function CommunityPage({ user, communities }) {
  const { communityId } = useParams()
  const [listings, setListings] = useState([])
  const [communityDetail, setCommunityDetail] = useState(null)
  const [invite, setInvite] = useState(null)
  const [newDomain, setNewDomain] = useState('')
  const [error, setError] = useState('')
  const community = communities.find(item => String(item.communityId) === communityId)

  useEffect(() => {
    if (!communityId) return
    api(`/communities/${communityId}`)
      .then(setCommunityDetail)
      .catch(err => setError(err.message))
  }, [communityId])

  useEffect(() => {
    if (!user || !communityId || !community) return
    api('/listings')
      .then(response => setListings(response.filter(item => String(item.communityId) === communityId)))
      .catch(err => setError(err.message))
  }, [user, communityId, community])

  async function generateInvite() {
    try {
      const response = await api(`/communities/${communityId}/invites`, {
        method: 'POST',
        body: JSON.stringify({ maxUses: 10 })
      })
      setInvite(response)
    } catch (err) {
      setError(err.message)
    }
  }

  async function addDomain(event) {
    event.preventDefault()
    try {
      await api(`/communities/${communityId}/domains`, {
        method: 'POST',
        body: JSON.stringify({ emailDomain: newDomain })
      })
      setNewDomain('')
      setError('Email domain added.')
    } catch (err) {
      setError(err.message)
    }
  }

  if (!communityDetail) {
    return <section className="panel"><p>Loading community...</p></section>
  }

  if (!community) {
    return (
      <section className="panel">
        <div className="panel-header panel-stack-mobile">
          <div>
            <span className="badge neutral">{communityDetail.privateCommunity ? 'Closed marketplace' : 'Open marketplace'}</span>
            <h1>{communityDetail.name}</h1>
            <p>{communityDetail.description}</p>
          </div>
        </div>
        <p>Only approved members can view listings and create posts in this community.</p>
        {!user && <p>Sign in first to request access or join with an invite.</p>}
        {error && <p className="error">{error}</p>}
      </section>
    )
  }

  return (
    <section className="panel">
      <div className="panel-header">
        <div>
          <span className="badge neutral">{communityDetail.privateCommunity ? 'Closed marketplace' : 'Open marketplace'}</span>
          <p className="eyebrow">{community.role}</p>
          <h1>{community.name}</h1>
        </div>
        {(community.role === 'ADMIN' || community.role === 'MODERATOR') && (
          <button onClick={generateInvite}>Generate invite link</button>
        )}
      </div>

      {invite && <p className="code-block">Invite token: {invite.token}</p>}
      {error && <p className="error">{error}</p>}
      {(community.role === 'ADMIN' || community.role === 'MODERATOR') && (
        <form className="searchbar invite-form" onSubmit={addDomain}>
          <input value={newDomain} onChange={event => setNewDomain(event.target.value)} placeholder="Add allowed email domain" />
          <button type="submit">Add domain</button>
        </form>
      )}

      <div className="listing-grid">
        {listings.map(listing => (
          <article key={listing.id} className="listing-card">
            <ListingPreview listing={listing} mode="grid" />
            <p className="price">${listing.price}</p>
            <h3>{listing.title}</h3>
            <p>{listing.description}</p>
            <small><Link className="text-link" to={`/users/${listing.sellerId}`}>{listing.sellerName}</Link></small>
          </article>
        ))}
      </div>
    </section>
  )
}
