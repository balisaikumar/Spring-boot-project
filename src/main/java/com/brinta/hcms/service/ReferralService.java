package com.brinta.hcms.service;

import com.brinta.hcms.dto.ReferralDto;
import com.brinta.hcms.enums.AgentType;
import com.brinta.hcms.enums.ProfileStatus;
import com.brinta.hcms.request.registerRequest.ReferralRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ReferralService {

    ReferralDto createReferral(ReferralRequest referralRequest);

    ReferralDto getReferralById(Long referralId);

    List<ReferralDto> getReferralsForCurrentAgent();

    Page<ReferralDto> getAllReferralsWithPagination(int page, int size);

    List<ReferralDto> getReferralsByAgentType(AgentType agentType);

    Page<ReferralDto> getReferralsByProfileStatus(ProfileStatus status, int page, int size);

    void deleteReferralById(Long id);

}

