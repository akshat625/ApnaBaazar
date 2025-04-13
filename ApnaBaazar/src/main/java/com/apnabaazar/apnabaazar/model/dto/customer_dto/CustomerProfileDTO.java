package com.apnabaazar.apnabaazar.model.dto.customer_dto;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerProfileDTO {

    private String id;
    private String firstName;
    private String lastName;
    private boolean isActive;
    private String contact;
    private String profileImageUrl;
}
