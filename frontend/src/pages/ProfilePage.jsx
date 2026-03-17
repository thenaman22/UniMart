import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { ListingPreview } from '../components/ListingPreview'

const OWNER_TABS = [
  { id: 'current', label: 'Current', emptyCopy: 'No current listings yet. Publish a listing to get started.' },
  { id: 'sold', label: 'Sold', emptyCopy: 'No sold listings yet.' },
  { id: 'archive', label: 'Archive', emptyCopy: 'No archived listings yet.' }
]

function hasListingMedia(listing) {
  return Boolean(listing.media?.length || listing.previewMediaUrl)
}

function formatStatusLabel(status) {
  if (status === 'ACTIVE') return 'Current'
  if (status === 'SOLD') return 'Sold'
  if (status === 'REMOVED') return 'Archive'
  return status
}

export function ProfilePage({ user, communities, onCommunitiesChanged }) {
  const [profile, setProfile] = useState(null)
  const [listings, setListings] = useState([])
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState('current')
  const [communityModalOpen, setCommunityModalOpen] = useState(false)
  const [communitySearch, setCommunitySearch] = useState('')
  const [leavingCommunityId, setLeavingCommunityId] = useState(null)

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
      active,
      sold
    }
  }, [listings])

  const listingsByTab = useMemo(() => ({
    current: listings.filter(listing => listing.status === 'ACTIVE'),
    sold: listings.filter(listing => listing.status === 'SOLD'),
    archive: listings.filter(listing => listing.status === 'REMOVED')
  }), [listings])

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

  const visibleListings = listingsByTab[activeTab] || []
  const activeTabConfig = OWNER_TABS.find(tab => tab.id === activeTab) || OWNER_TABS[0]
  const joinedCommunities = communities || []
  const filteredCommunities = joinedCommunities.filter(community => {
    const query = communitySearch.trim().toLowerCase()
    if (!query) return true
    return (
      community.name.toLowerCase().includes(query) ||
      community.role.toLowerCase().includes(query)
    )
  })

  async function leaveCommunity(community) {
    if (!window.confirm(`Leave ${community.name}? You will lose access to that marketplace and any profile listings from it.`)) {
      return
    }

    setLeavingCommunityId(community.communityId)
    setStatus('')
    setError('')

    try {
      await api(`/communities/${community.communityId}/membership`, { method: 'DELETE' })
      await Promise.all([
        loadProfile(),
        onCommunitiesChanged?.()
      ])
      setStatus(`Left ${community.name}.`)
    } catch (err) {
      setError(err.message)
    } finally {
      setLeavingCommunityId(null)
    }
  }

  return (
    <div className="stack profile-page-stack profile-page-shell">
      <section className="panel profile-hero-card profile-hero-card-owner">
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
                <p className="eyebrow">Your storefront</p>
                <h1>{profile.displayName}</h1>
              </div>
              <div className="button-row wrap-row profile-hero-actions">
                <Link className="button-link dark" to="/profile/edit">Edit profile</Link>
                <Link className="button-link" to="/sell">Create listing</Link>
              </div>
            </div>

            <div className="profile-stat-strip">
              <div className="profile-stat-pill">
                <strong>{stats.active}</strong>
                <span>Current</span>
              </div>
              <div className="profile-stat-pill">
                <strong>{stats.sold}</strong>
                <span>Sold</span>
              </div>
              <button type="button" className="profile-stat-pill profile-stat-pill-button" onClick={() => setCommunityModalOpen(true)}>
                <strong>{joinedCommunities.length}</strong>
                <span>Communities</span>
              </button>
            </div>

            <div className="profile-info-stack">
              <p className="profile-bio-copy">{profile.bio || 'Add a bio from your edit page so other members know what you sell.'}</p>
              <div className="profile-contact-row">
                <span>{profile.email}</span>
                {profile.location && <span>{profile.location}</span>}
                {profile.publicPhoneVisible && profile.phoneNumber && <span>{profile.phoneNumber}</span>}
              </div>
            </div>
          </div>
        </div>
      </section>

      {status && <p className="success">{status}</p>}
      {error && <p className="error">{error}</p>}

      <section className="panel profile-marketplace-panel">
        <div className="profile-section-bar">
          <div>
            <p className="eyebrow">Your marketplace</p>
            <h2>Your listings</h2>
            <p className="profile-section-copy">Switch between current, sold, and archived posts without leaving your profile.</p>
          </div>
        </div>

        <div className="profile-tab-row" role="tablist" aria-label="Your listings">
          {OWNER_TABS.map(tab => (
            <button
              key={tab.id}
              type="button"
              className={`profile-tab${activeTab === tab.id ? ' active' : ''}`}
              onClick={() => setActiveTab(tab.id)}
            >
              <span>{tab.label}</span>
              <strong>{listingsByTab[tab.id].length}</strong>
            </button>
          ))}
        </div>

        <div className="profile-gallery-grid">
          {visibleListings.map(listing => (
            <article key={listing.id} className="profile-tile-card">
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
                  <span className={`badge ${listing.status === 'ACTIVE' ? 'success' : 'neutral'}`}>
                    {formatStatusLabel(listing.status)}
                  </span>
                </div>

                <div className="profile-tile-copy profile-tile-copy-static">
                  <strong>{listing.title}</strong>
                  <p>{listing.description}</p>
                </div>

                <p className="feed-meta">{listing.category} • {listing.itemCondition}</p>

                <div className="profile-tile-actions">
                  {activeTab === 'current' && (
                    <>
                      <Link className="profile-tile-action profile-tile-action-primary" to={`/listings/${listing.id}/edit`}>Edit</Link>
                      <button type="button" className="profile-tile-action" onClick={() => updateListingStatus(listing.id, 'SOLD', 'Listing marked as sold.')}>
                        Sold
                      </button>
                      <button type="button" className="profile-tile-action" onClick={() => updateListingStatus(listing.id, 'REMOVED', 'Listing archived.')}>
                        Archive
                      </button>
                    </>
                  )}
                  {activeTab === 'sold' && (
                    <>
                      <button type="button" className="profile-tile-action" onClick={() => updateListingStatus(listing.id, 'ACTIVE', 'Listing relisted.')}>
                        Relist
                      </button>
                      <button type="button" className="profile-tile-action" onClick={() => updateListingStatus(listing.id, 'REMOVED', 'Listing archived.')}>
                        Archive
                      </button>
                    </>
                  )}
                  {activeTab === 'archive' && (
                    <>
                      <button type="button" className="profile-tile-action" onClick={() => updateListingStatus(listing.id, 'ACTIVE', 'Listing relisted.')}>
                        Relist
                      </button>
                      <button type="button" className="profile-tile-action profile-tile-action-danger" onClick={() => deleteListing(listing.id)}>
                        Delete
                      </button>
                    </>
                  )}
                </div>
              </div>
            </article>
          ))}

          {visibleListings.length === 0 && (
            <div className="profile-empty-state">
              <p>{activeTabConfig.emptyCopy}</p>
            </div>
          )}
        </div>
      </section>

      {communityModalOpen && (
        <div className="profile-modal-backdrop" role="presentation" onClick={() => setCommunityModalOpen(false)}>
          <div className="profile-modal-card" role="dialog" aria-modal="true" aria-label="Your communities" onClick={event => event.stopPropagation()}>
            <div className="profile-modal-header">
              <div>
                <p className="eyebrow">Your spaces</p>
                <h2>Communities</h2>
                <p className="profile-section-copy">You are currently part of {joinedCommunities.length} communities.</p>
              </div>
              <button type="button" className="profile-modal-close" onClick={() => setCommunityModalOpen(false)} aria-label="Close communities list">
                ×
              </button>
            </div>

            <div className="profile-modal-search">
              <input
                value={communitySearch}
                onChange={event => setCommunitySearch(event.target.value)}
                placeholder="Search communities"
              />
            </div>

            <div className="profile-community-list">
              {filteredCommunities.map(community => (
                <article key={community.communityId} className="profile-community-row">
                  <div className="profile-community-copy">
                    <strong>{community.name}</strong>
                    <span>{community.role}</span>
                  </div>
                  <div className="profile-community-actions">
                    <Link className="profile-tile-action" to={`/communities/${community.communityId}`} onClick={() => setCommunityModalOpen(false)}>
                      View
                    </Link>
                    <button
                      type="button"
                      className="profile-tile-action profile-community-leave"
                      disabled={leavingCommunityId === community.communityId}
                      onClick={() => leaveCommunity(community)}
                    >
                      {leavingCommunityId === community.communityId ? 'Leaving...' : 'Leave'}
                    </button>
                  </div>
                </article>
              ))}

              {filteredCommunities.length === 0 && (
                <div className="profile-empty-state profile-empty-state-compact">
                  <p>No communities match your search.</p>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
