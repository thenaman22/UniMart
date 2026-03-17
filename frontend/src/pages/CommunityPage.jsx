import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api'
import { ListingPreview } from '../components/ListingPreview'

export function CommunityPage({ user, communities, onCommunitiesChanged }) {
  const { communityId } = useParams()
  const navigate = useNavigate()
  const [listings, setListings] = useState([])
  const [communityDetail, setCommunityDetail] = useState(null)
  const [invite, setInvite] = useState(null)
  const [newDomain, setNewDomain] = useState('')
  const [error, setError] = useState('')
  const [status, setStatus] = useState('')
  const [deleting, setDeleting] = useState(false)
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
      setError('')
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
      setError('')
      await api(`/communities/${communityId}/domains`, {
        method: 'POST',
        body: JSON.stringify({ emailDomain: newDomain })
      })
      setNewDomain('')
      setStatus('Email domain added.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function deleteCommunity() {
    if (!window.confirm(`Delete ${communityDetail.name}? This permanently removes its listings, messages, members, and reports.`)) {
      return
    }
    setDeleting(true)
    setError('')
    try {
      await api(`/communities/${communityId}`, { method: 'DELETE' })
      await onCommunitiesChanged?.()
      navigate('/communities')
    } catch (err) {
      setError(err.message)
    } finally {
      setDeleting(false)
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
        <p>Only approved members can view listings. Posting policy: {communityDetail.postingPolicyLabel}.</p>
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
          <p className="eyebrow">{community.roleLabel || community.role}</p>
          <h1>{community.name}</h1>
          <p className="feed-meta">{communityDetail.postingPolicyLabel}</p>
        </div>
        <div className="button-row wrap-row">
          {community.canPost && <Link className="button-link dark" to="/sell">Create listing</Link>}
          {community.canManageCommunity && (
            <button onClick={generateInvite}>Generate invite link</button>
          )}
          {community.canDelete && (
            <button className="ghost" onClick={deleteCommunity} disabled={deleting}>
              {deleting ? 'Deleting...' : 'Delete community'}
            </button>
          )}
        </div>
      </div>

      {invite && <p className="code-block">Invite token: {invite.token}</p>}
      {status && <p className="success">{status}</p>}
      {error && <p className="error">{error}</p>}
      {community.canManageCommunity && (
        <form className="searchbar invite-form" onSubmit={addDomain}>
          <input value={newDomain} onChange={event => setNewDomain(event.target.value)} placeholder="Add allowed email domain" />
          <button type="submit">Add domain</button>
        </form>
      )}

      <div className="listing-grid community-market-grid">
        {listings.map(listing => (
          <article key={listing.id} className="listing-card community-listing-card">
            <ListingPreview listing={listing} mode="grid" />
            <div className="community-listing-card-body">
              <p className="price">${listing.price}</p>
              <h3>{listing.title}</h3>
              <p className="community-listing-description">{listing.description}</p>
              <div className="community-listing-footer">
                <small className="community-listing-seller">
                  <Link className="text-link" to={`/users/${listing.sellerId}`}>{listing.sellerName}</Link>
                </small>
                {user && listing.sellerId !== user.id && (
                  <div className="button-row wrap-row community-listing-actions">
                    <Link className="button-link dark" to={`/messages?view=buyer&compose=${listing.id}`}>
                      Message seller
                    </Link>
                  </div>
                )}
              </div>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
