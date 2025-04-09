package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.users.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SellerRepository extends JpaRepository<Seller, String> {

    Page<Seller> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}
