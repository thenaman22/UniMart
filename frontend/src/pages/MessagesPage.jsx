import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api'

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleString([], {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  })
}

function Avatar({ participant }) {
  return (
    <div className="message-avatar">
      {participant?.profileImageUrl ? (
        <img src={`http://localhost:8080${participant.profileImageUrl}`} alt={participant.displayName} />
      ) : (
        <span>{participant?.displayName?.[0] || '?'}</span>
      )}
    </div>
  )
}

function ListingThumb({ listing }) {
  if (!listing?.previewMediaUrl) {
    return null
  }

  const src = `http://localhost:8080${listing.previewMediaUrl}`

  return (
    <div className="message-listing-thumb">
      {listing.previewMediaType === 'VIDEO' ? (
        <video src={src} muted playsInline />
      ) : (
        <img src={src} alt={listing.title} />
      )}
    </div>
  )
}

export function MessagesPage({ user }) {
  const [searchParams, setSearchParams] = useSearchParams()
  const [sellerListings, setSellerListings] = useState([])
  const [sellerThreads, setSellerThreads] = useState([])
  const [buyerListings, setBuyerListings] = useState([])
  const [selectedSellerListingId, setSelectedSellerListingId] = useState(null)
  const [selectedConversationId, setSelectedConversationId] = useState(null)
  const [conversation, setConversation] = useState(null)
  const [composeListing, setComposeListing] = useState(null)
  const [draft, setDraft] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [status, setStatus] = useState('')

  const composeListingId = searchParams.get('compose')
  const activeTab = useMemo(() => {
    if (composeListingId) return 'buyer'
    return searchParams.get('view') === 'buyer' ? 'buyer' : 'seller'
  }, [composeListingId, searchParams])
  const sellerUnreadTotal = useMemo(
    () => sellerListings.reduce((sum, item) => sum + (item.unreadCount || 0), 0),
    [sellerListings]
  )
  const buyerUnreadTotal = useMemo(
    () => buyerListings.reduce((sum, item) => sum + (item.unreadCount || 0), 0),
    [buyerListings]
  )

  useEffect(() => {
    if (!user) return
    loadInboxes()
  }, [user])

  useEffect(() => {
    if (!composeListingId || !user) {
      setComposeListing(null)
      return
    }

    const existing = buyerListings.find(item => String(item.listing.id) === composeListingId)
    if (existing) {
      setComposeListing(null)
      setSelectedConversationId(existing.conversationId)
      return
    }

    api(`/listings/${composeListingId}`)
      .then(response => {
        setComposeListing(response.listing)
        setSelectedConversationId(null)
      })
      .catch(err => setError(err.message))
  }, [buyerListings, composeListingId, user])

  useEffect(() => {
    if (!user || activeTab !== 'seller' || !selectedSellerListingId) {
      setSellerThreads([])
      return
    }

    api(`/messages/seller/listings/${selectedSellerListingId}`)
      .then(response => {
        setSellerThreads(response.conversations)
        setSelectedConversationId(current => {
          if (current && response.conversations.some(item => item.id === current)) {
            return current
          }
          return response.conversations[0]?.id || null
        })
      })
      .catch(err => setError(err.message))
  }, [activeTab, selectedSellerListingId, user])

  useEffect(() => {
    if (!user || !selectedConversationId) {
      setConversation(null)
      return
    }

    let cancelled = false

    async function loadConversation() {
      try {
        const response = await api(`/messages/conversations/${selectedConversationId}`)
        if (cancelled) return
        setConversation(response)
        setError('')
        await api(`/messages/conversations/${selectedConversationId}/read`, { method: 'POST' })
        if (cancelled) return
        await refreshLists({ preserveConversation: true })
      } catch (err) {
        if (!cancelled) {
          setError(err.message)
          setConversation(null)
        }
      }
    }

    loadConversation()
    return () => {
      cancelled = true
    }
  }, [selectedConversationId, user])

  useEffect(() => {
    if (activeTab === 'seller' && !composeListingId && sellerListings.length > 0 && !selectedSellerListingId) {
      setSelectedSellerListingId(sellerListings[0].listing.id)
    }
  }, [activeTab, composeListingId, sellerListings, selectedSellerListingId])

  useEffect(() => {
    if (activeTab === 'buyer' && !composeListingId && buyerListings.length > 0 && !selectedConversationId) {
      setSelectedConversationId(buyerListings[0].conversationId)
    }
  }, [activeTab, composeListingId, buyerListings, selectedConversationId])

  async function loadInboxes() {
    try {
      setError('')
      const [sellerResponse, buyerResponse] = await Promise.all([
        api('/messages/seller'),
        api('/messages/buyer')
      ])
      setSellerListings(sellerResponse.listings || [])
      setBuyerListings(buyerResponse.listings || [])
    } catch (err) {
      setError(err.message)
    }
  }

  async function refreshLists({ preserveConversation = false } = {}) {
    const [sellerResponse, buyerResponse] = await Promise.all([
      api('/messages/seller'),
      api('/messages/buyer')
    ])

    setSellerListings(sellerResponse.listings || [])
    setBuyerListings(buyerResponse.listings || [])

    if (activeTab === 'seller' && selectedSellerListingId) {
      const listingResponse = await api(`/messages/seller/listings/${selectedSellerListingId}`)
      setSellerThreads(listingResponse.conversations || [])
      if (!preserveConversation && !listingResponse.conversations.some(item => item.id === selectedConversationId)) {
        setSelectedConversationId(listingResponse.conversations[0]?.id || null)
      }
    }
  }

  function switchTab(nextTab) {
    const nextParams = new URLSearchParams(searchParams)
    nextParams.set('view', nextTab)
    nextParams.delete('compose')
    setSearchParams(nextParams)
    setComposeListing(null)
    setStatus('')
    setError('')
    if (nextTab === 'seller') {
      setSelectedConversationId(null)
      setSelectedSellerListingId(sellerListings[0]?.listing.id || null)
      return
    }
    setSelectedSellerListingId(null)
    setSelectedConversationId(buyerListings[0]?.conversationId || null)
  }

  async function sendMessage(event) {
    event.preventDefault()
    if (!draft.trim()) return

    setLoading(true)
    setError('')
    setStatus('')

    try {
      let response
      if (composeListing && !selectedConversationId) {
        response = await api(`/listings/${composeListing.id}/messages`, {
          method: 'POST',
          body: JSON.stringify({ body: draft.trim() })
        })
        const nextParams = new URLSearchParams(searchParams)
        nextParams.delete('compose')
        nextParams.set('view', 'buyer')
        setSearchParams(nextParams)
      } else {
        response = await api(`/messages/conversations/${selectedConversationId}/messages`, {
          method: 'POST',
          body: JSON.stringify({ body: draft.trim() })
        })
      }

      setConversation(response)
      setSelectedConversationId(response.conversationId)
      setComposeListing(null)
      setDraft('')
      setStatus('Message sent.')
      await refreshLists({ preserveConversation: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  if (!user) {
    return <section className="panel"><p>Please sign in to access messages.</p></section>
  }

  const selectedBuyerListing = buyerListings.find(item => item.conversationId === selectedConversationId)

  return (
    <div className="stack">
      <section className="panel messages-shell">
        <div className="feed-header messages-header">
          <div>
            <p className="eyebrow">Inbox</p>
            <h2>Messages</h2>
          </div>
          <div className="messages-tab-row">
            <button
              type="button"
              className={`ghost messages-tab${activeTab === 'seller' ? ' active' : ''}`}
              onClick={() => switchTab('seller')}
            >
              Seller inbox
              {sellerUnreadTotal > 0 && <span className="messages-tab-badge">{sellerUnreadTotal > 9 ? '9+' : sellerUnreadTotal}</span>}
            </button>
            <button
              type="button"
              className={`ghost messages-tab${activeTab === 'buyer' ? ' active' : ''}`}
              onClick={() => switchTab('buyer')}
            >
              Sent inquiries
              {buyerUnreadTotal > 0 && <span className="messages-tab-badge">{buyerUnreadTotal > 9 ? '9+' : buyerUnreadTotal}</span>}
            </button>
          </div>
        </div>

        {status && <p className="success">{status}</p>}
        {error && <p className="error">{error}</p>}

        {activeTab === 'seller' ? (
          <div className="messages-layout seller-layout">
            <aside className="messages-panel">
              <div className="messages-panel-header">
                <p className="eyebrow">Products</p>
                <h3>Listings with messages</h3>
              </div>
              <div className="messages-list">
                {sellerListings.map(item => (
                  <button
                    key={item.listing.id}
                    type="button"
                    className={`message-listing-card${selectedSellerListingId === item.listing.id ? ' active' : ''}${item.unreadCount > 0 ? ' unread' : ''}`}
                    onClick={() => {
                      setSelectedSellerListingId(item.listing.id)
                      setSelectedConversationId(null)
                    }}
                  >
                    <ListingThumb listing={item.listing} />
                    <div className="message-listing-copy">
                      <div className="message-listing-title-row">
                        <strong>{item.listing.title}</strong>
                        {item.unreadCount > 0 && (
                          <div className="message-unread-meta">
                            <span className="message-unread-dot" aria-hidden="true" />
                            <span className="badge success">{item.unreadCount} new</span>
                          </div>
                        )}
                      </div>
                      <span>{item.conversationCount} buyer threads</span>
                      <small>{formatDate(item.lastMessageAt)}</small>
                    </div>
                  </button>
                ))}
                {sellerListings.length === 0 && <p>You have no product inquiries yet.</p>}
              </div>
            </aside>

            <section className="messages-panel">
              <div className="messages-panel-header">
                <p className="eyebrow">Threads</p>
                <h3>{selectedSellerListingId ? 'Buyer conversations' : 'Select a product'}</h3>
              </div>
              <div className="messages-list">
                {sellerThreads.map(thread => (
                  <button
                    key={thread.id}
                    type="button"
                    className={`message-thread-card${selectedConversationId === thread.id ? ' active' : ''}${thread.unreadCount > 0 ? ' unread' : ''}`}
                    onClick={() => setSelectedConversationId(thread.id)}
                  >
                    <div className="message-thread-top">
                      <div className="message-thread-identity">
                        <Avatar participant={thread.buyer} />
                        <div>
                          <strong>{thread.buyer.displayName}</strong>
                          <small>{formatDate(thread.lastMessageAt)}</small>
                        </div>
                      </div>
                      {thread.unreadCount > 0 && (
                        <div className="message-unread-meta">
                          <span className="message-unread-dot" aria-hidden="true" />
                          <span className="badge success">{thread.unreadCount}</span>
                        </div>
                      )}
                    </div>
                    <p>{thread.lastMessagePreview || 'No messages yet.'}</p>
                  </button>
                ))}
                {selectedSellerListingId && sellerThreads.length === 0 && <p>No buyer threads for this listing yet.</p>}
                {!selectedSellerListingId && <p>Select a product to see who has messaged you.</p>}
              </div>
            </section>

            <ConversationPanel
              conversation={conversation}
              draft={draft}
              setDraft={setDraft}
              sendMessage={sendMessage}
              loading={loading}
              fallback="Select a buyer thread to open the conversation."
            />
          </div>
        ) : (
          <div className="messages-layout buyer-layout">
            <aside className="messages-panel">
              <div className="messages-panel-header">
                <p className="eyebrow">Sent inquiries</p>
                <h3>Your product conversations</h3>
              </div>
              <div className="messages-list">
                {buyerListings.map(item => (
                  <button
                    key={item.conversationId}
                    type="button"
                    className={`message-listing-card${selectedConversationId === item.conversationId ? ' active' : ''}${item.unreadCount > 0 ? ' unread' : ''}`}
                    onClick={() => {
                      const nextParams = new URLSearchParams(searchParams)
                      nextParams.delete('compose')
                      nextParams.set('view', 'buyer')
                      setSearchParams(nextParams)
                      setComposeListing(null)
                      setSelectedConversationId(item.conversationId)
                    }}
                  >
                    <ListingThumb listing={item.listing} />
                    <div className="message-listing-copy">
                      <div className="message-listing-title-row">
                        <strong>{item.listing.title}</strong>
                        {item.unreadCount > 0 && (
                          <div className="message-unread-meta">
                            <span className="message-unread-dot" aria-hidden="true" />
                            <span className="badge success">{item.unreadCount} new</span>
                          </div>
                        )}
                      </div>
                      <span>Seller: {item.seller.displayName}</span>
                      <small>{formatDate(item.lastMessageAt)}</small>
                    </div>
                  </button>
                ))}
                {buyerListings.length === 0 && !composeListing && <p>You have not sent any inquiries yet.</p>}
              </div>
            </aside>

            <ConversationPanel
              conversation={conversation}
              draft={draft}
              setDraft={setDraft}
              sendMessage={sendMessage}
              loading={loading}
              composeListing={composeListing}
              selectedBuyerListing={selectedBuyerListing}
              fallback="Choose a product inquiry or start one from a listing."
            />
          </div>
        )}
      </section>
    </div>
  )
}

function ConversationPanel({
  conversation,
  draft,
  setDraft,
  sendMessage,
  loading,
  composeListing = null,
  selectedBuyerListing = null,
  fallback
}) {
  const listing = conversation?.listing || composeListing || selectedBuyerListing?.listing
  const headerParticipant = conversation?.seller && conversation?.buyer
    ? (conversation.messages?.find(message => !message.mine)?.sender || conversation.seller)
    : selectedBuyerListing?.seller

  return (
    <section className="messages-panel conversation-panel">
      {listing ? (
        <>
          <div className="messages-panel-header conversation-header">
            <div>
              <p className="eyebrow">Listing context</p>
              <h3>{listing.title}</h3>
              <p className="feed-meta">
                {headerParticipant?.displayName && (
                  <>
                    With{' '}
                    {headerParticipant.id ? (
                      <Link className="text-link" to={`/users/${headerParticipant.id}`}>{headerParticipant.displayName}</Link>
                    ) : (
                      headerParticipant.displayName
                    )}
                    {' '}•{' '}
                  </>
                )}
                {listing.status}
              </p>
            </div>
            <span className={`badge ${listing.status === 'ACTIVE' ? 'success' : 'neutral'}`}>{listing.status}</span>
          </div>

          <div className="conversation-timeline">
            {conversation?.messages?.length ? (
              conversation.messages.map(message => (
                <article
                  key={message.id}
                  className={`message-bubble${message.mine ? ' mine' : ''}`}
                >
                  <div className="message-bubble-meta">
                    <span>{message.sender.displayName}</span>
                    <small>{formatDate(message.createdAt)}</small>
                  </div>
                  <p>{message.body}</p>
                </article>
              ))
            ) : (
              <p className="conversation-placeholder">
                {composeListing ? 'Start the conversation about this product.' : 'No messages yet.'}
              </p>
            )}
          </div>

          <form className="conversation-form" onSubmit={sendMessage}>
            {conversation?.readOnly && (
              <p className="error">This listing is no longer active, so the conversation is now read-only.</p>
            )}
            <textarea
              value={draft}
              onChange={event => setDraft(event.target.value)}
              rows={4}
              placeholder="Write a message about the product..."
              disabled={loading || conversation?.readOnly}
            />
            <button type="submit" disabled={loading || !draft.trim() || conversation?.readOnly}>
              {loading ? 'Sending...' : 'Send message'}
            </button>
          </form>
        </>
      ) : (
        <div className="conversation-empty">
          <p>{fallback}</p>
        </div>
      )}
    </section>
  )
}
