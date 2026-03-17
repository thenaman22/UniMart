# UniMart Plan Iteration 3

## Summary

This iteration turns profile, community, and moderation into more complete product surfaces instead of basic utility pages.

The main outcomes are:

- a redesigned self profile as a seller workspace
- a redesigned public seller profile for shared-community discovery
- self-serve community membership visibility and leaving
- more consistent community listing cards
- a moderation workspace with member management

## Objective

Design and implement the next UX pass for trusted-community selling by improving:

- owner profile management at `"/profile"`
- public seller browsing at `"/users/:userId"`
- joined-community visibility and leaving flows
- community listing presentation quality
- moderator tools for current-member review and removal

## Product Decisions Locked In

- The signed-in owner profile remains at `"/profile"`.
- Public seller profiles remain at `"/users/:userId"`.
- Public seller profiles stay read-only and continue to show only currently active listings visible through shared active communities.
- The profile-page top search bar is hidden on:
  - `"/profile"`
  - `"/users/:userId"`
- The moderation page removes the normal topbar/search entirely and uses a dedicated workspace layout.
- Owner profile listing management uses three tabs:
  - `Current`
  - `Sold`
  - `Archive`
- `Archive` is a UI label for existing `REMOVED` listings, not a new backend status.
- Owner profile listings are shown as compact product-style tiles rather than long management cards.
- Public seller listings use the same compact tile language, but without owner actions.
- The owner profile stats remove the old `Posts` and `Archive` summary cards.
- The owner profile header now surfaces:
  - `Current`
  - `Sold`
  - `Communities`
- The `Communities` stat opens a searchable modal listing the user’s joined communities.
- Users can leave their own active communities from the profile flow.
- Community marketplace cards should have uniform visual sizing.
- Moderators can review current members of the community they manage and remove them if needed.
- Member search inside moderation is client-side over the fetched member list for now.

## Backend Changes

- Added self-serve leave-community support:
  - `DELETE /communities/{communityId}/membership`
- Added `MembershipService.leaveCommunity(...)`:
  - only active memberships can be left
  - leaving sets membership status to `REVOKED`
- Added active-member moderation listing:
  - `GET /moderation/{communityId}/members`
- Expanded moderation-related response mapping:
  - active member payload now includes member identity fields
  - membership request payload now includes `userId`
- No schema or database changes were required.

## Frontend Changes

### Profile Experience

- Redesigned `"/profile"` into a seller workspace with:
  - compact hero header
  - current/sold/archive listing tabs
  - tile-based listing presentation
  - inline owner actions per tab
- Redesigned `"/users/:userId"` into a cleaner public seller page with:
  - normal seller info only
  - current listings only
  - no seller-management controls
- Updated listing media preview to support square tile mode for profile cards.
- Replaced the old owner-profile stats row with:
  - current listings
  - sold listings
  - joined communities
- Added a searchable community modal on the owner profile with:
  - joined-community count
  - `View` links
  - `Leave` action for active memberships

### Community Marketplace

- Updated community-page listing cards to use a more consistent marketplace-card layout.
- Forced community listing media into one shared visual ratio so listings line up cleanly.
- Anchored seller/action sections so cards feel aligned across the grid.

### Moderation Workspace

- Reworked moderation into a dedicated split layout:
  - centered community name pill at top
  - `Pending requests` panel on the upper left
  - `Reports` panel on the lower left
  - `Member list` panel on the right
- Each moderation panel now owns its own scroll area when content exceeds available height.
- Added current-member display to moderation.
- Added moderator remove-member action for active members.
- Prevented the current moderator from seeing a remove button for themself.
- Added a small inline member search input inside the member-list header.

### Shared App Shell

- Updated topbar behavior so:
  - profile routes hide the search area
  - moderation hides the topbar entirely
- Added moderation-specific page sizing so the workspace fits the viewport and uses internal panel scrolling.

## API / Data Contract Changes

- New endpoint:
  - `DELETE /communities/{communityId}/membership`
- New endpoint:
  - `GET /moderation/{communityId}/members`
- Membership request payload now includes:
  - `userId`
- Active-member moderation payload includes:
  - `membershipId`
  - `communityId`
  - `communityName`
  - `userId`
  - `memberName`
  - `memberEmail`
  - `status`
  - `role`

## Acceptance Criteria

- `"/profile"` shows a compact owner profile with `Current`, `Sold`, and `Archive` tabs.
- Owner listings render as compact product tiles rather than long stacked management cards.
- Public seller profiles keep read-only behavior and only show active shared-community listings.
- The owner profile shows joined-community count instead of the old posts/archive summary.
- The owner can open a community list modal from profile and leave communities from it.
- Leaving a community removes that community from active memberships and updates the owner profile accordingly.
- Community-page listing cards render at consistent size and align cleanly.
- Moderation shows:
  - pending requests
  - current members
  - reports
- Moderators can remove current members from the managed community.
- Member list supports quick client-side filtering by:
  - member name
  - email
  - role
- The moderation layout uses internal scrolling within each panel when needed.

## Verification Completed

- Frontend verification:
  - `npm run build` passed after profile, community, and moderation updates
- Backend verification:
  - `.\gradlew.bat test` passed after membership and moderation endpoint updates

## Compatibility With Iteration 2

- Public seller profiles remain shared-community gated and read-only.
- Owner profile management extends the Iteration 2 seller-profile foundation rather than replacing it.
- Listing status management still uses the same existing statuses:
  - `ACTIVE`
  - `SOLD`
  - `REMOVED`
- Community membership continues to drive listing visibility and seller-profile access.
- The new self-serve leave flow works within the existing membership status model.

## Known Constraints / Follow-up For Iteration 4

- Moderation currently assumes a single active managed community context by choosing the first community where the user is not just a `MEMBER`.
- Member search is client-side; if communities become large, server-side search and pagination may be worth adding.
- Reports are still read-only; there is no resolve/dismiss workflow yet.
- Community leaving currently revokes access immediately and does not add any extra confirmation or role-transfer flow for admins.
- If admins should be prevented from leaving the last managed community without reassignment, that rule should be added in a future iteration.
