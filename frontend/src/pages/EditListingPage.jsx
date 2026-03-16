import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ListingPreview } from '../components/ListingPreview'
import { api } from '../api'

export function EditListingPage({ user, communities }) {
  const { listingId } = useParams()
  const navigate = useNavigate()
  const [form, setForm] = useState({
    title: '',
    description: '',
    price: '',
    category: '',
    itemCondition: ''
  })
  const [listing, setListing] = useState(null)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const communityName = useMemo(() => {
    if (!listing) return ''
    return communities.find(item => item.communityId === listing.communityId)?.name || ''
  }, [communities, listing])

  useEffect(() => {
    if (!user || !listingId) return
    api(`/listings/${listingId}`)
      .then(response => {
        const nextListing = response.listing
        setListing(nextListing)
        setForm({
          title: nextListing.title,
          description: nextListing.description,
          price: nextListing.price,
          category: nextListing.category,
          itemCondition: nextListing.itemCondition
        })
        setError('')
      })
      .catch(err => setError(err.message))
  }, [user, listingId])

  async function submit(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    try {
      const updated = await api(`/listings/${listingId}`, {
        method: 'PATCH',
        body: JSON.stringify({
          title: form.title,
          description: form.description,
          price: Number(form.price),
          category: form.category,
          itemCondition: form.itemCondition
        })
      })
      setListing(updated)
      setMessage('Listing updated successfully.')
    } catch (err) {
      setError(err.message)
    }
  }

  if (!user) {
    return <section className="panel"><p>Please sign in to edit your listing.</p></section>
  }

  if (error && !listing) {
    return <section className="panel"><p className="error">{error}</p></section>
  }

  if (!listing) {
    return <section className="panel"><p>Loading listing...</p></section>
  }

  if (listing.sellerId !== user.id) {
    return <section className="panel"><p>You can only edit your own listings.</p></section>
  }

  return (
    <div className="social-layout profile-layout">
      <section className="panel">
        <div className="panel-header panel-stack-mobile">
          <div>
            <p className="eyebrow">Edit listing</p>
            <h1>Update listing details</h1>
            <p>{communityName ? `Visible in ${communityName}.` : 'Update the details shoppers see on your listing.'}</p>
          </div>
          <Link className="button-link ghost-link" to="/profile">Back to profile</Link>
        </div>
        <form className="stack profile-form" onSubmit={submit}>
          <label>
            <span>Title</span>
            <input value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} required />
          </label>
          <label>
            <span>Description</span>
            <textarea value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} rows="5" required />
          </label>
          <label>
            <span>Price</span>
            <input value={form.price} onChange={event => setForm({ ...form, price: event.target.value })} type="number" min="0" step="0.01" required />
          </label>
          <label>
            <span>Category</span>
            <input value={form.category} onChange={event => setForm({ ...form, category: event.target.value })} required />
          </label>
          <label>
            <span>Condition</span>
            <input value={form.itemCondition} onChange={event => setForm({ ...form, itemCondition: event.target.value })} required />
          </label>
          <div className="button-row wrap-row">
            <button type="submit">Save changes</button>
            <button type="button" className="ghost" onClick={() => navigate('/profile')}>Cancel</button>
          </div>
        </form>
        {message && <p className="success">{message}</p>}
        {error && <p className="error">{error}</p>}
      </section>

      <aside className="panel sidebar-panel">
        <div className="sidebar-section">
          <p className="eyebrow">Preview</p>
          <h2>Current media</h2>
        </div>
        <article className="listing-card profile-listing-card">
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
          </div>
        </article>
      </aside>
    </div>
  )
}
