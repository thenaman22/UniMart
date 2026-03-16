import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { ListingPreview } from '../components/ListingPreview'

export function ProfilePage({ user, onProfileUpdated }) {
  const [form, setForm] = useState({
    displayName: '',
    email: '',
    bio: '',
    phoneNumber: '',
    location: '',
    publicPhoneVisible: false
  })
  const [listings, setListings] = useState([])
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [picturePreview, setPicturePreview] = useState('')

  async function loadProfile() {
    const response = await api('/profile')
    const profile = response.profile
    setForm({
      displayName: profile.displayName || '',
      email: profile.email || '',
      bio: profile.bio || '',
      phoneNumber: profile.phoneNumber || '',
      location: profile.location || '',
      publicPhoneVisible: Boolean(profile.publicPhoneVisible)
    })
    setPicturePreview(profile.profileImageUrl || '')
    setListings(response.myListings || [])
  }

  useEffect(() => {
    if (!user) return
    loadProfile()
      .catch(err => setError(err.message))
  }, [user])

  if (!user) {
    return <section className="panel"><p>Please sign in to edit your profile.</p></section>
  }

  async function submit(event) {
    event.preventDefault()
    setStatus('')
    setError('')
    try {
      const profile = await api('/profile', {
        method: 'PATCH',
        body: JSON.stringify({
          displayName: form.displayName,
          bio: form.bio,
          phoneNumber: form.phoneNumber,
          location: form.location,
          publicPhoneVisible: form.publicPhoneVisible
        })
      })
      onProfileUpdated(profile)
      setPicturePreview(profile.profileImageUrl || '')
      setStatus('Profile updated successfully.')
    } catch (err) {
      setError(err.message)
    }
  }

  async function uploadPicture(event) {
    const file = event.target.files?.[0]
    if (!file) return
    setStatus('')
    setError('')
    try {
      const body = new FormData()
      body.append('file', file)
      const token = localStorage.getItem('unimart-token')
      const response = await fetch('http://localhost:8080/profile/picture', {
        method: 'POST',
        headers: token ? { 'X-Auth-Token': token } : {},
        body
      })
      const profile = await response.json()
      if (!response.ok) {
        throw new Error(profile.message || 'Profile picture upload failed')
      }
      onProfileUpdated(profile)
      setPicturePreview(profile.profileImageUrl || '')
      setStatus('Profile picture updated.')
    } catch (err) {
      setError(err.message)
    }
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
    <div className="social-layout profile-layout">
      <section className="panel">
        <div className="panel-header panel-stack-mobile">
          <div>
            <p className="eyebrow">Your profile</p>
            <h1>Account and seller settings</h1>
            <p>This information belongs to your account. Public seller details appear to people who share a community with you.</p>
          </div>
          <div className="profile-avatar-block">
            <div className="profile-avatar">
              {picturePreview ? <img src={`http://localhost:8080${picturePreview}`} alt="Profile" /> : <span>{form.displayName?.[0] || '?'}</span>}
            </div>
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={uploadPicture} />
          </div>
        </div>

        <form className="stack profile-form" onSubmit={submit}>
          <label>
            <span>Display name</span>
            <input value={form.displayName} onChange={event => setForm({ ...form, displayName: event.target.value })} />
          </label>
          <label>
            <span>Email</span>
            <input value={form.email} disabled />
          </label>
          <label>
            <span>Bio</span>
            <textarea value={form.bio} onChange={event => setForm({ ...form, bio: event.target.value })} rows="5" />
          </label>
          <label>
            <span>Phone number</span>
            <input value={form.phoneNumber} onChange={event => setForm({ ...form, phoneNumber: event.target.value })} />
          </label>
          <label>
            <span>Location</span>
            <input value={form.location} onChange={event => setForm({ ...form, location: event.target.value })} />
          </label>
          <label className="checkbox-row">
            <input
              type="checkbox"
              checked={form.publicPhoneVisible}
              onChange={event => setForm({ ...form, publicPhoneVisible: event.target.checked })}
            />
            <span>Show phone number on your public seller profile</span>
          </label>
          <button type="submit">Save profile</button>
        </form>

        {status && <p className="success">{status}</p>}
        {error && <p className="error">{error}</p>}
      </section>

      <aside className="panel sidebar-panel">
        <div className="sidebar-section">
          <p className="eyebrow">Seller hub</p>
          <h2>My listings</h2>
          <p className="feed-intro-copy">Quick actions live here, and full edits open a dedicated editor.</p>
        </div>
        <div className="stack">
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
      </aside>
    </div>
  )
}
