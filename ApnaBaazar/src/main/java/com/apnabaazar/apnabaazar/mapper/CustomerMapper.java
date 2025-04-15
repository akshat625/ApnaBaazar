package com.apnabaazar.apnabaazar.mapper;

import com.apnabaazar.apnabaazar.model.dto.AddressDTO;
import com.apnabaazar.apnabaazar.model.dto.customer_dto.CustomerProfileDTO;
import com.apnabaazar.apnabaazar.model.users.Address;
import com.apnabaazar.apnabaazar.model.users.Customer;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CustomerMapper {

    public static CustomerProfileDTO toCustomerProfileDTO(Customer customer, String  imageUrl) {
        return CustomerProfileDTO.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .isActive(customer.isActive())
                .contact(customer.getContact())
                .profileImageUrl(imageUrl)
                .build();
    }

    public static List<AddressDTO> toAllAddressDTO(Set<Address> customerAddresses) {
        if (customerAddresses == null || customerAddresses.isEmpty()) {
            return Collections.emptyList();
        }
        return customerAddresses
                .stream()
                .map(address -> {
                    AddressDTO addressDTO = new AddressDTO();
                    addressDTO.setAddressLine(address.getAddressLine());
                    addressDTO.setCity(address.getCity());
                    addressDTO.setCountry(address.getCountry());
                    addressDTO.setState(address.getState());
                    addressDTO.setZipCode(address.getZipCode());
                    return addressDTO;
                }).toList();
    }

    public static Address toAddressDTO(AddressDTO addressDTO) {
        Address address = new Address();
        address.setAddressLine(addressDTO.getAddressLine());
        address.setCity(addressDTO.getCity());
        address.setCountry(addressDTO.getCountry());
        address.setState(addressDTO.getState());
        address.setZipCode(addressDTO.getZipCode());

        return address;
    }
}
