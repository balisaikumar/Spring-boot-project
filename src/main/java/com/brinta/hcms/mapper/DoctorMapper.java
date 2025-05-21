package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.request.RegisterRequest.RegisterDoctor;
import com.brinta.hcms.request.UpdateRequest.UpdateDoctorRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DoctorMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specialization", ignore = true)
    Doctor register(RegisterDoctor registerDoctor);

    void update(UpdateDoctorRequest updateDoctorRequest, @MappingTarget Doctor doctor);

    DoctorDto findBy (Doctor doctor);

    DoctorDto toDto (Doctor doctor);

}
