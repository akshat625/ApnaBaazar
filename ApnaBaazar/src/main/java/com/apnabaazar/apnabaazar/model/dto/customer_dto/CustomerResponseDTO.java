package com.apnabaazar.apnabaazar.model.dto.customer_dto;

import lombok.*;


@Getter
@Setter
public class CustomerResponseDTO {
    private String id;
    private String fullName;
    private String email;
    private boolean isActive;

}
