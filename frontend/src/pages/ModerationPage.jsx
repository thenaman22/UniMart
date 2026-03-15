import { useEffect, useState } from 'react'
import { api } from '../api'

export function ModerationPage({ user, communities }) {
  const modCommunity = communities.find(item => item.role !== 'MEMBER')
  const [requests, setRequests] = useState([])
  const [reports, setReports] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user || !modCommunity) return
    Promise.all([
      api(`/moderation/${modCommunity.communityId}/requests`),
      api(`/moderation/${modCommunity.communityId}/reports`)
    ])
      .then(([pendingRequests, openReports]) => {
        setRequests(pendingRequests)
        setReports(openReports)
      })
      .catch(err => setError(err.message))
  }, [user, modCommunity])

  async function updateMembership(membershipId, approve) {
    try {
      await api(`/moderation/memberships/${membershipId}?approve=${approve}`, { method: 'PATCH' })
      setRequests(current => current.filter(item => item.membershipId !== membershipId))
    } catch (err) {
      setError(err.message)
    }
  }

  if (!user) {
    return <section className="panel"><p>Please sign in first.</p></section>
  }

  if (!modCommunity) {
    return <section className="panel"><p>You do not have moderator access in any community yet.</p></section>
  }

  return (
    <div className="dashboard-grid">
      <section className="panel">
        <h1>Moderation queue</h1>
        {error && <p className="error">{error}</p>}
        <div className="stack">
          {requests.map(request => (
            <article key={request.membershipId} className="queue-card">
              <strong>{request.requesterName}</strong>
              <p>{request.requesterEmail}</p>
              <p>Status: {request.status}</p>
              <div className="button-row">
                <button onClick={() => updateMembership(request.membershipId, true)}>Approve</button>
                <button className="ghost" onClick={() => updateMembership(request.membershipId, false)}>Reject</button>
              </div>
            </article>
          ))}
          {requests.length === 0 && <p>No pending membership requests.</p>}
        </div>
      </section>

      <section className="panel">
        <h2>Reports</h2>
        <div className="stack">
          {reports.map(report => (
            <article key={report.id} className="queue-card">
              <strong>Listing #{report.listingId}</strong>
              <p>{report.reason}</p>
              <small>Reported by {report.reporterName}</small>
            </article>
          ))}
          {reports.length === 0 && <p>No open reports.</p>}
        </div>
      </section>
    </div>
  )
}
