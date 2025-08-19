package com.brinta.hcms.request.updateRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentUpdate {

    private String name;

    private String email;

    private String contactNumber;

}

