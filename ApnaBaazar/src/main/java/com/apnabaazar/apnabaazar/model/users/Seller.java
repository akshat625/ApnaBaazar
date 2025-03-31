package com.apnabaazar.apnabaazar.model.users;

import com.apnabaazar.apnabaazar.model.products.Product;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "seller_id")

public class Seller extends User {

    private String gst;

    private String companyContact;

    private String companyName;

    @OneToMany(mappedBy = "seller")
    private Set<Product> products =  new HashSet<>();

}
