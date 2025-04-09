package com.apnabaazar.apnabaazar.controller;
import com.apnabaazar.apnabaazar.mapper.Mapper;
import com.apnabaazar.apnabaazar.model.dto.CustomerResponseDTO;
import com.apnabaazar.apnabaazar.model.dto.SellerResponseDTO;
import com.apnabaazar.apnabaazar.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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



}
