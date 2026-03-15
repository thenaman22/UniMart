import { useState } from 'react'
import { api } from '../api'

export function CreateListingPage({ user, communities }) {
  const [form, setForm] = useState({
    communityId: '',
    title: '',
    description: '',
    price: '',
    category: '',
    itemCondition: '',
    mediaType: 'image/jpeg'
  })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  if (!user) {
    return <section className="panel"><p>Please sign in to create a listing.</p></section>
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    try {
      const upload = await api('/uploads/prepare', {
        method: 'POST',
        body: JSON.stringify({ contentType: form.mediaType, fileSize: 1024 })
      })

      await api('/listings', {
        method: 'POST',
        body: JSON.stringify({
          communityId: Number(form.communityId),
          title: form.title,
          description: form.description,
          price: Number(form.price),
          category: form.category,
          itemCondition: form.itemCondition,
          media: [{
            storageKey: upload.storageKey,
            contentType: upload.contentType,
            fileSize: upload.fileSize
          }]
        })
      })

      setMessage('Listing published successfully.')
      setForm({
        communityId: '',
        title: '',
        description: '',
        price: '',
        category: '',
        itemCondition: '',
        mediaType: 'image/jpeg'
      })
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
          {communities.map(community => (
            <option key={community.communityId} value={community.communityId}>{community.name}</option>
          ))}
        </select>
        <input value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} placeholder="Title" required />
        <textarea value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} placeholder="Description" rows="5" required />
        <input value={form.price} onChange={event => setForm({ ...form, price: event.target.value })} placeholder="Price" type="number" min="0" step="0.01" required />
        <input value={form.category} onChange={event => setForm({ ...form, category: event.target.value })} placeholder="Category" required />
        <input value={form.itemCondition} onChange={event => setForm({ ...form, itemCondition: event.target.value })} placeholder="Condition" required />
        <select value={form.mediaType} onChange={event => setForm({ ...form, mediaType: event.target.value })}>
          <option value="image/jpeg">Image</option>
          <option value="video/mp4">Video</option>
        </select>
        <button type="submit">Publish listing</button>
      </form>

      {message && <p className="success">{message}</p>}
      {error && <p className="error">{error}</p>}
    </section>
  )
}
