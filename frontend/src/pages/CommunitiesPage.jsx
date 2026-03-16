import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'

export function CommunitiesPage({ user, communities }) {
  const [communityDirectory, setCommunityDirectory] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    api('/communities/discover')
      .then(setCommunityDirectory)
      .catch(err => setError(err.message))
  }, [])

  const membershipsById = useMemo(
    () => new Map(communities.map(community => [String(community.communityId), community])),
    [communities]
  )

  return (
    <section className="panel">
      <div className="panel-header panel-stack-mobile">
        <div>
          <p className="eyebrow">Community directory</p>
          <h1>Explore all communities</h1>
          <p>Anyone can browse the directory. Only approved members can open listings and post inside a community.</p>
        </div>
        {!user && <Link className="button-link" to="/auth">Sign in to join</Link>}
      </div>

      {error && <p className="error">{error}</p>}

      <div className="community-grid">
        {communityDirectory.map(community => {
          const membership = membershipsById.get(String(community.id))
          return (
            <article key={community.id} className="community-card">
              <div className="community-card-top">
                <span className="badge neutral">{community.privateCommunity ? 'Closed marketplace' : 'Open marketplace'}</span>
                {membership && <span className="badge success">{membership.role}</span>}
              </div>
              <h3>{community.name}</h3>
              <p>{community.description}</p>
              <div className="button-row wrap-row">
                <Link className="button-link dark" to={`/communities/${community.id}`}>View community</Link>
                {!membership && user && (
                  <>
                    <button onClick={async () => {
                      try {
                        await api(`/communities/${community.id}/join-by-domain`, { method: 'POST' })
                        window.location.reload()
                      } catch (err) {
                        setError(err.message)
                      }
                    }}>Join by email</button>
                    <button className="ghost" onClick={async () => {
                      try {
                        await api(`/communities/${community.id}/request`, { method: 'POST' })
                        setError(`Membership request sent for ${community.name}.`)
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
      </div>
    </section>
  )
}
