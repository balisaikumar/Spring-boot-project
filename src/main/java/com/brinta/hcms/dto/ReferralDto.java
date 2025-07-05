package com.brinta.hcms.dto;

import com.brinta.hcms.enums.ProfileStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReferralDto {

    // Will be set only for EXTERNAL_DOCTOR referrals
    private String doctorName;

    // Will be set only for other agent types
    private String agentName;

    private String patientName;

    private String patientContactNumber;

    private String area;

    private ProfileStatus profileStatus;

}

