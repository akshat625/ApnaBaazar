package com.apnabaazar.apnabaazar.model.users;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    public Address(String addressLine, String city, String state, String country, String zipCode) {
    }
}