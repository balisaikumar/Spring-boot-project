package com.brinta.hcms.mappers;

import com.brinta.hcms.entity.PatientProfile;
import com.brinta.hcms.request.RegisterPatientRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientMapper {

    @Mapping(target = "user.role", constant = "PATIENT")
    @Mapping(target = "user.password", ignore = true)
    @Mapping(target = "user.username", source = "userName")
    @Mapping(target = "user.email", source = "email")
    PatientProfile toEntity(RegisterPatientRequest request);

}

