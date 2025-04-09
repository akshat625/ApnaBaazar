package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.mapper.Mapper;
import com.apnabaazar.apnabaazar.model.dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.repository.CustomerRepository;
import com.apnabaazar.apnabaazar.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;


    public List<CustomerResponseDTO> getCustomers(int pageSize, int pageOffset, String sort) {
        Pageable pageable = PageRequest.of(pageOffset,pageSize, Sort.by(sort));
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        return customerPage.stream().map(Mapper::fromCustomer).toList();
    }

    public List<SellerResponseDTO> getSellers(int pageSize, int pageOffset, String sort) {
        Pageable pageable = PageRequest.of(pageOffset,pageSize, Sort.by(sort));
        Page<Seller> sellerPage = sellerRepository.findAll(pageable);
        return  sellerPage.stream().map(Mapper::fromSeller).toList();
    }
}
