# UniMart Plan Iteration 1

## Goal

Build a private community marketplace where only approved members of a school or organization can view, search, and create listings inside their community.

## Current Build Status

### Backend

- Spring Boot API is set up with PostgreSQL and Spring Data JPA.
- Session-based auth is implemented using OTP-style login codes.
- Community membership is enforced before listing access.
- Supported membership flows:
  - join by allowed email domain
  - join by invite token
  - request access for moderator approval
- Listings support:
  - create
  - read
  - search across active memberships
  - edit basic fields
  - mark sold
  - mark removed
- Moderation support:
  - approve membership requests
  - revoke memberships
  - create invite links
  - add allowed email domains
  - review listing reports
- Upload flow is stubbed and validates type and size before returning storage metadata.

### Frontend

- Auth screen with local-development OTP flow.
- Dashboard with:
  - joined communities
  - searchable listing feed
  - discover communities
  - join by domain
  - request membership
  - join by invite token
- Community page with:
  - community listings
  - moderator invite generation
  - moderator domain management
- Profile page where users can edit account details and upload a profile picture.
- Create listing page with up to 5 media uploads.
- Moderation page for membership requests and reports.
- Shared listing preview carousel for feed and community views.

## Seeded Demo State

- Communities:
  - `Campus Market` with domain `school.edu`
  - `Makers Exchange` with domain `makers.org`
- Active users:
  - `admin@school.edu`
  - `ava@school.edu`
  - `noah@school.edu`
  - `lead@makers.org`
  - `mia@makers.org`
  - `ethan@makers.org`
  - `sofia@makers.org`
  - `liam@makers.org`
  - `grace@makers.org`
  - `jack@makers.org`
- Extra membership test users:
  - `pending@school.edu` with pending access
  - `former@makers.org` with revoked access
- Seeded content:
  - sample invite links for both communities
  - multiple listings with media
  - one sold listing
  - one removed listing
  - sample moderation reports

## Verified / Expected MVP Flows

### Auth

- Requesting a login code works.
- Verifying the code creates a session.
- Protected endpoints fail without auth.

### Community Access

- A user with an eligible email domain can join a community.
- Invite token join works.
- Membership request enters pending state.
- Non-members cannot access private listings or search results.

### Listings

- An approved member can create a listing.
- A listing appears in search and feed views for community members.
- The seller can edit listing details.
- The seller can mark a listing sold.
- The seller can mark a listing removed.
- Other regular members cannot edit another seller's listing.

### Moderation

- Moderator or admin users can approve pending membership requests.
- Moderator or admin users can revoke access.
- Moderator or admin users can create invite links.
- Moderator or admin users can add a new allowed email domain.
- Reports appear in moderation view.

### Media

- Allowed image and video content types are accepted.
- Unsupported file types are rejected.
- Oversized uploads are rejected.

## Known Simplifications

- OTP is returned directly in API responses for local development.
- Real email delivery is not integrated yet.
- Real object storage is not integrated yet.
- Upload URLs are placeholder-backed media URLs.
- Profile pages are self-edit only; there is no public seller profile yet.
- There is no listing delete endpoint yet.
- No chat, payment, ratings, or delivery workflows exist yet.
- In `dev`, demo seed data is refreshed on each backend startup.

## Immediate Next Priorities

1. Manually verify each MVP flow end to end in the browser.
2. Add backend integration tests for auth, membership gating, and listings.
3. Add public seller profile pages and seller listing management UX.
4. Replace stub upload flow with real storage.
5. Improve frontend validation and error handling.
