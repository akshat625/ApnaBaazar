package com.apnabaazar.apnabaazar.repository;

import com.apnabaazar.apnabaazar.model.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    @Query("select COUNT(*) > 0 from Seller where LOWER(companyName) = LOWER(:companyName)")
    boolean existsByCompanyName(@Param("companyName") String companyName);


    @Query("select COUNT(*) > 0 from Seller where gst = :gst")
    boolean existsByGst(@Param("gst") String gst);


}
