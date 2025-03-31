package com.apnabaazar.apnabaazar.model.products;

import com.apnabaazar.apnabaazar.model.users.Customer;
import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProductReviewId implements Serializable {
    private String customer;
    private String product;
}
