package com.brinta.hcms.request.registerRequest;

import com.brinta.hcms.enums.AgentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @NotBlank(message = "specialization is required")
    private String specialization;

    @NotBlank(message = "Phone is required")
    private String contactNumber;

    @NotBlank(message = "Qualification is required")
    private String qualification;

    private AgentType agentType; // Optional for external doctors only

    @NotNull(message = "Branch ID is required")
    private Long branchId;

}

