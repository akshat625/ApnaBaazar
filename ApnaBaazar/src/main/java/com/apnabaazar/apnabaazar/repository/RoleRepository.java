package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.users.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByAuthority(String authority);
}
