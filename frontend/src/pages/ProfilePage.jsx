import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { ListingPreview } from '../components/ListingPreview'

export function ProfilePage({ user }) {
  const [profile, setProfile] = useState(null)
  const [listings, setListings] = useState([])
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')

  async function loadProfile() {
    const response = await api('/profile')
    setProfile(response.profile)
    setListings(response.myListings || [])
  }

  useEffect(() => {
    if (!user) return
    loadProfile().catch(err => setError(err.message))
  }, [user])

  const stats = useMemo(() => {
    const active = listings.filter(listing => listing.status === 'ACTIVE').length
    const sold = listings.filter(listing => listing.status === 'SOLD').length
    return {
      total: listings.length,
      active,
      sold
    }
  }, [listings])

  if (!user) {
    return <section className="panel"><p>Please sign in to view your profile.</p></section>
  }

  if (error && !profile) {
    return <section className="panel"><p className="error">{error}</p></section>
  }

  if (!profile) {
    return <section className="panel"><p>Loading your profile...</p></section>
  }

  async function updateListingStatus(listingId, nextStatus, successMessage) {
    setStatus('')
    setError('')
    try {
      await api(`/listings/${listingId}/status?status=${nextStatus}`, { method: 'PATCH' })
      await loadProfile()
      setStatus(successMessage)
    } catch (err) {
      setError(err.message)
    }
  }

  async function deleteListing(listingId) {
    if (!window.confirm('Delete this listing permanently?')) {
      return
    }
    setStatus('')
    setError('')
    try {
      await api(`/listings/${listingId}`, { method: 'DELETE' })
      setListings(current => current.filter(item => item.id !== listingId))
      setStatus('Listing deleted.')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="stack profile-page-stack">
      <section className="panel seller-hero own-profile-hero">
        <div className="seller-profile-header">
          <div className="profile-avatar profile-avatar-large">
            {profile.profileImageUrl ? (
              <img src={`http://localhost:8080${profile.profileImageUrl}`} alt={profile.displayName} />
            ) : (
              <span>{profile.displayName?.[0] || '?'}</span>
            )}
          </div>

          <div className="seller-profile-copy seller-profile-copy-wide">
            <div className="seller-name-row">
              <h1>{profile.displayName}</h1>
              <Link className="button-link dark" to="/profile/edit">Edit profile</Link>
              <a className="button-link" href="#my-listings">Manage listings</a>
            </div>

            <div className="seller-stats-row">
              <span><strong>{stats.total}</strong> posts</span>
              <span><strong>{stats.active}</strong> active</span>
              <span><strong>{stats.sold}</strong> sold</span>
            </div>

            <div className="seller-bio-block">
              <strong>{profile.email}</strong>
              {profile.location && <span>{profile.location}</span>}
              {profile.publicPhoneVisible && profile.phoneNumber && <span>{profile.phoneNumber}</span>}
              <p>{profile.bio || 'Add a bio from your edit page so other members know what you sell.'}</p>
            </div>
          </div>
        </div>
      </section>

      {status && <p className="success">{status}</p>}
      {error && <p className="error">{error}</p>}

      <section id="my-listings" className="panel">
        <div className="feed-header profile-section-header">
          <div>
            <p className="eyebrow">Your marketplace</p>
            <h2>Your listings</h2>
          </div>
          <Link className="button-link dark" to="/sell">Create listing</Link>
        </div>

        <div className="listing-grid profile-grid">
          {listings.map(listing => (
            <article key={listing.id} className="listing-card profile-listing-card">
              <ListingPreview listing={listing} mode="grid" />
              <div className="profile-listing-body">
                <div className="listing-heading-row">
                  <div>
                    <h3>{listing.title}</h3>
                    <p className="feed-meta">{listing.category} • {listing.itemCondition}</p>
                  </div>
                  <div className="listing-heading-meta">
                    <p className="price">${listing.price}</p>
                    <span className={`badge ${listing.status === 'ACTIVE' ? 'success' : 'neutral'}`}>{listing.status}</span>
                  </div>
                </div>
                <p>{listing.description}</p>
                <div className="button-row wrap-row">
                  <Link className="button-link dark" to={`/listings/${listing.id}/edit`}>Edit details</Link>
                  {listing.status !== 'SOLD' && (
                    <button type="button" className="ghost" onClick={() => updateListingStatus(listing.id, 'SOLD', 'Listing marked as sold.')}>
                      Mark sold
                    </button>
                  )}
                  {listing.status !== 'ACTIVE' && (
                    <button type="button" className="ghost" onClick={() => updateListingStatus(listing.id, 'ACTIVE', 'Listing relisted.')}>
                      Relist
                    </button>
                  )}
                  {listing.status !== 'REMOVED' && (
                    <button type="button" className="ghost" onClick={() => updateListingStatus(listing.id, 'REMOVED', 'Listing removed from sale.')}>
                      Remove
                    </button>
                  )}
                  <button type="button" className="ghost danger-button" onClick={() => deleteListing(listing.id)}>
                    Delete
                  </button>
                </div>
              </div>
            </article>
          ))}
          {listings.length === 0 && <p>You have not created any listings yet.</p>}
        </div>
      </section>
    </div>
  )
}
