# UniMart Plan Iteration 2

## Summary

This iteration adds seller profile pages as the next major product feature. The goal is to let users visit their own profile as a seller workspace, let other signed-in users visit a seller's public profile, and keep the experience aligned with the private-community access model already established in Iteration 1.

## Objective

Design and implement profile pages that support:

- self profile management at `"/profile"`
- public seller profiles at `"/users/:userId"`
- owner management of their own listings
- discovery of a seller's currently available items by other eligible users

Messaging between users is intentionally deferred to a later iteration.

## Product Decisions Locked In

- The signed-in user's profile remains at `"/profile"`.
- Other users' public seller pages live at `"/users/:userId"`.
- A user may view another seller's profile only if both users share at least one active community membership.
- A public seller profile shows:
  - display name
  - profile image
  - bio
  - location
  - email
  - phone number only if the seller has opted into showing it
  - only currently active listings
- The signed-in user's own profile is both:
  - an account settings page
  - a seller management hub
- Listing management uses a mixed approach:
  - quick actions from the profile page
  - a dedicated edit page for full listing edits
- Deleting a listing is a real delete action, not a soft-delete alias.
- Editing listing media is out of scope for this iteration.

## Backend Changes

- Extend the user profile model with a `publicPhoneVisible` boolean flag, defaulting to `false`.
- Keep `GET /profile` for the signed-in user's full editable profile.
- Add `GET /users/{userId}/profile` for public seller profile data.
- Enforce profile visibility so that `GET /users/{userId}/profile` succeeds only when viewer and seller share at least one active community.
- Add seller-listing query support:
  - self profile returns all of the owner's listings, including `ACTIVE`, `SOLD`, and `REMOVED`
  - public seller profile returns only `ACTIVE` listings visible to the viewer through shared communities
- Expand listing summaries to include `sellerId` so frontend routes can link to seller pages.
- Keep existing listing edit and status endpoints:
  - `PATCH /listings/{listingId}`
  - `PATCH /listings/{listingId}/status`
- Add a new owner-authorized delete endpoint:
  - `DELETE /listings/{listingId}`
- Return response shapes that clearly separate:
  - self profile data
  - public profile data
  - listing summary data used across feed, community, and profile screens

## Frontend Changes

- Update routing to include:
  - `"/profile"` for self profile
  - `"/users/:userId"` for public seller profiles
  - `"/listings/:listingId/edit"` for dedicated listing editing
- Turn seller names in listing feed and community views into links to `"/users/:userId"`.
- Refactor the self profile page into two sections:
  - account details and privacy settings
  - a "My listings" management section
- On self profile listing cards, support quick actions for:
  - mark sold
  - relist to active
  - remove from sale
  - delete with confirmation
  - open the full editor
- Add a dedicated listing editor page for:
  - title
  - description
  - price
  - category
  - item condition
- Reuse existing create-listing form patterns where practical for edit mode.
- Public seller profile pages must:
  - show seller information and public contact fields
  - show only active listings
  - hide all owner management controls

## API / Data Contract Changes

- Self profile response should include:
  - editable user profile fields
  - `publicPhoneVisible`
  - `myListings`
- Public seller profile response should include:
  - public seller info
  - `activeListings`
- Listing summary objects should include:
  - `sellerId`
  - existing seller display name
  - current listing status
  - media preview fields already used by the frontend
- Self profile update input should accept the public phone visibility setting.

## Authorization Rules

- Self profile is available only to the authenticated owner.
- Public profile access requires authentication plus at least one shared active community with the seller.
- Sellers may edit, status-change, or delete only their own listings.
- Non-owners may never manage another seller's listings unless future moderation rules explicitly add that capability.
- Public profile listing visibility is limited to currently active listings only.

## Seller Management UX

- Keep account editing on `"/profile"` for display name, bio, phone, location, profile image, and public phone visibility.
- Use quick actions for fast lifecycle changes without leaving the profile page.
- Use a dedicated listing edit page for full content edits so the profile page does not become overloaded.
- Keep destructive actions explicit with confirmation before delete.

## Acceptance Criteria

- `"/profile"` shows account settings plus the signed-in user's listings.
- `"/users/:userId"` renders a public seller profile when the viewer shares an active community with that seller.
- Public seller profiles never expose sold or removed listings.
- Seller names in listing views navigate to the matching public profile route.
- Owners can mark listings sold, relist them, remove them from sale, and delete them from their self profile.
- Owners can open a dedicated edit page to update listing details.
- Users without a shared active community cannot view another user's public profile.
- Public phone appears only when the seller has opted into sharing it.

## Compatibility With Iteration 1

- Public seller profile routes: new requirement not previously covered.
- Shared-community visibility gate for public profiles: consistent with Iteration 1's community-scoped access model.
- Showing only active listings on public profiles: refinement of Iteration 1 listing visibility expectations.
- Mixed listing management UX for owners: new requirement not previously covered.
- Dedicated listing edit page: refinement of the existing listing edit capability from Iteration 1.
- Real delete endpoint for listings: new requirement not previously covered.
- Public phone visibility setting: new requirement not previously covered.
- Messaging deferral to a later phase: consistent with Iteration 1, which did not include chat.

## Open Follow-up For Iteration 3

- Add messaging between users, likely anchored from public seller profiles and listings.
- Revisit whether public email should remain always visible once messaging exists.
- Consider whether listing media editing should be added after the seller profile workflow is stable.
