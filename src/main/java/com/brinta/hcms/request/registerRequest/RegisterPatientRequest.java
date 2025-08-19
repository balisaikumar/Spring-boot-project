package com.brinta.hcms.request.registerRequest;

import com.brinta.hcms.enums.PatientRegistrationStatus;
import com.brinta.hcms.enums.ProfileStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPatientRequest {

    @NotBlank(message = "Username is required")
    private String userName;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Age is required")
    private String age;

    @NotBlank(message = "Gender is required")
    private String gender;

    @NotBlank(message = "Contact number is required")
    private String contactNumber;

    @NotBlank(message = "Address is required")
    private String address;

    private PatientRegistrationStatus status;

    private ProfileStatus profileStatus;

    @NotNull
    private Long branchId;

}

