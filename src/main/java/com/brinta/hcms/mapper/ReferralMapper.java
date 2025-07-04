package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.ReferralDto;
import com.brinta.hcms.entity.Referral;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ReferralMapper {

    // Maps Referral entity to ReferralDto for response
    @Mapping(source = "doctor.name", target = "doctorName")
    @Mapping(source = "agent.name", target = "agentName")
    @Mapping(source = "patient.name", target = "patientName")
    @Mapping(source = "patient.contactNumber", target = "patientContactNumber")
    ReferralDto createReferral(Referral referral);

    // List of entities to DTOs
    List<ReferralDto> createReferralList(List<Referral> referrals);

}

