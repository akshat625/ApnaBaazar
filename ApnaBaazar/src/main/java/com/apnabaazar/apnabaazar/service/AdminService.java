package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.exceptions.UserNotFoundException;
import com.apnabaazar.apnabaazar.mapper.Mapper;
import com.apnabaazar.apnabaazar.model.dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.model.users.Customer;
import com.apnabaazar.apnabaazar.model.users.Seller;
import com.apnabaazar.apnabaazar.model.users.User;
import com.apnabaazar.apnabaazar.repository.CustomerRepository;
import com.apnabaazar.apnabaazar.repository.SellerRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AdminService {

    private final CustomerRepository customerRepository;
    private final SellerRepository sellerRepository;
    private final EmailService emailService;


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

    public ResponseEntity<GenericResponseDTO> activateCustomer(String  id) throws MessagingException {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Customer not found with this ID."));

        if (customer.isActive()) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Customer is already active."));
        }
        customer.setActive(true);
        customerRepository.save(customer);
        emailService.sendVerificationSuccessEmail(customer.getEmail(),"Account Activated");

        return ResponseEntity.ok(new GenericResponseDTO(true, "Customer account activated successfully."));
    }

    public ResponseEntity<GenericResponseDTO> activateSeller(String id) throws MessagingException {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Seller not found with this ID."));

        if (seller.isActive()) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Seller is already active."));
        }
        seller.setActive(true);
        sellerRepository.save(seller);
        emailService.sendVerificationSuccessEmail(seller.getEmail(),"Account Activated");

        return ResponseEntity.ok(new GenericResponseDTO(true, "Seller account activated successfully."));
    }

    public ResponseEntity<GenericResponseDTO> deActivateCustomer(String id) throws MessagingException {

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Customer not found with this ID."));
        if (!customer.isActive()) {
            return ResponseEntity.ok(new GenericResponseDTO(true, "Customer is already deactivate."));
        }
        customer.setActive(false);
        customerRepository.save(customer);
        emailService.sendAccountDeactivationEmail(customer.getEmail(),"Account Deactivated");
        return ResponseEntity.ok(new GenericResponseDTO(true, "Account deactivated successfully."));
    }


}
