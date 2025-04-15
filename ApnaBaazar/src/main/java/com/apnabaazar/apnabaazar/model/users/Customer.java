package com.apnabaazar.apnabaazar.model.users;
import com.apnabaazar.apnabaazar.model.orders.Cart;
import com.apnabaazar.apnabaazar.model.orders.Order;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "customer_id")
public class Customer extends User {

    @Column(name = "contact")
    private String contact;

    @OneToMany(mappedBy = "customer")
    private Set<Order> orders =  new HashSet<>();

    @OneToMany(mappedBy = "customer")
    private Set<Cart> carts;
}