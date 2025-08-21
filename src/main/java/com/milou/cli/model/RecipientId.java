package com.milou.cli.model;

import java.io.Serializable;
import java.util.Objects;

public class RecipientId implements Serializable {
    private Long email;
    private Long user;

    public RecipientId() {}
    public RecipientId(Long email, Long user) {
        this.email = email;
        this.user = user;
    }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecipientId)) return false;
        RecipientId that = (RecipientId) o;
        return Objects.equals(email, that.email) && Objects.equals(user, that.user);
    }
    @Override public int hashCode() {
        return Objects.hash(email, user);
    }
}
