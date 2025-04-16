package com.apnabaazar.apnabaazar.controller;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.GenericResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.seller_dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.service.AdminService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/test")
    public String index(){
        return "Admin Page";
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerResponseDTO>> getCustomers(
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "0") int pageOffset,
            @RequestParam(defaultValue = "id") String sort
    ) {
        return ResponseEntity.ok(adminService.getCustomers(pageSize, pageOffset, sort));
    }

    @GetMapping("/sellers")
    public ResponseEntity<List<SellerResponseDTO>> getAllSellers(
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "0") int pageOffset,
            @RequestParam(defaultValue = "id") String sort
    ) {
        return ResponseEntity.ok(adminService.getSellers(pageSize, pageOffset, sort));
    }

    @PutMapping("/activate/customer")
    public ResponseEntity<GenericResponseDTO> activateCustomer(@RequestParam String id) throws MessagingException {
        return adminService.activateCustomer(id);
    }

    @PutMapping("/activate/seller")
    public ResponseEntity<GenericResponseDTO> activateSeller(@RequestParam String id) throws MessagingException {
        return adminService.activateSeller(id);
    }

    @PutMapping("/de-activate/customer")
    public ResponseEntity<GenericResponseDTO> deActivateCustomer(@RequestParam String id) throws MessagingException {
        return adminService.deActivateCustomer(id);
    }

    @PutMapping("/de-activate/seller")
    public ResponseEntity<GenericResponseDTO> deActivateSeller(@RequestParam String id) throws MessagingException {
        return adminService.deActivateSeller(id);
    }

}
