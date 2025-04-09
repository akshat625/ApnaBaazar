package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.users.Seller;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, String> {
}
