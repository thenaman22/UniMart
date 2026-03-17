# UniMart Plan Iteration 4

## Summary

This iteration turns communities into self-serve product surfaces instead of seed-only data.

The main outcomes are:

- user-created communities with a hard cap of 5 per creator
- community-level posting policies
- community-specific seller and moderator roles
- admin-managed role assignment and hard delete
- a moderation workspace that can switch between multiple managed communities
- a community directory that now searches communities instead of listings
- a cleaner create-listing flow with no top search bar distraction
- a tighter main feed card layout with more controlled listing-media framing

## Objective

Design and implement community creation and role-based posting so UniMart supports:

- user-created communities from the product UI
- distinct posting rules per community
- different community roles for the same user across different spaces
- admin-controlled seller and moderator assignment
- multi-community moderation without assuming one fixed managed community

## Product Decisions Locked In

- Communities can now be created by signed-in users from the app.
- A creator may own at most 5 active communities at one time.
- Every created community stores a dedicated creator/owner reference.
- The creator is the only `ADMIN` in the current implementation.
- Community posting uses one of three policies:
  - `ALL_MEMBERS_CAN_POST`
  - `APPROVED_SELLERS_ONLY`
  - `CREATOR_ONLY`
- Existing persisted `MEMBER` data is kept for compatibility and is treated as the viewer role in product behavior and UI copy.
- `SELLER` is a new membership role for communities that restrict who may post.
- Community joining remains:
  - request to join
  - join by invite
- Email-domain allowlists remain optional admin-managed settings and are not part of the create-community form.
- Admins can:
  - manage seller/moderator roles
  - manage invites
  - manage domains
  - hard-delete the community
- Moderators can:
  - review reports
  - approve or reject membership requests
  - remove non-staff members
- Moderators cannot:
  - assign roles
  - remove the creator/admin
  - remove other staff members
- Creator/admin memberships cannot leave their own community through the normal leave flow; deletion is the supported path.
- The moderation page no longer assumes one managed community and instead lets the user choose which community they are moderating.
- The `SELLER` role is only meaningful for communities using `APPROVED_SELLERS_ONLY` and is not offered in open-posting or creator-only communities.
- The community-directory search experience is community-specific on `"/communities"` and no longer routes users into listing search from that page.
- The create-listing page intentionally hides the global top search bar to keep the flow focused.

## Backend Changes

- Added community ownership and posting-policy data to the community model:
  - `creator_user_id`
  - `postingPolicy`
- Added compatibility handling for older community rows:
  - `null` posting-policy values are treated as `ALL_MEMBERS_CAN_POST` at runtime
  - this avoids startup failures when existing databases already contain seeded communities
- Added `SELLER` to `MembershipRole`.
- Added a new `CommunityPostingPolicy` enum.
- Added a dedicated `CommunityService` for:
  - creating communities
  - enforcing the 5-community creator limit
  - generating unique slugs from community names
  - hard-deleting a community and all related data
- Added `POST /communities`:
  - creates a new closed community
  - stores the creator
  - creates an active `ADMIN` membership for the creator
- Added `DELETE /communities/{communityId}`:
  - admin-only
  - hard-deletes memberships, domains, invites, listings, listing media, conversations, messages, reports, and uploaded media files tied to that community
- Added `PATCH /communities/{communityId}/memberships/{membershipId}/role`:
  - admin-only
  - supports `MEMBER`, `SELLER`, and `MODERATOR`
  - explicitly blocks reassigning `ADMIN`
  - blocks assigning `SELLER` unless the community uses `APPROVED_SELLERS_ONLY`
- Updated community discovery and membership mapping so community responses now expose:
  - `postingPolicy`
  - `postingPolicyLabel`
  - `creatorUserId`
  - `roleLabel`
  - `canPost`
  - `canModerate`
  - `canManageRoles`
  - `canManageCommunity`
  - `canDelete`
  - `isCreator`
- Updated listing creation authorization so posting depends on the community posting policy instead of only active membership.
- Tightened moderation and revocation rules so moderators cannot remove staff and cannot remove the creator/admin.
- Restricted invite and domain management to admins.
- Updated seeded communities so they populate creator ownership and default posting policy values.
- Added a PostgreSQL startup repair for older databases so `memberships_role_check` is updated to include `SELLER`.

## Frontend Changes

### Communities Directory

- Added a create-community flow on `"/communities"` for signed-in users.
- The create form now sits behind a `Create your own community` toggle instead of always rendering open.
- Added create-community form fields for:
  - name
  - description
  - posting policy
- Surfaced the creator’s current owned-community count and disabled creation at 5 of 5.
- Auto-closes the create form after successful community creation.
- Updated the topbar search on `"/communities"` so it:
  - uses community-focused placeholder copy
  - keeps users on the communities route
  - filters by community name, description, and posting-policy label
- Community cards now show:
  - posting policy label
  - viewer-facing role label when already joined

### Community Page

- Community detail pages now show posting policy information.
- Admins can now:
  - generate invite links
  - add allowed email domains
  - hard-delete the community
- Users who can post now get a clear create-listing entry point from the community page.

### Listing Creation

- The create-listing page now filters the community dropdown to only communities where the current user can post.
- If the user cannot post anywhere, the page renders a clear empty state instead of a misleading generic form.
- The top search bar is hidden on `"/sell"` so the page stays focused on listing creation.

### Moderation Workspace

- Replaced the old “first non-member community” assumption with an explicit community selector.
- Moderation data now reloads when switching communities.
- Added admin-only role controls in the member list for:
  - viewer (`MEMBER`)
  - `SELLER`
  - `MODERATOR`
- Seller role controls now appear only when the selected community uses `APPROVED_SELLERS_ONLY`.
- The selected moderation community now clearly shows the signed-in user’s current role in the workspace header.
- The signed-in user’s own member card now repeats that role explicitly.
- Current member roles are visually highlighted in the role-button group so the active role is obvious at a glance.
- Kept member removal in moderation, but aligned it with the stricter backend rules.

### Shared UI Copy

- Updated role display to use viewer-facing labels so `MEMBER` is shown as `Viewer`.
- Updated profile and dashboard community labels to use the richer role metadata.
- Prevented profile leave actions from presenting the normal leave path for creator/admin-owned communities.

### Marketplace Feed

- Narrowed the main dashboard feed layout so listing cards sit in a more social-feed-like column.
- Tuned feed listing-media framing to use a shallower, consistent stage that reduces excess empty space around portrait items.
- Kept feed media from being needlessly enlarged just to fill width, preserving image quality for smaller uploads.

## API / Data Contract Changes

- New endpoint:
  - `POST /communities`
- New endpoint:
  - `DELETE /communities/{communityId}`
- New endpoint:
  - `PATCH /communities/{communityId}/memberships/{membershipId}/role`
- Community payloads now include:
  - `postingPolicy`
  - `postingPolicyLabel`
  - `creatorUserId`
  - `roleLabel`
  - `canPost`
  - `canModerate`
  - `canManageRoles`
  - `canManageCommunity`
  - `canDelete`
  - `isCreator`
- Active-member moderation payloads now also include:
  - `roleLabel`
  - `isCreator`
- Membership-request payloads now also include:
  - `roleLabel`
  - `isCreator`

## Authorization Rules

- `ADMIN`
  - may manage invites and allowed domains
  - may update viewer/seller/moderator roles
  - may remove members and moderators
  - may delete the community
- `MODERATOR`
  - may approve or reject membership requests
  - may review reports
  - may remove non-staff members
  - may not change roles
- `SELLER`
  - may post only in communities whose posting policy allows them to post
  - may manage only their own listings
  - may only be assigned in communities using `APPROVED_SELLERS_ONLY`
- `MEMBER` / viewer
  - may browse and inquire inside joined communities
  - may not post in seller-restricted or creator-only communities
- Posting rights are enforced as:
  - `ALL_MEMBERS_CAN_POST`: any active membership can post
  - `APPROVED_SELLERS_ONLY`: only `ADMIN`, `MODERATOR`, or `SELLER` can post
  - `CREATOR_ONLY`: only the creator can post

## Acceptance Criteria

- A signed-in user can create a community until they reach the 5-community cap.
- Newly created communities store a creator and a posting policy.
- Community listing creation respects the configured posting policy.
- Seller-only communities deny posting to viewer-role members.
- Creator-only communities deny posting to moderators, sellers, and viewers.
- Admins can promote or demote members between viewer, seller, and moderator.
- Moderators cannot grant seller or moderator access.
- Moderation supports switching between all communities the user can moderate.
- Open-posting and creator-only communities do not surface seller-role controls in moderation.
- The moderation workspace makes the current role visually obvious for both the selected community and each member row.
- Community deletion removes the target community and its related marketplace data.
- Existing seeded `MEMBER` data still works and is presented as `Viewer`.
- The communities directory search filters communities instead of sending users to listing search.
- The create-listing page does not show the global top search bar.
- Main feed cards use the updated shallower media framing without forcing unnecessary image upscaling.

## Verification Completed

- Backend verification:
  - `.\gradlew.bat test`
- Frontend verification:
  - `npm run build`
- Added integration coverage for:
  - community creation
  - 5-community limit enforcement
  - creator auto-admin membership
  - posting-policy enforcement
  - admin-only role changes
  - seller-role rejection in non-seller-gated communities
  - domain auto-assignment regression
  - hard-delete cleanup

## Compatibility With Iteration 3

- Community membership still gates listing visibility and messaging visibility.
- Existing join-by-request, join-by-invite, and optional domain behaviors remain supported.
- Existing `MEMBER` rows continue to work without forcing an enum migration.
- The new moderation selector extends the Iteration 3 moderation workspace instead of replacing it.
- Profile, dashboard, and community screens continue to use the same shared community membership source, now enriched with role and capability metadata.

## Known Constraints / Follow-up For Iteration 5

- Admin promotion is intentionally not implemented; the creator remains the only admin in the current implementation.
- Report moderation is still read-only; there is no resolve/dismiss workflow yet.
- The create-community flow does not include visibility toggles or domain setup at creation time.
- Seller access is admin-assigned directly; there is no separate seller-request workflow yet.
- Community deletion is irreversible and does not offer an archive or recovery path.
