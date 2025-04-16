package com.apnabaazar.apnabaazar.model.users;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;


@Entity
@Table(name = "addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@SQLDelete(sql = "UPDATE addresses SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String city;

    private String state;

    private String country;

    private String addressLine;

    private String zipCode;

    private String label;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}