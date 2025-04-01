package com.apnabaazar.apnabaazar.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserVerificationDTO {
    private String email;
    private String verificationCode;
}
