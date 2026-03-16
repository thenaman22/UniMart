# V1 Listing Messaging Plan

## Summary
Build a lightweight, product-scoped messaging feature for marketplace inquiries. Messaging is only available in the context of a listing, sellers see message activity grouped by their products, and buyers get a simple `Sent inquiries` view for the listings they asked about.

The v1 UX:
- Seller `Messages` shows only seller-owned listings with message activity.
- Clicking a listing opens the buyer threads for that listing.
- Buyers get a `Sent inquiries` section grouped by listing.
- Unread is tracked at the conversation level for badges/counts.
- Once a listing is not `ACTIVE`, existing conversations stay visible but become read-only.

## Key Changes
### Data model
- Add a `ListingConversation` entity as the primary thread record.
- Store:
  - `listing`
  - `seller`
  - `buyer`
  - `lastMessageAt`
  - seller unread count
  - buyer unread count
- Enforce one conversation per `(listing, buyer)` so repeat inquiries reuse the same thread.
- Add a `ListingMessage` entity for the timeline:
  - `conversation`
  - `sender`
  - `body`
  - `createdAt`

### Backend behavior and APIs
- Add messaging repository/service/controller layers.
- Add `POST /listings/{listingId}/messages`:
  - only non-seller users can send
  - create conversation if none exists for that buyer/listing
  - append message otherwise
  - blocked if listing is not `ACTIVE`
- Add `GET /messages/seller`:
  - return seller-owned listings that have at least one conversation
  - each listing includes unread count and latest activity
- Add `GET /messages/seller/listings/{listingId}`:
  - return buyer thread summaries for that listing
  - include buyer name, latest message preview, last activity, unread badge
- Add `GET /messages/conversations/{conversationId}`:
  - return full thread and listing summary
  - authorize only seller or buyer in that conversation
- Add `POST /messages/conversations/{conversationId}/read`:
  - zero out the current user's conversation-level unread count when they open/read the thread
- Add `GET /messages/buyer`:
  - return buyer conversations grouped by listing as `Sent inquiries`
- Preserve existing membership/access rules:
  - users must be able to access the listing's community
  - seller cannot message own listing
  - inactive listings remain viewable in existing threads but cannot accept new sends

### Frontend UX
- Add a sidebar `Messages` route.
- Seller view:
  - first panel shows listing cards with message activity only
  - each card shows listing title, preview image if available, latest activity, and unread badge
  - selecting a listing shows buyer threads for that listing
  - selecting a buyer thread opens the conversation pane
- Buyer view:
  - same route includes a `Sent inquiries` section/tab
  - group buyer conversations by listing
  - opening a listing shows the seller conversation for that listing
- Conversation view:
  - show listing context at the top
  - show message timeline
  - show composer only if listing is `ACTIVE`
  - show read-only notice if listing is sold/removed
- Add a `Message seller` action on listing surfaces where users discover products.
  - Minimum v1 assumption: place it on listing cards in feed/community/profile views where the current user is not the seller.

## Test Plan
- Backend:
  - first buyer message creates one conversation for that listing
  - second message from same buyer reuses the same conversation
  - different buyer on same listing creates a second conversation
  - seller cannot message own listing
  - unauthorized users cannot view/send outside shared community access
  - inactive listing blocks new sends
  - seller listing inbox excludes listings with no message activity
  - buyer sent view groups by listing correctly
  - read endpoint clears only the current user's conversation unread count
- Frontend/acceptance:
  - seller sees `Messages` listing cards only after inquiries exist
  - opening a listing shows buyer-specific threads
  - unread badge appears on conversation/listing summaries
  - buyer sees sent inquiry under the relevant listing
  - inactive listing thread stays visible and composer is disabled

## Assumptions
- V1 uses conversation-level unread counts, not per-message read receipts.
- One buyer can have only one thread per listing.
- Real-time updates are out of scope for v1; refresh/refetch on page load and after send/read actions is sufficient.
- There is no separate general-purpose chat entry point; messaging always starts from a listing.
- If needed, the `Messages` page can default to seller inbox first and expose `Sent inquiries` as a secondary tab/section for users who are also buyers.
