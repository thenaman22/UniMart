import { useEffect, useMemo, useState } from 'react'
import { api } from '../api'

const ROLE_OPTIONS = [
  { value: 'MEMBER', label: 'Viewer' },
  { value: 'MODERATOR', label: 'Moderator' }
]

const SELLER_ROLE_OPTIONS = [
  { value: 'MEMBER', label: 'Viewer' },
  { value: 'SELLER', label: 'Seller' },
  { value: 'MODERATOR', label: 'Moderator' }
]

function formatNotificationCount(count) {
  return count > 9 ? '9+' : count
}

export function ModerationPage({
  user,
  communities,
  moderationSummary,
  onCommunitiesChanged,
  onModerationSummaryChanged
}) {
  const manageableCommunities = useMemo(
    () => communities.filter(item => item.canModerate),
    [communities]
  )
  const [selectedCommunityId, setSelectedCommunityId] = useState('')
  const [requests, setRequests] = useState([])
  const [members, setMembers] = useState([])
  const [reports, setReports] = useState([])
  const [error, setError] = useState('')
  const [busyMembershipId, setBusyMembershipId] = useState(null)
  const [memberSearch, setMemberSearch] = useState('')

  const modCommunity = manageableCommunities.find(item => String(item.communityId) === String(selectedCommunityId)) || manageableCommunities[0]

  useEffect(() => {
    if (!selectedCommunityId && manageableCommunities.length > 0) {
      setSelectedCommunityId(String(manageableCommunities[0].communityId))
    }
  }, [selectedCommunityId, manageableCommunities])

  async function loadModerationData(communityId) {
    const [pendingRequests, activeMembers, openReports] = await Promise.all([
      api(`/moderation/${communityId}/requests`),
      api(`/moderation/${communityId}/members`),
      api(`/moderation/${communityId}/reports`)
    ])
    setRequests(pendingRequests)
    setMembers(activeMembers)
    setReports(openReports)
  }

  useEffect(() => {
    if (!user || !modCommunity) return
    loadModerationData(modCommunity.communityId).catch(err => setError(err.message))
  }, [user, modCommunity])

  async function updateMembership(membershipId, approve) {
    setBusyMembershipId(membershipId)
    try {
      setError('')
      await api(`/moderation/memberships/${membershipId}?approve=${approve}`, { method: 'PATCH' })
      await Promise.all([
        loadModerationData(modCommunity.communityId),
        onCommunitiesChanged?.(),
        onModerationSummaryChanged?.()
      ])
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyMembershipId(null)
    }
  }

  async function updateRole(membershipId, role) {
    setBusyMembershipId(membershipId)
    try {
      setError('')
      await api(`/communities/${modCommunity.communityId}/memberships/${membershipId}/role`, {
        method: 'PATCH',
        body: JSON.stringify({ role })
      })
      await loadModerationData(modCommunity.communityId)
      await onCommunitiesChanged?.()
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

  const roleOptions = useMemo(() => (
    modCommunity?.postingPolicy === 'APPROVED_SELLERS_ONLY'
      ? SELLER_ROLE_OPTIONS
      : ROLE_OPTIONS
  ), [modCommunity])

  const currentRoleLabel = modCommunity
    ? `${modCommunity.roleLabel || modCommunity.role}${modCommunity.isCreator ? ' (creator)' : ''}`
    : ''
  const pendingCountsByCommunityId = useMemo(() => (
    new Map((moderationSummary?.communities || []).map(item => [String(item.communityId), item.pendingRequestCount]))
  ), [moderationSummary])
  const totalPendingRequestCount = moderationSummary?.pendingRequestCount || 0
  const selectedCommunityPendingCount = modCommunity
    ? (pendingCountsByCommunityId.get(String(modCommunity.communityId)) || 0)
    : 0
  const otherPendingRequestCount = Math.max(0, totalPendingRequestCount - selectedCommunityPendingCount)

  if (!user) {
    return <section className="panel"><p>Please sign in first.</p></section>
  }

  if (!modCommunity) {
    return <section className="panel"><p>You do not have moderator access in any community yet.</p></section>
  }

  return (
    <div className="moderation-shell">
      <div className="moderation-toolbar">
        <div className="moderation-toolbar-copy">
          <div className="moderation-title-pill">{modCommunity.name}</div>
          <p className="feed-meta moderation-role-summary">Your role: <strong>{currentRoleLabel}</strong></p>
        </div>
        <div className="moderation-community-select-shell">
          <select
            className="moderation-community-select"
            value={modCommunity.communityId}
            onChange={event => setSelectedCommunityId(event.target.value)}
          >
            {manageableCommunities.map(community => (
              <option key={community.communityId} value={community.communityId}>
                {community.name}
              </option>
            ))}
          </select>
          {otherPendingRequestCount > 0 && (
            <span className="sidebar-notification-badge moderation-community-select-badge">
              {formatNotificationCount(otherPendingRequestCount)}
            </span>
          )}
        </div>
      </div>
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
                  <p>{member.roleLabel || member.role}</p>
                  {modCommunity.canManageRoles && !member.isCreator && member.userId !== user.id && (
                    <div className="member-role-controls">
                      {roleOptions.map(option => {
                        const isCurrentRole = option.value === member.role
                        return (
                        <button
                          key={option.value}
                          type="button"
                          className={`role-option-button${isCurrentRole ? ' active' : ' ghost'}`}
                          aria-pressed={isCurrentRole}
                          disabled={busyMembershipId === member.membershipId || isCurrentRole}
                          onClick={() => updateRole(member.membershipId, option.value)}
                        >
                          {isCurrentRole ? `Current: ${option.label}` : option.label}
                        </button>
                        )
                      })}
                    </div>
                  )}
                  {member.userId === user.id ? (
                    <p className="feed-meta">Your role here: <strong>{member.roleLabel || member.role}{member.isCreator ? ' (creator)' : ''}</strong></p>
                  ) : (!member.isCreator && (modCommunity.canManageRoles || (member.role !== 'MODERATOR' && member.role !== 'ADMIN'))) ? (
                    <div className="button-row">
                      <button
                        className="ghost"
                        disabled={busyMembershipId === member.membershipId}
                        onClick={() => updateMembership(member.membershipId, false)}
                      >
                        {busyMembershipId === member.membershipId ? 'Removing...' : 'Remove member'}
                      </button>
                    </div>
                  ) : null}
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
