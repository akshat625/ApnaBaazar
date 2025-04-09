package com.apnabaazar.apnabaazar.model.dto;

import lombok.*;


@Getter
@Setter
public class CustomerResponseDTO {
    private String id;
    private String fullName;
    private String email;
    private boolean isActive;

}
