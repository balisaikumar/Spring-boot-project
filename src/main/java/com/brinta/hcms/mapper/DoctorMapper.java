package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DoctorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user.email", source = "email")
    @Mapping(target = "user.username", source = "userName")
    @Mapping(target = "user.role", ignore = true)
    @Mapping(target = "user.password", ignore = true)
    Doctor register(RegisterDoctorRequest registerDoctor);

    void update(UpdateDoctorRequest updateDoctorRequest, @MappingTarget Doctor doctor);

    DoctorDto findBy (Doctor doctor);

    DoctorDto toDto (Doctor doctor);

}
