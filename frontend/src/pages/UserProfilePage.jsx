import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ListingPreview } from '../components/ListingPreview'
import { api } from '../api'

function hasListingMedia(listing) {
  return Boolean(listing.media?.length || listing.previewMediaUrl)
}

export function UserProfilePage({ user }) {
  const { userId } = useParams()
  const [profile, setProfile] = useState(null)
  const [listings, setListings] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user || !userId) return
    api(`/users/${userId}/profile`)
      .then(response => {
        setProfile(response.profile)
        setListings(response.activeListings || [])
        setError('')
      })
      .catch(err => {
        setError(err.message)
        setProfile(null)
        setListings([])
      })
  }, [user, userId])

  if (!user) {
    return <section className="panel"><p>Please sign in to view seller profiles.</p></section>
  }

  if (error) {
    return <section className="panel"><p className="error">{error}</p></section>
  }

  if (!profile) {
    return <section className="panel"><p>Loading seller profile...</p></section>
  }

  return (
    <div className="stack profile-page-stack profile-page-shell">
      <section className="panel profile-hero-card profile-hero-card-public">
        <div className="profile-hero-main">
          <div className="profile-avatar profile-avatar-large profile-hero-avatar">
            {profile.profileImageUrl ? (
              <img src={`http://localhost:8080${profile.profileImageUrl}`} alt={profile.displayName} />
            ) : (
              <span>{profile.displayName?.[0] || '?'}</span>
            )}
          </div>

          <div className="profile-hero-copy">
            <div className="profile-hero-heading">
              <div className="profile-hero-title-block">
                <p className="eyebrow">Seller profile</p>
                <h1>{profile.displayName}</h1>
              </div>
            </div>

            <div className="profile-stat-strip profile-stat-strip-public">
              <div className="profile-stat-pill">
                <strong>{listings.length}</strong>
                <span>Current listings</span>
              </div>
            </div>

            <div className="profile-info-stack">
              <p className="profile-bio-copy">{profile.bio || 'This seller has not added a bio yet.'}</p>
              <div className="profile-contact-row">
                {profile.location && <span>{profile.location}</span>}
                {profile.phoneNumber && <span>{profile.phoneNumber}</span>}
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="panel profile-marketplace-panel">
        <div className="profile-section-bar">
          <div>
            <p className="eyebrow">Available now</p>
            <h2>{profile.displayName}'s current listings</h2>
            <p className="profile-section-copy">These listings are visible because you share at least one community with this seller.</p>
          </div>
        </div>

        <div className="profile-gallery-grid">
          {listings.map(listing => (
            <article key={listing.id} className="profile-tile-card profile-tile-card-public">
              {hasListingMedia(listing) ? (
                <ListingPreview listing={listing} mode="tile" />
              ) : (
                <div className="profile-tile-placeholder">
                  <span>{listing.title?.[0] || '?'}</span>
                </div>
              )}

              <div className="profile-tile-body">
                <div className="profile-tile-price-row">
                  <p className="price">${listing.price}</p>
                </div>

                <div className="profile-tile-copy profile-tile-copy-static">
                  <strong>{listing.title}</strong>
                  <p>{listing.description}</p>
                </div>

                <p className="feed-meta">{listing.category} • {listing.itemCondition}</p>

                {user && listing.sellerId !== user.id && (
                  <div className="profile-tile-actions">
                    <Link className="profile-tile-action profile-tile-action-primary" to={`/messages?view=buyer&compose=${listing.id}`}>
                      Message seller
                    </Link>
                  </div>
                )}
              </div>
            </article>
          ))}
          {listings.length === 0 && (
            <div className="profile-empty-state">
              <p>This seller has no current listings in your shared communities right now.</p>
            </div>
          )}
        </div>
      </section>
    </div>
  )
}
