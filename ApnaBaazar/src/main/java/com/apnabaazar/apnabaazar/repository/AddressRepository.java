package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.users.Address;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AddressRepository extends JpaRepository<Address, String> {

    @Modifying
    @Query("UPDATE Address set deleted=true WHERE id=:id")
    void deleteAddressById(@Param("id") String id);
}
