import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api'

const RECENT_EMOJIS_KEY = 'unimart_recent_emojis'
const RECENT_EMOJIS_LIMIT = 8

const EMOJI_CATEGORIES = [
  {
    id: 'smileys',
    label: 'Smileys & People',
    nav: '🙂',
    emojis: [
      { value: '🥰', name: 'smiling face with hearts', keywords: ['love', 'happy', 'smile'] },
      { value: '🤓', name: 'nerd face', keywords: ['glasses', 'study', 'smart'] },
      { value: '😚', name: 'kissing face', keywords: ['kiss', 'love'] },
      { value: '😭', name: 'loudly crying face', keywords: ['cry', 'sad'] },
      { value: '😌', name: 'relieved face', keywords: ['calm', 'soft'] },
      { value: '😀', name: 'grinning face', keywords: ['happy', 'smile'] },
      { value: '😄', name: 'grinning face with smiling eyes', keywords: ['joy', 'smile'] },
      { value: '😁', name: 'beaming face', keywords: ['cheese', 'happy'] },
      { value: '😆', name: 'grinning squinting face', keywords: ['laugh', 'funny'] },
      { value: '🥹', name: 'face holding back tears', keywords: ['aww', 'cry'] },
      { value: '😍', name: 'smiling face with heart eyes', keywords: ['love', 'heart'] },
      { value: '😘', name: 'face blowing a kiss', keywords: ['kiss', 'love'] },
      { value: '🙂', name: 'slightly smiling face', keywords: ['gentle', 'smile'] },
      { value: '😊', name: 'smiling face with smiling eyes', keywords: ['blush', 'happy'] }
    ]
  },
  {
    id: 'animals',
    label: 'Animals & Nature',
    nav: '🐾',
    emojis: [
      { value: '🐶', name: 'dog face', keywords: ['pet', 'puppy'] },
      { value: '🐱', name: 'cat face', keywords: ['pet', 'kitty'] },
      { value: '🦊', name: 'fox', keywords: ['animal'] },
      { value: '🐼', name: 'panda', keywords: ['animal'] },
      { value: '🐻', name: 'bear', keywords: ['animal'] },
      { value: '🐰', name: 'rabbit', keywords: ['bunny'] },
      { value: '🌿', name: 'herb', keywords: ['leaf', 'green'] },
      { value: '🌸', name: 'cherry blossom', keywords: ['flower', 'pink'] }
    ]
  },
  {
    id: 'food',
    label: 'Food & Drink',
    nav: '🍔',
    emojis: [
      { value: '☕', name: 'hot beverage', keywords: ['coffee', 'tea'] },
      { value: '🍕', name: 'pizza', keywords: ['food'] },
      { value: '🍔', name: 'hamburger', keywords: ['food'] },
      { value: '🍟', name: 'fries', keywords: ['food'] },
      { value: '🍜', name: 'steaming bowl', keywords: ['ramen', 'noodles'] },
      { value: '🧋', name: 'bubble tea', keywords: ['drink', 'boba'] },
      { value: '🍩', name: 'doughnut', keywords: ['dessert'] },
      { value: '🍓', name: 'strawberry', keywords: ['fruit'] }
    ]
  },
  {
    id: 'activities',
    label: 'Activities',
    nav: '🏀',
    emojis: [
      { value: '⚽', name: 'soccer ball', keywords: ['sport'] },
      { value: '🏀', name: 'basketball', keywords: ['sport'] },
      { value: '🎮', name: 'video game', keywords: ['game'] },
      { value: '🎵', name: 'musical note', keywords: ['music'] },
      { value: '🎉', name: 'party popper', keywords: ['celebration'] },
      { value: '🔥', name: 'fire', keywords: ['lit', 'hot'] },
      { value: '💯', name: 'hundred points', keywords: ['perfect'] },
      { value: '✨', name: 'sparkles', keywords: ['shine'] }
    ]
  },
  {
    id: 'travel',
    label: 'Travel & Places',
    nav: '🚗',
    emojis: [
      { value: '🚗', name: 'car', keywords: ['drive'] },
      { value: '🚲', name: 'bicycle', keywords: ['bike'] },
      { value: '✈️', name: 'airplane', keywords: ['travel'] },
      { value: '🚌', name: 'bus', keywords: ['travel'] },
      { value: '🏡', name: 'house with garden', keywords: ['home'] },
      { value: '🌆', name: 'cityscape at dusk', keywords: ['city'] },
      { value: '🌙', name: 'crescent moon', keywords: ['night'] },
      { value: '☀️', name: 'sun', keywords: ['weather'] }
    ]
  },
  {
    id: 'objects',
    label: 'Objects',
    nav: '💡',
    emojis: [
      { value: '📚', name: 'books', keywords: ['study'] },
      { value: '💻', name: 'laptop', keywords: ['computer'] },
      { value: '📱', name: 'mobile phone', keywords: ['phone'] },
      { value: '🎁', name: 'gift', keywords: ['present'] },
      { value: '📦', name: 'package', keywords: ['box'] },
      { value: '🧸', name: 'teddy bear', keywords: ['toy'] },
      { value: '💡', name: 'light bulb', keywords: ['idea'] },
      { value: '🔑', name: 'key', keywords: ['lock'] }
    ]
  },
  {
    id: 'symbols',
    label: 'Symbols',
    nav: '🔣',
    emojis: [
      { value: '❤️', name: 'red heart', keywords: ['love'] },
      { value: '💬', name: 'speech balloon', keywords: ['chat'] },
      { value: '👍', name: 'thumbs up', keywords: ['yes', 'ok'] },
      { value: '👀', name: 'eyes', keywords: ['look'] },
      { value: '🙏', name: 'folded hands', keywords: ['thanks', 'please'] },
      { value: '✅', name: 'check mark button', keywords: ['done'] },
      { value: '❗', name: 'red exclamation mark', keywords: ['alert'] },
      { value: '➕', name: 'plus', keywords: ['add'] }
    ]
  }
]

const SEARCHABLE_EMOJIS = EMOJI_CATEGORIES.flatMap(category =>
  category.emojis.map(emoji => ({ ...emoji, categoryId: category.id }))
)

function formatDate(value) {
  if (!value) return ''
  return new Date(value).toLocaleString([], {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  })
}

function formatHandle(participant) {
  if (!participant?.displayName) return '@user'
  return `@${participant.displayName.toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_+|_+$/g, '') || 'user'}`
}

function SmileIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="M9 14.2c.7.8 1.7 1.3 3 1.3s2.3-.5 3-1.3" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <circle cx="9" cy="10" r="1" fill="currentColor" />
      <circle cx="15" cy="10" r="1" fill="currentColor" />
    </svg>
  )
}

function ImageIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <rect x="4" y="5" width="16" height="14" rx="3" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="9" cy="10" r="1.4" fill="currentColor" />
      <path d="m7 16 3.2-3.2a1 1 0 0 1 1.4 0l1.7 1.7 1.3-1.3a1 1 0 0 1 1.4 0L18 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function SendIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M20 4 9 15"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
      />
      <path
        d="m20 4-7 16-2.6-6.4L4 11z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function SearchIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="11" cy="11" r="6.5" fill="none" stroke="currentColor" strokeWidth="1.8" />
      <path d="m16 16 4 4" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="m7 7 10 10M17 7 7 17" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  )
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
    return (
      <div className="message-listing-thumb placeholder">
        <span>{listing?.title?.[0] || '?'}</span>
      </div>
    )
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
  const [emojiOpen, setEmojiOpen] = useState(false)
  const [pendingAttachment, setPendingAttachment] = useState(null)
  const [uploadingAttachment, setUploadingAttachment] = useState(false)

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
        setSelectedConversationId(current => (
          current && response.conversations.some(item => item.id === current) ? current : null
        ))
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
    setPendingAttachment(null)
  }, [activeTab, selectedConversationId, composeListingId])

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
    setError('')
    if (nextTab === 'seller') {
      setSelectedConversationId(null)
      setSelectedSellerListingId(null)
      return
    }
    setSelectedSellerListingId(null)
    setSelectedConversationId(null)
  }

  async function sendMessage(event) {
    event.preventDefault()
    if (!draft.trim() && !pendingAttachment) return

    setLoading(true)
    setError('')

    try {
      let response
      if (composeListing && !selectedConversationId) {
        response = await api(`/listings/${composeListing.id}/messages`, {
          method: 'POST',
          body: JSON.stringify({
            body: draft.trim(),
            attachment: pendingAttachment
              ? {
                  storageKey: pendingAttachment.storageKey,
                  contentType: pendingAttachment.contentType,
                  fileSize: pendingAttachment.fileSize,
                  mediaType: pendingAttachment.mediaType
                }
              : null
          })
        })
        const nextParams = new URLSearchParams(searchParams)
        nextParams.delete('compose')
        nextParams.set('view', 'buyer')
        setSearchParams(nextParams)
      } else {
        response = await api(`/messages/conversations/${selectedConversationId}/messages`, {
          method: 'POST',
          body: JSON.stringify({
            body: draft.trim(),
            attachment: pendingAttachment
              ? {
                  storageKey: pendingAttachment.storageKey,
                  contentType: pendingAttachment.contentType,
                  fileSize: pendingAttachment.fileSize,
                  mediaType: pendingAttachment.mediaType
                }
              : null
          })
        })
      }

      setConversation(response)
      setSelectedConversationId(response.conversationId)
      setComposeListing(null)
      setDraft('')
      setEmojiOpen(false)
      setPendingAttachment(null)
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
  const sellerSelectedListing = sellerListings.find(item => item.listing.id === selectedSellerListingId)?.listing || null
  const hasSellerInquiries = sellerListings.length > 0
  const showSellerThreadPlaceholder = activeTab === 'seller' && !selectedSellerListingId
  const showSellerConversationPanel = activeTab !== 'seller' || (selectedSellerListingId && selectedConversationId)
  const conversationFallback = activeTab === 'seller'
    ? !selectedSellerListingId
      ? null
      : !selectedConversationId
        ? 'Choose a buyer to reply.'
        : null
    : !selectedConversationId && !composeListing
      ? 'Choose a product inquiry or start one from a listing.'
      : null

  function appendEmoji(emoji) {
    setDraft(current => `${current}${emoji}`)
    setEmojiOpen(false)
  }

  function handleComposerKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      if (!loading && !uploadingAttachment && (draft.trim() || pendingAttachment) && !conversation?.readOnly) {
        sendMessage(event)
      }
    }
  }

  async function uploadChatImage(file) {
    if (!file) return
    setUploadingAttachment(true)
    setError('')

    try {
      const token = localStorage.getItem('unimart-token')
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
      if (payload.mediaType !== 'IMAGE') {
        throw new Error('Only images can be shared in chat right now.')
      }
      setPendingAttachment({
        storageKey: payload.storageKey,
        contentType: payload.contentType,
        fileSize: payload.fileSize,
        mediaType: payload.mediaType,
        url: payload.url
      })
    } catch (err) {
      setError(err.message)
    } finally {
      setUploadingAttachment(false)
    }
  }

  return (
    <div className="stack">
      <section className="panel messages-shell">
        <div className="messages-toolbar">
          <span className="messages-toolbar-label">Messages:</span>
          <div className="messages-tab-row">
            <button
              type="button"
              className={`ghost messages-tab${activeTab === 'seller' ? ' active' : ''}${sellerUnreadTotal > 0 ? ' has-unread' : ''}`}
              onClick={() => switchTab('seller')}
            >
              Seller inbox
              {sellerUnreadTotal > 0 && <span className="messages-tab-dot" aria-hidden="true" />}
            </button>
            <button
              type="button"
              className={`ghost messages-tab${activeTab === 'buyer' ? ' active' : ''}${buyerUnreadTotal > 0 ? ' has-unread' : ''}`}
              onClick={() => switchTab('buyer')}
            >
              Sent inquiries
              {buyerUnreadTotal > 0 && <span className="messages-tab-dot" aria-hidden="true" />}
            </button>
          </div>
        </div>

        {error && <p className="error">{error}</p>}

        {activeTab === 'seller' ? (
          <div className="messages-layout seller-layout">
            <aside className="messages-panel messages-selection-panel">
              <div className="messages-panel-header">
                <p className="eyebrow">Products</p>
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
                      setConversation(null)
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

            {hasSellerInquiries && (
              <section className="messages-panel messages-selection-panel">
                <div className="messages-panel-header">
                  <p className="eyebrow">Users</p>
                  {!sellerSelectedListing && <span className="messages-panel-caption">No product selected.</span>}
                </div>
                {showSellerThreadPlaceholder ? (
                  <div className="messages-empty-panel">
                    <p>No product selected.</p>
                  </div>
                ) : (
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
                  </div>
                )}
              </section>
            )}

            {hasSellerInquiries && showSellerConversationPanel && (
              <ConversationPanel
                conversation={conversation}
                draft={draft}
                setDraft={setDraft}
                sendMessage={sendMessage}
                loading={loading}
                fallback={conversationFallback}
                emojiOpen={emojiOpen}
                setEmojiOpen={setEmojiOpen}
                appendEmoji={appendEmoji}
                onComposerKeyDown={handleComposerKeyDown}
                pendingAttachment={pendingAttachment}
                clearPendingAttachment={() => setPendingAttachment(null)}
                uploadingAttachment={uploadingAttachment}
                onSelectAttachment={uploadChatImage}
              />
            )}
          </div>
        ) : (
          <div className="messages-layout buyer-layout">
            <aside className="messages-panel messages-selection-panel">
              <div className="messages-panel-header">
                <p className="eyebrow">Sent inquiries</p>
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
              fallback={conversationFallback}
              emojiOpen={emojiOpen}
              setEmojiOpen={setEmojiOpen}
              appendEmoji={appendEmoji}
              onComposerKeyDown={handleComposerKeyDown}
              pendingAttachment={pendingAttachment}
              clearPendingAttachment={() => setPendingAttachment(null)}
              uploadingAttachment={uploadingAttachment}
              onSelectAttachment={uploadChatImage}
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
  fallback,
  emojiOpen,
  setEmojiOpen,
  appendEmoji,
  onComposerKeyDown,
  pendingAttachment,
  clearPendingAttachment,
  uploadingAttachment,
  onSelectAttachment
}) {
  const listing = conversation?.listing || composeListing || selectedBuyerListing?.listing
  const headerParticipant = conversation?.seller && conversation?.buyer
    ? (conversation.messages?.find(message => !message.mine)?.sender || conversation.seller)
    : selectedBuyerListing?.seller
  const listingContextLabel = listing ? 'Regarding listing' : null
  const timelineRef = useRef(null)
  const fileInputRef = useRef(null)
  const [emojiQuery, setEmojiQuery] = useState('')
  const [activeEmojiCategory, setActiveEmojiCategory] = useState('smileys')
  const [recentEmojis, setRecentEmojis] = useState([])

  useEffect(() => {
    try {
      const stored = window.localStorage.getItem(RECENT_EMOJIS_KEY)
      if (stored) {
        const parsed = JSON.parse(stored)
        if (Array.isArray(parsed)) {
          setRecentEmojis(parsed.filter(value => typeof value === 'string'))
        }
      }
    } catch {
      setRecentEmojis([])
    }
  }, [])

  useEffect(() => {
    if (!emojiOpen) {
      setEmojiQuery('')
      setActiveEmojiCategory('smileys')
    }
  }, [emojiOpen])

  useEffect(() => {
    if (!timelineRef.current) return
    timelineRef.current.scrollTop = timelineRef.current.scrollHeight
  }, [conversation?.conversationId, conversation?.messages?.length])

  const emojiSections = useMemo(() => {
    const query = emojiQuery.trim().toLowerCase()
    if (query) {
      const matches = SEARCHABLE_EMOJIS.filter(emoji =>
        emoji.name.includes(query) || emoji.keywords.some(keyword => keyword.includes(query))
      )
      return matches.length ? [{ id: 'search', label: 'Search results', emojis: matches }] : []
    }

    const sections = []
    if (recentEmojis.length) {
      sections.push({
        id: 'recent',
        label: 'Recently Used',
        emojis: recentEmojis.map(value => ({ value, name: value, keywords: [] }))
      })
    }

    const activeCategory = EMOJI_CATEGORIES.find(category => category.id === activeEmojiCategory) || EMOJI_CATEGORIES[0]
    sections.push(activeCategory)
    return sections
  }, [activeEmojiCategory, emojiQuery, recentEmojis])

  function handleEmojiSelect(emoji) {
    const nextRecent = [emoji, ...recentEmojis.filter(item => item !== emoji)].slice(0, RECENT_EMOJIS_LIMIT)
    setRecentEmojis(nextRecent)
    try {
      window.localStorage.setItem(RECENT_EMOJIS_KEY, JSON.stringify(nextRecent))
    } catch {
      // Ignore storage failures and keep the picker working.
    }
    appendEmoji(emoji)
  }

  return (
    <section className="messages-panel conversation-panel">
      {listing ? (
        <>
          <div className="messages-panel-header conversation-header conversation-header-shell">
            <div className="conversation-header-identity">
              <Avatar participant={headerParticipant} />
              <div className="conversation-header-copy">
                <h3>
                  {headerParticipant?.id ? (
                    <Link className="text-link conversation-header-link" to={`/users/${headerParticipant.id}`}>
                      {headerParticipant.displayName}
                    </Link>
                  ) : (
                    headerParticipant?.displayName || listing.title
                  )}
                </h3>
                <p>{formatHandle(headerParticipant)}</p>
              </div>
            </div>
          </div>

          <div className="conversation-context-strip">
            <div className="conversation-context-pill">
              <span>{listingContextLabel}</span>
              <strong>{listing.title}</strong>
            </div>
            <span className={`badge ${listing.status === 'ACTIVE' ? 'success' : 'neutral'}`}>{listing.status}</span>
          </div>

          <div className="conversation-timeline" ref={timelineRef}>
            <div className="conversation-timeline-content">
              {conversation?.messages?.length ? (
                conversation.messages.map(message => (
                <article
                  key={message.id}
                  className={`message-bubble${message.mine ? ' mine' : ''}`}
                >
                    {message.attachment?.url && message.attachment.mediaType === 'IMAGE' && (
                      <img
                        className="message-bubble-image"
                        src={`http://localhost:8080${message.attachment.url}`}
                        alt={message.body ? `Shared by ${message.sender.displayName}` : 'Shared image'}
                      />
                    )}
                    <div className="message-bubble-meta">
                      <small>{formatDate(message.createdAt)}</small>
                    </div>
                    {message.body && <p>{message.body}</p>}
                  </article>
                ))
              ) : (
                <p className="conversation-placeholder">
                  {composeListing ? 'Start the conversation about this product.' : 'No messages yet.'}
                </p>
              )}
            </div>
          </div>

          <form className="conversation-form" onSubmit={sendMessage}>
            {conversation?.readOnly && (
              <p className="error">This listing is no longer active, so the conversation is now read-only.</p>
            )}
            {pendingAttachment && (
              <div className="conversation-attachment-preview">
                <img src={`http://localhost:8080${pendingAttachment.url}`} alt="Pending upload" />
                <div className="conversation-attachment-copy">
                  <strong>Image ready to send</strong>
                  <span>{Math.max(1, Math.round(pendingAttachment.fileSize / 1024))} KB</span>
                </div>
                <button
                  type="button"
                  className="ghost conversation-attachment-remove"
                  onClick={clearPendingAttachment}
                  aria-label="Remove image"
                >
                  <CloseIcon />
                </button>
              </div>
            )}
            <div className="conversation-composer-shell">
              <button
                type="button"
                className="ghost composer-tool-button conversation-inline-tool"
                onClick={() => setEmojiOpen(current => !current)}
                disabled={conversation?.readOnly}
                aria-label="Add emoji"
              >
                <SmileIcon />
              </button>
              <textarea
                value={draft}
                onChange={event => setDraft(event.target.value)}
                onKeyDown={onComposerKeyDown}
                rows={2}
                placeholder={`Write a message to ${headerParticipant?.displayName || 'the seller'}...`}
                disabled={loading || uploadingAttachment || conversation?.readOnly}
              />
              <button
                type="button"
                className="ghost composer-tool-button conversation-inline-tool"
                onClick={() => fileInputRef.current?.click()}
                disabled={loading || uploadingAttachment || conversation?.readOnly}
                aria-label="Attach image"
              >
                <ImageIcon />
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="sr-only"
                onChange={event => {
                  const file = event.target.files?.[0]
                  onSelectAttachment(file || null)
                  event.target.value = ''
                }}
                disabled={loading || uploadingAttachment || conversation?.readOnly}
              />
            </div>
            <div className="conversation-composer-footer">
              <span className="composer-helper-text">
                {uploadingAttachment ? 'Uploading image...' : 'Enter sends. Shift+Enter adds a new line.'}
              </span>
              <button
                type="submit"
                className="conversation-send-button"
                disabled={loading || uploadingAttachment || (!draft.trim() && !pendingAttachment) || conversation?.readOnly}
              >
                <SendIcon />
                <span>{loading ? 'Sending...' : 'Send'}</span>
              </button>
            </div>
            {emojiOpen && !conversation?.readOnly && (
              <div className="emoji-picker">
                <label className="emoji-search">
                  <span className="emoji-search-icon" aria-hidden="true">
                    <SearchIcon />
                  </span>
                  <input
                    type="text"
                    value={emojiQuery}
                    onChange={event => setEmojiQuery(event.target.value)}
                    placeholder="Search emoji"
                  />
                </label>
                <div className="emoji-picker-body">
                  {emojiSections.length ? (
                    emojiSections.map(section => (
                      <div key={section.id} className="emoji-section">
                        <p className="emoji-section-label">{section.label}</p>
                        <div className="emoji-grid">
                          {section.emojis.map(emoji => (
                            <button
                              key={`${section.id}-${emoji.value}`}
                              type="button"
                              className="ghost emoji-button"
                              onClick={() => handleEmojiSelect(emoji.value)}
                              title={emoji.name}
                            >
                              {emoji.value}
                            </button>
                          ))}
                        </div>
                      </div>
                    ))
                  ) : (
                    <p className="emoji-empty">No emojis match that search.</p>
                  )}
                </div>
                <div className="emoji-picker-nav">
                  {EMOJI_CATEGORIES.map(category => (
                    <button
                      key={category.id}
                      type="button"
                      className={`ghost emoji-nav-button${activeEmojiCategory === category.id ? ' active' : ''}`}
                      onClick={() => setActiveEmojiCategory(category.id)}
                      aria-label={category.label}
                    >
                      {category.nav}
                    </button>
                  ))}
                </div>
              </div>
            )}
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
