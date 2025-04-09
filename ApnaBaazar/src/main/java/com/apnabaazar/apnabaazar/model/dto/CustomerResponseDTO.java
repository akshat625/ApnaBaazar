package com.apnabaazar.apnabaazar.model.dto;

import com.apnabaazar.apnabaazar.model.users.Customer;
import lombok.*;


@Getter
@Setter

@NoArgsConstructor
public class CustomerResponseDTO {
    private String id;
    private String fullName;
    private String email;
    private boolean isActive;


    private static String buildFullName(String first, String middle, String last) {
        if (middle != null && !middle.isBlank()) {
            return first + " " + middle + " " + last;
        }
        return first + " " + last;
    }

}
