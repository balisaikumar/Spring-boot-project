package com.brinta.hcms.request.registerRequest;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReferralRequest {

    // For EXTERNAL_DOCTOR

    private String doctorName;

    private String doctorEmail;

    private String specialization;

    private String qualification;

    private String doctorContactNumber;

    // For other agents

    private String name;

    private String email;

    private String contactNumber;

    // Patient Details
    @NotBlank(message = "PatientName is required")
    private String patientName;

    @NotBlank(message = "PatientAge is required")
    private String patientAge;

    @NotBlank(message = "PatientGender is required")
    private String patientGender;

    @NotBlank(message = "PatientContactNumber is required")
    private String patientContactNumber;

    @NotBlank(message = "PatientAddress is required")
    private String patientAddress;

    @NotBlank(message = "Area is required")
    private String area;

}

