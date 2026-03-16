import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'

export function ProfileEditPage({ user, onProfileUpdated }) {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    displayName: '',
    email: '',
    bio: '',
    phoneNumber: '',
    location: '',
    publicPhoneVisible: false
  })
  const [status, setStatus] = useState('')
  const [error, setError] = useState('')
  const [picturePreview, setPicturePreview] = useState('')

  useEffect(() => {
    if (!user) return
    api('/profile')
      .then(response => {
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
      })
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

  return (
    <div className="social-layout profile-layout">
      <section className="panel">
        <div className="panel-header panel-stack-mobile">
          <div>
            <p className="eyebrow">Edit profile</p>
            <h1>Update your seller profile</h1>
            <p>Choose what other members can see when they visit your seller page.</p>
          </div>
          <Link className="button-link ghost-link" to="/profile">Back to profile</Link>
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
          <div className="button-row wrap-row">
            <button type="submit">Save profile</button>
            <button type="button" className="ghost" onClick={() => navigate('/profile')}>Cancel</button>
          </div>
        </form>

        {status && <p className="success">{status}</p>}
        {error && <p className="error">{error}</p>}
      </section>

      <aside className="panel sidebar-panel">
        <div className="sidebar-section">
          <p className="eyebrow">Photo</p>
          <h2>Profile picture</h2>
        </div>
        <div className="profile-avatar-block">
          <div className="profile-avatar profile-avatar-large">
            {picturePreview ? <img src={`http://localhost:8080${picturePreview}`} alt="Profile" /> : <span>{form.displayName?.[0] || '?'}</span>}
          </div>
          <input type="file" accept="image/jpeg,image/png,image/webp" onChange={uploadPicture} />
          <p className="feed-intro-copy">A square image works best for the profile header.</p>
        </div>
      </aside>
    </div>
  )
}
