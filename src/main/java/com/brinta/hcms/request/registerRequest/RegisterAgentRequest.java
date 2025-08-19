package com.brinta.hcms.request.registerRequest;

import com.brinta.hcms.enums.AgentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterAgentRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "contactNumber is required")
    private String contactNumber;

    @NotBlank(message = "password is required")
    private String password;

    @NotBlank(message = "email is required")
    private String email;

    @NotNull(message = "Agent type is required")
    private AgentType agentType;

    @NotBlank(message = "panCard is required")
    private String panCard;

}

