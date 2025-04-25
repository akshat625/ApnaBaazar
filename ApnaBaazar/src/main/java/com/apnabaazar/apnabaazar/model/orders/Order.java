package com.apnabaazar.apnabaazar.model.orders;

import com.apnabaazar.apnabaazar.model.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "customer_user_id", nullable = false)
    private User customer;

    @Column(nullable = false)
    private Long amountPaid;

    @Column(nullable = false)
    private LocalDateTime dateCreated;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private String customerAddressCity;

    @Column(nullable = false)
    private String customerAddressState;

    @Column(nullable = false)
    private String customerAddressCountry;

    @Column(nullable = false)
    private String customerAddressLine;

    @Column(nullable = false)
    private String customerAddressZipCode;

    @Column(nullable = false)
    private String customerAddressLabel;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Order_Product> orderProducts = new HashSet<>();
}