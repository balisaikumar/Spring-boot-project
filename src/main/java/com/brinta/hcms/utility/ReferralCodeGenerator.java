package com.brinta.hcms.utility;

import com.brinta.hcms.enums.AgentType;
import org.springframework.stereotype.Component;

import java.time.Year;
import java.util.UUID;

@Component
public class ReferralCodeGenerator {

    private static final int RANDOM_CODE_LENGTH = 6;

    public String generateReferralCode(AgentType type, String fullName) {
        String year = String.valueOf(Year.now().getValue());

        // Extract and sanitize the first part of the name (remove non-alphabet characters)
        String name = fullName.split(" ")[0]
                .toUpperCase()
                .replaceAll("[^A-Z]", "");

        // Generate random 6-character alphanumeric string
        String random = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, RANDOM_CODE_LENGTH)
                .toUpperCase();

        // Prefix based on agentType
        String prefix = switch (type) {
            case EXTERNAL_DOCTOR -> "HCMS_EXTDOC_";
            case EXTERNAL_STAFF -> "HCMS_CMN_";
            case PHARMACY -> "HCMS_PHARM_";
            case WELLNESS_PARTNER -> "HCMS_WELL_";
            case CAMPAIGN_AGENT -> "HCMS_CAMP_";
            default -> "HCMS_AGT_";
        };

        return prefix + year + "_" + name + "_" + random;
    }
}
