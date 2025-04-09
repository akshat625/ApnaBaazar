package com.apnabaazar.apnabaazar.controller;
import com.apnabaazar.apnabaazar.mapper.Mapper;
import com.apnabaazar.apnabaazar.model.dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Role;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.CustomerRepository;
import com.apnabaazar.apnabaazar.repository.SellerRepository;
import com.apnabaazar.apnabaazar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;

    @GetMapping("/test")
    public String index(){
        return "Admin Page";
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponseDTO>> getAllCustomers() {
        List<CustomerResponseDTO> customerResponse = customerRepository.findAll().stream()
                .map(Mapper::fromCustomer)
                .toList();

        return ResponseEntity.ok(customerResponse);
    }

    @GetMapping("/sellers")
    public ResponseEntity<List<SellerResponseDTO>> getAllSellers() {
        List<SellerResponseDTO> sellerResponse = sellerRepository.findAll().stream()
                .map(Mapper::fromSeller)
                .toList();
        return ResponseEntity.ok(sellerResponse);
    }



}
