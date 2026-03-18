# UniMart: Technical Build, Iteration by Iteration

## Stack and Overall Architecture

UniMart is built as a full-stack web application with:

- Java 17
- Spring Boot 3.3 for the backend API
- Spring Data JPA for persistence
- Spring Security for request protection
- PostgreSQL as the main database
- React 18 + Vite for the frontend
- React Router for route-based page flows

The backend follows a layered structure:

- `domain` for persistent entities such as communities, memberships, listings, sessions, reports, and messages
- `repository` for database access
- `service` for business rules and authorization
- `api` for controllers, DTOs, and response mapping

The frontend is page-oriented and keeps feature logic close to the route that uses it. A small shared API client in `frontend/src/api.js` sends requests with the session token in the `X-Auth-Token` header.

## Iteration 1: Private Community Marketplace Foundation

The first iteration established the base product model and the minimum end-to-end marketplace flow.

### Backend work

- Set up Spring Boot with PostgreSQL and JPA entities for:
  - `UserAccount`
  - `LoginCode`
  - `UserSession`
  - `Community`
  - `Membership`
  - `InviteLink`
  - `Listing`
  - `ListingMedia`
  - `Report`

- Implemented OTP-style login:
  - request a code
  - verify the code
  - create a session
  - protect private endpoints

- Enforced community membership before allowing listing access or search visibility.
- Implemented three join paths:
  - allowed email-domain entry
  - invite-token join
  - request-for-approval flow
- Added core listing operations:
  - create
  - read
  - search
  - edit basic fields
  - mark sold
  - mark removed
- Added moderation APIs for:
  - membership approval
  - membership revocation
  - invite creation
  - allowed email-domain management
  - report review
  
- Added an upload stub that validates file type and size before returning storage metadata, which prepared the API contract for later storage integration.

### Frontend work

- Built the initial auth screen and local-development OTP flow.
- Added the dashboard with:
  - joined communities
  - searchable listing feed
  - community discovery
  - join by domain
  - join by invite
  - membership request flow
- Added:
  - community page
  - profile page
  - create-listing page
  - moderation page
- Reused a shared listing preview carousel across feed and community surfaces.

### Technical outcome

By the end of Iteration 1, the app already enforced its core architectural rule: listings are never treated as globally public objects; visibility depends on authenticated membership inside a community.

## Iteration 2: Seller Profiles and Owner Listing Management

The second iteration focused on making profiles useful both for the owner and for other users inside shared communities.

### Backend work

- Extended the user profile model with `publicPhoneVisible`.
- Kept `GET /profile` for the signed-in user’s editable profile.
- Added `GET /users/{userId}/profile` for public seller profiles.
- Added authorization logic requiring at least one shared active community between viewer and seller before returning public profile data.
- Split profile response shapes into:
  - self profile data
  - public profile data
  - listing summary data
- Expanded listing summary DTOs to include `sellerId`, which allowed the frontend to link listing cards to seller pages.
- Added `DELETE /listings/{listingId}` as a true owner-authorized delete endpoint.
- Kept existing patch endpoints for editing content and changing listing status.

### Frontend work

- Added routes for:
  - `"/profile"`
  - `"/users/:userId"`
  - `"/listings/:listingId/edit"`
- Turned seller names into links from listing views to seller profiles.
- Reworked the signed-in profile into:
  - account settings
  - seller management workspace
- Added quick listing actions from profile:
  - mark sold
  - relist
  - remove from sale
  - delete
  - open full editor
- Added a dedicated listing editor for title, description, price, category, and condition.

### Technical outcome

Iteration 2 separated private owner management from public seller discovery without breaking the community-gated access model established in Iteration 1.

## Iteration 3: Seller Workspace UX, Self-Serve Membership Management, and Better Moderation

This iteration mostly expanded behavior and UI quality rather than introducing major schema changes.

### Backend work

- Added `DELETE /communities/{communityId}/membership` for self-serve leave-community support.
- Implemented leave behavior in `MembershipService` by revoking active memberships.
- Added `GET /moderation/{communityId}/members` so moderators could review current active members.
- Expanded moderation payloads with member identity fields and `userId` references.
- Kept the existing membership and listing status model intact, so this iteration stayed compatible with the earlier data model.

### Frontend work

- Redesigned `"/profile"` into a seller workspace with tabs:
  - `Current`
  - `Sold`
  - `Archive`
- Used the existing backend statuses (`ACTIVE`, `SOLD`, `REMOVED`) rather than inventing a new archive state.
- Changed owner listings from long management cards into compact product tiles.
- Added a searchable communities modal on the profile page with leave actions.
- Updated public seller profiles into cleaner read-only pages.
- Standardized community marketplace cards so media ratios and card heights align better.
- Rebuilt moderation as a split workspace with:
  - pending requests
  - reports
  - current member list
  - inline member search
  - member removal controls
- Adjusted app-shell behavior so profile pages hide the top search and moderation uses a dedicated workspace layout.

### Technical outcome

Iteration 3 improved usability while staying within the same authorization and status model, which kept the implementation relatively low-risk.

## Iteration 4: Community Creation, Posting Policies, and Role-Based Community Control

This was the biggest structural expansion of the product because communities became user-created, role-aware product objects.

### Backend work

- Added community ownership and posting-policy data to the `Community` model:
  - `creator_user_id`
  - `postingPolicy`
- Added the `CommunityPostingPolicy` enum with:
  - `ALL_MEMBERS_CAN_POST`
  - `APPROVED_SELLERS_ONLY`
  - `CREATOR_ONLY`
- Added `SELLER` to `MembershipRole`.
- Created `CommunityService` to handle:
  - community creation
  - creator-limit enforcement
  - slug generation
  - hard-delete cleanup
- Added `POST /communities` to create closed communities from the product UI.
- Added `DELETE /communities/{communityId}` for admin-only hard delete.
- Added `PATCH /communities/{communityId}/memberships/{membershipId}/role` for admin-controlled role updates.
- Enforced a hard cap of five active communities per creator.
- Changed listing-creation authorization so posting depends on community posting policy, not just active membership.
- Tightened moderation rules so moderators cannot manage staff or remove the creator/admin.
- Enriched community-related responses with capability metadata such as:
  - `canPost`
  - `canModerate`
  - `canManageRoles`
  - `canManageCommunity`
  - `canDelete`
  - `isCreator`
  - `roleLabel`
- Added compatibility handling for older data:
  - communities with `null` posting policy default at runtime
  - a startup repair updates the membership-role database constraint to include `SELLER`
- Implemented hard-delete cleanup across memberships, domains, invites, listings, listing media, conversations, messages, reports, and uploaded files tied to a community.

### Frontend work

- Added community creation to `"/communities"`.
- Added a create form for:
  - name
  - description
  - posting policy
- Surfaced the creator’s owned-community count and disabled creation once the limit is reached.
- Changed community-directory search so it filters communities instead of redirecting into listing search.
- Updated community detail pages to show posting-policy information.
- Restricted create-listing choices to communities where the current user can actually post.
- Added empty-state handling when a user has no posting rights anywhere.
- Reworked moderation so users can switch between multiple managed communities.
- Added admin-only role controls for viewer, seller, and moderator states.
- Hid seller-role controls when the selected community’s posting policy does not use approved sellers.

### Technical outcome

Iteration 4 moved the product from a seeded demo marketplace into a self-serve platform model where communities can be created, governed, and moderated directly inside the application.

## Messaging v1 Track

Alongside the main iteration track, UniMart also added listing-scoped messaging.

### Backend work

- Added `ListingConversation` as the thread-level record.
- Added `ListingMessage` as the timeline-level record.
- Enforced one conversation per `(listing, buyer)` pair so repeated inquiries reuse the same thread.
- Stored unread state at the conversation level:
  - seller unread count
  - buyer unread count
- Added messaging endpoints for:
  - creating a listing inquiry
  - viewing seller inbox summaries
  - viewing buyer inquiry summaries
  - opening a conversation
  - marking a conversation as read
- Preserved existing community-access rules so users can only message when they are allowed to access the listing’s community.
- Kept existing threads visible when a listing becomes inactive, but blocked new sends in that state.

### Frontend work

- Added a dedicated `"/messages"` route.
- Built seller-side views grouped by listing activity.
- Built buyer-side `Sent inquiries` views grouped by listing.
- Added unread badges and conversation views tied to listing context.
- Kept messaging product-scoped instead of introducing a general-purpose chat system.

### Technical outcome

This design kept messaging simple and marketplace-focused. It avoided building a full social chat system and instead anchored conversations to the actual item being discussed.

## Testing and Verification

- The backend uses Spring Boot test support, with H2 available in the test runtime for automated verification.
- `CommunityManagementTests` cover:
  - community creation
  - five-community limit enforcement
  - creator auto-admin behavior
  - posting-policy enforcement
  - admin-only role updates
  - seller-role restrictions
  - domain auto-assignment
  - hard-delete cleanup
- `MessagingServiceTests` cover:
  - conversation reuse for repeat buyer messages
  - inactive-listing read-only behavior
  - attachment/image message support
- The planning docs also record successful `.\gradlew.bat test` and `npm run build` checks during the iteration cycle.

## Overall Technical Pattern

The implementation stayed consistent across iterations:

- backend business rules live in service classes
- authorization is enforced close to the business logic
- repositories stay focused on persistence
- DTOs and mappers keep API contracts explicit
- frontend pages map closely to product surfaces and routes

That approach made it possible to keep extending the app iteration by iteration without rewriting the foundation each time.
