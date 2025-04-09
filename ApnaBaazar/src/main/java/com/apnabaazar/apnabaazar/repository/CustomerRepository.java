package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.users.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
}
