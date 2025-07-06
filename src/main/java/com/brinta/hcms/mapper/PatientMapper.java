package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.PatientDto;
import com.brinta.hcms.entity.Branch;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.entity.User;
import com.brinta.hcms.request.registerRequest.ReferralRequest;
import com.brinta.hcms.request.registerRequest.RegisterPatientRequest;
import com.brinta.hcms.request.updateRequest.UpdatePatientRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PatientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "request.name", target = "name")
    @Mapping(source = "request.contactNumber", target = "contactNumber")
    @Mapping(source = "request.address", target = "address")
    @Mapping(source = "request.email", target = "email")
    @Mapping(source = "user", target = "user")
    @Mapping(source = "branch", target = "branch")
    Patient register(RegisterPatientRequest request, User user, Branch branch);

    void update(UpdatePatientRequest updatePatientRequest, @MappingTarget Patient patient);

    PatientDto findBy(Patient patient);

    PatientDto toDto(Patient patient);

    // Maps from ReferralRequest to a minimal Patient object for referral creation
    @Mapping(target = "id", ignore = true)
    @Mapping(source = "patientName", target = "name")
    @Mapping(source = "patientAge", target = "age")
    @Mapping(source = "patientGender", target = "gender")
    @Mapping(source = "patientContactNumber", target = "contactNumber")
    @Mapping(source = "patientAddress", target = "address")
    @Mapping(target = "user", ignore = true) // user is null at referral stage
    @Mapping(target = "email", ignore = true) // email not captured during referral
    @Mapping(target = "status", constant = "REFERRAL")
    @Mapping(target = "profileStatus", constant = "PENDING")
    Patient fromReferralRequest(ReferralRequest request);

}

