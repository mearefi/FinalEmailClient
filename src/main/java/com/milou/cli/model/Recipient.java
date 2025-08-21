package com.milou.cli.model;

import jakarta.persistence.*;

@Entity
@Table(name = "recipients")
@IdClass(RecipientId.class)
public class Recipient {
    @Id
    @ManyToOne
    @JoinColumn(name = "email_id")
    private Email email;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    public Email getEmail() {
        return email;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
