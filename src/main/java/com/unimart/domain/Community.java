package com.unimart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "communities")
public class Community extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean privateCommunity = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id")
    private UserAccount creator;

    @Enumerated(EnumType.STRING)
    @Column
    private CommunityPostingPolicy postingPolicy = CommunityPostingPolicy.ALL_MEMBERS_CAN_POST;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPrivateCommunity() {
        return privateCommunity;
    }

    public void setPrivateCommunity(boolean privateCommunity) {
        this.privateCommunity = privateCommunity;
    }

    public UserAccount getCreator() {
        return creator;
    }

    public void setCreator(UserAccount creator) {
        this.creator = creator;
    }

    public CommunityPostingPolicy getPostingPolicy() {
        return postingPolicy == null ? CommunityPostingPolicy.ALL_MEMBERS_CAN_POST : postingPolicy;
    }

    public void setPostingPolicy(CommunityPostingPolicy postingPolicy) {
        this.postingPolicy = postingPolicy;
    }
}
