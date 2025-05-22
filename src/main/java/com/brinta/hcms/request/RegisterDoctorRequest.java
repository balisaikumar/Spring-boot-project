package com.brinta.hcms.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDoctorRequest {

    @NotBlank(message = "Username is required")
    private String userName;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @NotBlank(message = "Phone is required")
    private String contactNumber;

    @NotBlank(message = "Qualification is required")
    private String qualification;

}

