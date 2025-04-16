package com.apnabaazar.apnabaazar.model.dto.customer_dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomerProfileDTO {

    private String id;
    private String firstName;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String middleName;
    private String lastName;
    private boolean isActive;
    private String contact;
    private String profileImageUrl;
}
