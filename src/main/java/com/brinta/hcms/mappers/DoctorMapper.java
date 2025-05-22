package com.brinta.hcms.mappers;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.entity.DoctorProfile;
import com.brinta.hcms.request.RegisterDoctorRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DoctorMapper {

    @Mapping(target = "user.email", source = "email")
    @Mapping(target = "user.username", source = "userName")
    @Mapping(target = "user.role", ignore = true)
    @Mapping(target = "user.password", ignore = true)
    DoctorProfile toEntity(RegisterDoctorRequest request);

    DoctorProfileDto toDto(DoctorProfile doctorProfile);

}

