package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.users.Seller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, String> {

    Page<Seller> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Optional<Seller> findByEmail(String email);
}
