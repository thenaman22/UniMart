import { useState } from 'react'
import { api } from '../api'

export function CreateListingPage({ user, communities }) {
  const postableCommunities = communities.filter(community => community.canPost)
  const [form, setForm] = useState({
    communityId: '',
    title: '',
    description: '',
    price: '',
    category: '',
    itemCondition: ''
  })
  const [files, setFiles] = useState([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  if (!user) {
    return <section className="panel"><p>Please sign in to create a listing.</p></section>
  }

  if (postableCommunities.length === 0) {
    return (
      <section className="panel">
        <h1>Create a listing</h1>
        <p>You do not currently have posting access in any community.</p>
        <p className="feed-meta">Join a community that allows member posting, ask an admin to assign you as a seller, or create your own community.</p>
      </section>
    )
  }

  async function uploadFiles() {
    const token = localStorage.getItem('unimart-token')
    const uploaded = []
    for (const file of files) {
      const body = new FormData()
      body.append('file', file)
      const response = await fetch('http://localhost:8080/uploads/file', {
        method: 'POST',
        headers: token ? { 'X-Auth-Token': token } : {},
        body
      })
      const payload = await response.json()
      if (!response.ok) {
        throw new Error(payload.message || 'Upload failed')
      }
      uploaded.push({
        storageKey: payload.storageKey,
        contentType: payload.contentType,
        fileSize: payload.fileSize
      })
    }
    return uploaded
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    try {
      if (files.length === 0) {
        throw new Error('Please select at least one image or video.')
      }
      if (files.length > 5) {
        throw new Error('You can upload up to 5 photos or videos per listing.')
      }

      const media = await uploadFiles()
      await api('/listings', {
        method: 'POST',
        body: JSON.stringify({
          communityId: Number(form.communityId),
          title: form.title,
          description: form.description,
          price: Number(form.price),
          category: form.category,
          itemCondition: form.itemCondition,
          media
        })
      })

      setMessage('Listing published successfully.')
      setForm({
        communityId: '',
        title: '',
        description: '',
        price: '',
        category: '',
        itemCondition: ''
      })
      setFiles([])
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="panel">
      <h1>Create a listing</h1>
      <form className="stack" onSubmit={submit}>
        <select value={form.communityId} onChange={event => setForm({ ...form, communityId: event.target.value })} required>
          <option value="">Choose a community</option>
          {postableCommunities.map(community => (
            <option key={community.communityId} value={community.communityId}>{community.name}</option>
          ))}
        </select>
        <input value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} placeholder="Title" required />
        <textarea value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} placeholder="Description" rows="5" required />
        <input value={form.price} onChange={event => setForm({ ...form, price: event.target.value })} placeholder="Price" type="number" min="0" step="0.01" required />
        <input value={form.category} onChange={event => setForm({ ...form, category: event.target.value })} placeholder="Category" required />
        <input value={form.itemCondition} onChange={event => setForm({ ...form, itemCondition: event.target.value })} placeholder="Condition" required />
        <input
          type="file"
          accept="image/jpeg,image/png,image/webp,video/mp4,video/webm"
          multiple
          onChange={event => {
            const nextFiles = Array.from(event.target.files || []).slice(0, 5)
            setFiles(nextFiles)
            if ((event.target.files || []).length > 5) {
              setError('Only the first 5 files were selected.')
            } else {
              setError('')
            }
          }}
        />
        <p className="field-hint">Add up to 5 photos or videos. The first item becomes the opening slide.</p>
        {files.length > 0 && (
          <div className="media-preview-grid">
            {files.map(file => (
              <div key={`${file.name}-${file.size}`} className="media-chip">{file.name}</div>
            ))}
          </div>
        )}
        <button type="submit">Publish listing</button>
      </form>

      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}
    </section>
  )
}
