import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ListingPreview } from '../components/ListingPreview'
import { api } from '../api'

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
            </div>
            <div className="seller-bio-block">
              <strong>{profile.email}</strong>
              {profile.location && <span>{profile.location}</span>}
              {profile.phoneNumber && <span>{profile.phoneNumber}</span>}
              <p>{profile.bio || 'This seller has not added a bio yet.'}</p>
            </div>
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="feed-header">
          <div>
            <p className="eyebrow">Available now</p>
            <h2>{profile.displayName}'s active listings</h2>
          </div>
        </div>
        <div className="listing-grid">
          {listings.map(listing => (
            <article key={listing.id} className="listing-card profile-listing-card">
              <ListingPreview listing={listing} mode="grid" />
              <div className="profile-listing-body">
                <div className="listing-heading-row">
                  <div>
                    <h3>{listing.title}</h3>
                    <p className="feed-meta">{listing.category} • {listing.itemCondition}</p>
                  </div>
                  <p className="price">${listing.price}</p>
                </div>
                <p>{listing.description}</p>
                {user && listing.sellerId !== user.id && (
                  <div className="button-row wrap-row">
                    <Link className="button-link dark" to={`/messages?view=buyer&compose=${listing.id}`}>
                      Message seller
                    </Link>
                  </div>
                )}
              </div>
            </article>
          ))}
          {listings.length === 0 && <p>This seller has no active listings right now.</p>}
        </div>
      </section>
    </div>
  )
}
