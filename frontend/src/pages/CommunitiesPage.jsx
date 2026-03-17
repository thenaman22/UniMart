import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { api } from '../api'

const POSTING_POLICIES = [
  { value: 'ALL_MEMBERS_CAN_POST', label: 'All members can post' },
  { value: 'APPROVED_SELLERS_ONLY', label: 'Approved sellers only' },
  { value: 'CREATOR_ONLY', label: 'Creator only' }
]

export function CommunitiesPage({ user, communities, onCommunitiesChanged }) {
  const location = useLocation()
  const [communityDirectory, setCommunityDirectory] = useState([])
  const [error, setError] = useState('')
  const [status, setStatus] = useState('')
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState({
    name: '',
    description: '',
    postingPolicy: 'ALL_MEMBERS_CAN_POST'
  })

  async function loadDirectory() {
    const response = await api('/communities/discover')
    setCommunityDirectory(response)
  }

  useEffect(() => {
    loadDirectory()
      .catch(err => setError(err.message))
  }, [])

  const membershipsById = useMemo(() => new Map(communities.map(community => [String(community.communityId), community])), [communities])
  const ownedCount = communities.filter(community => community.isCreator).length
  const atCommunityLimit = ownedCount >= 5
  const searchQuery = useMemo(() => new URLSearchParams(location.search).get('q')?.trim().toLowerCase() || '', [location.search])
  const filteredCommunities = useMemo(() => {
    if (!searchQuery) {
      return communityDirectory
    }
    return communityDirectory.filter(community => (
      community.name.toLowerCase().includes(searchQuery) ||
      community.description.toLowerCase().includes(searchQuery) ||
      community.postingPolicyLabel.toLowerCase().includes(searchQuery)
    ))
  }, [communityDirectory, searchQuery])

  async function createCommunity(event) {
    event.preventDefault()
    setError('')
    setStatus('')
    setCreating(true)
    try {
      const response = await api('/communities', {
        method: 'POST',
        body: JSON.stringify(form)
      })
      setForm({
        name: '',
        description: '',
        postingPolicy: 'ALL_MEMBERS_CAN_POST'
      })
      await Promise.all([
        loadDirectory(),
        onCommunitiesChanged?.()
      ])
      setStatus(`Created ${response.name}.`)
      setShowCreateForm(false)
    } catch (err) {
      setError(err.message)
    } finally {
      setCreating(false)
    }
  }

  return (
    <section className="panel">
      <div className="panel-header panel-stack-mobile">
        <div>
          <p className="eyebrow">Community directory</p>
          <h1>Explore all communities</h1>
          <p>Anyone can browse the directory. Only approved members can open listings and post inside a community.</p>
        </div>
        {user ? (
          <button
            type="button"
            className={`community-create-toggle${showCreateForm ? ' open' : ''}`}
            onClick={() => {
              setError('')
              setStatus('')
              setShowCreateForm(current => !current)
            }}
          >
            {showCreateForm ? 'Hide community form' : '+ Create your own community'}
          </button>
        ) : (
          <Link className="button-link" to="/auth">Sign in to join</Link>
        )}
      </div>

      {user && showCreateForm && (
        <section className="community-create-panel">
          <div className="community-create-header">
            <div className="community-create-copy">
              <p className="eyebrow">Create your own</p>
              <h2>Start a community</h2>
              <p>You currently own {ownedCount} of 5 allowed communities.</p>
            </div>
            <span className={`community-create-count${atCommunityLimit ? ' limit' : ''}`}>{ownedCount}/5</span>
          </div>

          <form className="stack community-create-form" onSubmit={createCommunity}>
            <input
              value={form.name}
              onChange={event => setForm(current => ({ ...current, name: event.target.value }))}
              placeholder="Community name"
              maxLength={120}
              required
              disabled={creating || atCommunityLimit}
            />
            <textarea
              value={form.description}
              onChange={event => setForm(current => ({ ...current, description: event.target.value }))}
              placeholder="What is this community for?"
              rows="4"
              maxLength={1000}
              required
              disabled={creating || atCommunityLimit}
            />
            <select
              value={form.postingPolicy}
              onChange={event => setForm(current => ({ ...current, postingPolicy: event.target.value }))}
              disabled={creating || atCommunityLimit}
            >
              {POSTING_POLICIES.map(option => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
            <div className="button-row wrap-row community-create-actions">
              <button type="submit" className="community-create-submit" disabled={creating || atCommunityLimit}>
                {creating ? 'Creating...' : 'Create community'}
              </button>
              {atCommunityLimit && <span className="feed-meta">Delete one of your communities to free a slot.</span>}
            </div>
          </form>
        </section>
      )}

      {status && <p className="success">{status}</p>}
      {error && <p className="error">{error}</p>}

      <div className="community-grid">
        {filteredCommunities.map(community => {
          const membership = membershipsById.get(String(community.id))
          return (
            <article key={community.id} className="community-card">
              <div className="community-card-top">
                <span className="badge neutral">{community.privateCommunity ? 'Closed marketplace' : 'Open marketplace'}</span>
                {membership && <span className="badge success">{membership.roleLabel || membership.role}</span>}
              </div>
              <h3>{community.name}</h3>
              <p>{community.description}</p>
              <p className="feed-meta">{community.postingPolicyLabel}</p>
              <div className="button-row wrap-row">
                <Link className="button-link dark" to={`/communities/${community.id}`}>View community</Link>
                {!membership && user && (
                  <>
                    <button onClick={async () => {
                      try {
                        await api(`/communities/${community.id}/join-by-domain`, { method: 'POST' })
                        await Promise.all([loadDirectory(), onCommunitiesChanged?.()])
                      } catch (err) {
                        setError(err.message)
                      }
                    }}>Join by email</button>
                    <button className="ghost" onClick={async () => {
                      try {
                        await api(`/communities/${community.id}/request`, { method: 'POST' })
                        setStatus(`Membership request sent for ${community.name}.`)
                      } catch (err) {
                        setError(err.message)
                      }
                    }}>Request access</button>
                  </>
                )}
              </div>
            </article>
          )
        })}
        {filteredCommunities.length === 0 && (
          <article className="panel">
            <p>No communities match that search yet.</p>
          </article>
        )}
      </div>
    </section>
  )
}
