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
@Table(name = "listing_messages")
public class ListingMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private ListingConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id")
    private UserAccount sender;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(name = "attachment_storage_key", length = 255)
    private String attachmentStorageKey;

    @Column(name = "attachment_content_type", length = 120)
    private String attachmentContentType;

    @Column(name = "attachment_file_size")
    private Long attachmentFileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "attachment_type", length = 20)
    private MediaType attachmentType;

    public ListingConversation getConversation() {
        return conversation;
    }

    public void setConversation(ListingConversation conversation) {
        this.conversation = conversation;
    }

    public UserAccount getSender() {
        return sender;
    }

    public void setSender(UserAccount sender) {
        this.sender = sender;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAttachmentStorageKey() {
        return attachmentStorageKey;
    }

    public void setAttachmentStorageKey(String attachmentStorageKey) {
        this.attachmentStorageKey = attachmentStorageKey;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public void setAttachmentContentType(String attachmentContentType) {
        this.attachmentContentType = attachmentContentType;
    }

    public Long getAttachmentFileSize() {
        return attachmentFileSize;
    }

    public void setAttachmentFileSize(Long attachmentFileSize) {
        this.attachmentFileSize = attachmentFileSize;
    }

    public MediaType getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(MediaType attachmentType) {
        this.attachmentType = attachmentType;
    }

    public boolean hasAttachment() {
        return attachmentStorageKey != null && !attachmentStorageKey.isBlank();
    }
}
