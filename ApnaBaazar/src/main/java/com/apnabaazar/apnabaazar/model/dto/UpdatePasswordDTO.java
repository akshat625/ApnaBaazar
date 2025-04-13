package com.apnabaazar.apnabaazar.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePasswordDTO {
    @NotBlank(message = "Old password is required.")
    private String oldPassword;

    @NotBlank(message = "New password is required.")
    @Size(min = 8, max = 15, message = "Password must be between 8 and 15 characters long")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=[\\]{};':\"\\\\|,.<>/?]).{8,15}$",
            message = "Password must contain at least 1 uppercase, 1 lowercase, 1 digit, 1 special character and be 8–15 characters long."
    )
    private String newPassword;


    @NotBlank(message = "Confirm password is required.")
    @Size(min = 8, max = 15, message = "Password must be between 8 and 15 characters long")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=[\\]{};':\"\\\\|,.<>/?]).{8,15}$",
            message = "Password must contain at least 1 uppercase, 1 lowercase, 1 digit, 1 special character and be 8–15 characters long."
    )
    private String confirmPassword;
}
