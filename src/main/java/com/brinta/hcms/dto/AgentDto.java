package com.brinta.hcms.dto;

import com.brinta.hcms.enums.AgentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentDto {

    private String name;

    private String contactNumber;

    private String email;

    private String agentCode;

    private AgentType agentType;

}
