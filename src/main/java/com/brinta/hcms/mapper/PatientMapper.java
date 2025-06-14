package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.PatientDto;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.entity.User;
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
    Patient register(RegisterPatientRequest request, User user);

    void update(UpdatePatientRequest updatePatientRequest, @MappingTarget Patient patient);

    PatientDto toDto(Patient patient);

}
