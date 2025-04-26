package com.apnabaazar.apnabaazar.model.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminProfileUpdateDTO {

    @Size(min = 2, max = 50, message = "{first.name.size}")
    @Pattern(regexp = "^[A-Za-z]+$", message = "{first.name.pattern}")
    private String firstName;

    @Size(max = 50, message = "{middle.name.size}")
    @Pattern(regexp = "^[A-Za-z]*$", message = "{middle.name.pattern}")
    private String middleName;

    @Size(min = 2, max = 50, message = "{last.name.size}")
    @Pattern(regexp = "^[A-Za-z]+$", message = "{last.name.pattern}")
    private String lastName;

}
