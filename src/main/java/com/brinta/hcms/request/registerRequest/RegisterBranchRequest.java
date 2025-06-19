package com.brinta.hcms.request.registerRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterBranchRequest {

    @NotBlank(message = "Branch name is required")
    private String branchName;

    @NotBlank(message = "Branch code is required")
    private String branchCode;

    @NotBlank(message = "Branch manager is required")
    private String branchManager;

    @NotNull(message = "Established date is required")
    private LocalDate establishedDate;

    @NotBlank(message = "Address is required")
    private String address;

}

