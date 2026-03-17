import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'

export function ModerationPage({ user, communities }) {
  const modCommunity = communities.find(item => item.role !== 'MEMBER')
  const [requests, setRequests] = useState([])
  const [members, setMembers] = useState([])
  const [reports, setReports] = useState([])
  const [error, setError] = useState('')
  const [busyMembershipId, setBusyMembershipId] = useState(null)
  const [memberSearch, setMemberSearch] = useState('')

  useEffect(() => {
    if (!user || !modCommunity) return
    Promise.all([
      api(`/moderation/${modCommunity.communityId}/requests`),
      api(`/moderation/${modCommunity.communityId}/members`),
      api(`/moderation/${modCommunity.communityId}/reports`)
    ])
      .then(([pendingRequests, activeMembers, openReports]) => {
        setRequests(pendingRequests)
        setMembers(activeMembers)
        setReports(openReports)
      })
      .catch(err => setError(err.message))
  }, [user, modCommunity])

  async function updateMembership(membershipId, approve) {
    setBusyMembershipId(membershipId)
    try {
      await api(`/moderation/memberships/${membershipId}?approve=${approve}`, { method: 'PATCH' })
      if (approve) {
        const approved = requests.find(item => item.membershipId === membershipId)
        setRequests(current => current.filter(item => item.membershipId !== membershipId))
        if (approved) {
          setMembers(current => {
            const nextMembers = [
              ...current,
              {
                membershipId: approved.membershipId,
                communityId: approved.communityId,
                communityName: approved.communityName,
                userId: approved.userId,
                memberName: approved.requesterName,
                memberEmail: approved.requesterEmail,
                status: 'ACTIVE',
                role: approved.role
              }
            ]
            return nextMembers.sort((first, second) => first.memberName.localeCompare(second.memberName))
          })
        }
      } else if (requests.some(item => item.membershipId === membershipId)) {
        setRequests(current => current.filter(item => item.membershipId !== membershipId))
      } else {
        setMembers(current => current.filter(item => item.membershipId !== membershipId))
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyMembershipId(null)
    }
  }

  const visibleMembers = useMemo(() => {
    const query = memberSearch.trim().toLowerCase()
    if (!query) {
      return members
    }

    return members.filter(member => (
      member.memberName.toLowerCase().includes(query) ||
      member.memberEmail.toLowerCase().includes(query) ||
      member.role.toLowerCase().includes(query)
    ))
  }, [memberSearch, members])

  if (!user) {
    return <section className="panel"><p>Please sign in first.</p></section>
  }

  if (!modCommunity) {
    return <section className="panel"><p>You do not have moderator access in any community yet.</p></section>
  }

  return (
    <div className="moderation-shell">
      <div className="moderation-title-pill">{modCommunity.name}</div>
      {error && <p className="error">{error}</p>}

      <div className="moderation-layout">
        <div className="moderation-left-column">
          <section className="panel moderation-panel">
            <div className="moderation-panel-header">
              <h2>Pending requests</h2>
              <span className="badge neutral">{requests.length}</span>
            </div>
            <div className="moderation-panel-scroll">
              <div className="stack">
                {requests.map(request => (
                  <article key={request.membershipId} className="queue-card">
                    <strong>{request.requesterName}</strong>
                    <p>{request.requesterEmail}</p>
                    <p>Status: {request.status}</p>
                    <div className="button-row">
                      <button disabled={busyMembershipId === request.membershipId} onClick={() => updateMembership(request.membershipId, true)}>
                        {busyMembershipId === request.membershipId ? 'Working...' : 'Approve'}
                      </button>
                      <button className="ghost" disabled={busyMembershipId === request.membershipId} onClick={() => updateMembership(request.membershipId, false)}>
                        Reject
                      </button>
                    </div>
                  </article>
                ))}
                {requests.length === 0 && <p className="moderation-empty">No pending membership requests.</p>}
              </div>
            </div>
          </section>

          <section className="panel moderation-panel">
            <div className="moderation-panel-header">
              <h2>Reports</h2>
              <span className="badge neutral">{reports.length}</span>
            </div>
            <div className="moderation-panel-scroll">
              <div className="stack">
                {reports.map(report => (
                  <article key={report.id} className="queue-card">
                    <strong>Listing #{report.listingId}</strong>
                    <p>{report.reason}</p>
                    <small>Reported by {report.reporterName}</small>
                  </article>
                ))}
                {reports.length === 0 && <p className="moderation-empty">No open reports.</p>}
              </div>
            </div>
          </section>
        </div>

        <section className="panel moderation-panel moderation-members-panel">
          <div className="moderation-panel-header">
            <div className="moderation-panel-header-inline">
              <h2>Member list</h2>
              <div className="moderation-searchbar moderation-searchbar-inline">
                <input
                  value={memberSearch}
                  onChange={event => setMemberSearch(event.target.value)}
                  placeholder="Search members"
                  aria-label="Search community members"
                />
              </div>
            </div>
            <span className="badge neutral">{visibleMembers.length}</span>
          </div>
          <div className="moderation-panel-scroll">
            <div className="stack">
              {visibleMembers.map(member => (
                <article key={member.membershipId} className="queue-card">
                  <strong>{member.memberName}</strong>
                  <p>{member.memberEmail}</p>
                  <p>{member.role}</p>
                  {member.userId === user.id ? (
                    <p className="feed-meta">You manage this community.</p>
                  ) : (
                    <div className="button-row">
                      <button
                        className="ghost"
                        disabled={busyMembershipId === member.membershipId}
                        onClick={() => updateMembership(member.membershipId, false)}
                      >
                        {busyMembershipId === member.membershipId ? 'Removing...' : 'Remove member'}
                      </button>
                    </div>
                  )}
                </article>
              ))}
              {visibleMembers.length === 0 && (
                <p className="moderation-empty">
                  {members.length === 0 ? 'No active members found.' : 'No members match your search.'}
                </p>
              )}
            </div>
          </div>
        </section>
      </div>
    </div>
  )
}
