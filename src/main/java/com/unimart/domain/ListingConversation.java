package com.unimart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "listing_conversations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"listing_id", "buyer_id"})
)
public class ListingConversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id")
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private UserAccount seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id")
    private UserAccount buyer;

    @Column(nullable = false)
    private Instant lastMessageAt;

    @Column(nullable = false)
    private int sellerUnreadCount;

    @Column(nullable = false)
    private int buyerUnreadCount;

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public UserAccount getSeller() {
        return seller;
    }

    public void setSeller(UserAccount seller) {
        this.seller = seller;
    }

    public UserAccount getBuyer() {
        return buyer;
    }

    public void setBuyer(UserAccount buyer) {
        this.buyer = buyer;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public int getSellerUnreadCount() {
        return sellerUnreadCount;
    }

    public void setSellerUnreadCount(int sellerUnreadCount) {
        this.sellerUnreadCount = sellerUnreadCount;
    }

    public int getBuyerUnreadCount() {
        return buyerUnreadCount;
    }

    public void setBuyerUnreadCount(int buyerUnreadCount) {
        this.buyerUnreadCount = buyerUnreadCount;
    }
}
