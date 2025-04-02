package com.apnabaazar.apnabaazar.model.token;

import com.apnabaazar.apnabaazar.model.users.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class UserVerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Instant expiresAt;

    public UserVerificationToken(String token, User user, Instant expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

}
