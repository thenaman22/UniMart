# UniMart Working Plan

## Goal

Build a private community marketplace where only approved members of a school or organization can view, search, and create listings inside their community.

## Current Build Status

### Backend

- Spring Boot API is set up with PostgreSQL
- Session-based auth is implemented using OTP-style login codes
- Community membership is enforced before listing access
- Supported membership flows:
  - join by allowed email domain
  - join by invite token
  - request access for moderator approval
- Listings support:
  - create
  - read
  - search
  - edit basic fields
  - mark sold / removed
- Moderation support:
  - approve or reject membership requests
  - create invite links
  - add allowed email domains
  - review listing reports
- Upload flow is stubbed and validates type and size

### Frontend

- Auth screen with dev OTP flow
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
- Create listing page
- Moderation page for requests and reports

## Seeded Demo State

- Community: `Campus Market`
- Allowed domain: `school.edu`
- Admin user: `admin@school.edu`

## What We Should Verify

### Auth

- Requesting a login code works
- Verifying the code creates a session
- Protected endpoints fail without auth

### Community Access

- User with eligible email domain can join a community
- Invite token join works
- Membership request enters pending state
- Non-members cannot access private listings or search results

### Listings

- Approved member can create a listing
- Listing appears in search/feed for community members
- Seller can edit listing
- Seller can mark listing sold
- Other regular members cannot edit another seller's listing

### Moderation

- Moderator/admin can approve pending membership requests
- Moderator/admin can revoke access
- Moderator/admin can create invite links
- Moderator/admin can add a new allowed email domain
- Reports appear in moderation view

### Media

- Allowed image/video content types are accepted
- Unsupported file types are rejected
- Oversized uploads are rejected

## Known Simplifications

- OTP is returned directly in API responses for local development
- Real email delivery is not integrated yet
- Real object storage is not integrated yet
- Upload URLs are placeholders
- No chat, payment, ratings, or delivery workflows

## Next Priorities

1. Manually verify each MVP flow end to end in the browser.
2. Add backend integration tests for auth, membership gating, and listings.
3. Replace stub upload flow with real storage.
4. Improve frontend validation and error handling.
5. Add persistent environment configuration for local and production setups.
