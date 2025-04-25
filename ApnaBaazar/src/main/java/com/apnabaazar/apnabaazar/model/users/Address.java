package com.apnabaazar.apnabaazar.model.users;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "is_deleted = false")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String city;
    @Column(nullable = false)

    private String state;
    @Column(nullable = false)

    private String country;
    @Column(nullable = false)

    private String addressLine;
    @Column(nullable = false)

    private String zipCode;
    @Column(nullable = false)

    private String label;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}