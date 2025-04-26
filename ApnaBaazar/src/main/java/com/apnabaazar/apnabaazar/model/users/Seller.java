package com.apnabaazar.apnabaazar.model.users;

import com.apnabaazar.apnabaazar.model.products.Product;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sellers",uniqueConstraints = {
        @UniqueConstraint(columnNames = "gst"),
        @UniqueConstraint(columnNames = "company_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PrimaryKeyJoinColumn(name = "seller_id")

public class Seller extends User {

    @Column(nullable = false)
    private String gstin;

    @Column(nullable = false)
    private String companyContact;

    @Column(nullable = false)
    private String companyName;

    @OneToMany(mappedBy = "seller")
    private Set<Product> products =  new HashSet<>();

}
