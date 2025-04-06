package com.apnabaazar.apnabaazar.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordDTO {

    private String token;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 15, message = "Password must be between 8 and 15 characters long")
    @Pattern(
            regexp = "^(?=.[A-Z])(?=.[a-z])(?=.\\d)(?=.[@$!%?&])[A-Za-z\\d@$!%?&]{8,15}$",
            message = "Password must contain at least 1 uppercase, 1 lowercase, 1 digit, and 1 special character"
    )
    private String password;

    @NotBlank(message = "Confirm Password is required")
    private String confirmPassword;

}
