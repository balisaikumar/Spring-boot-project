package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.DoctorAppointmentDto;
import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.entity.Branch;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.entity.DoctorAppointment;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DoctorMapper {

    // For external doctors — do NOT map to user
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "referralCode", ignore = true)  // Will be set manually
    @Mapping(target = "user", ignore = true)          // External doctors don't have user
    Doctor register(RegisterDoctorRequest registerDoctor, @Context Branch branch);

    // Updating internal doctor
    @Mapping(target = "name", source = "name")
    @Mapping(target = "specialization", source = "specialization")
    @Mapping(target = "contactNumber", source = "contactNumber")
    @Mapping(target = "qualification", source = "qualification")
    void update(UpdateDoctorRequest updateDoctorRequest, @MappingTarget Doctor doctor);

    DoctorDto findBy(Doctor doctor);

    DoctorDto toDto(Doctor doctor);

    @Mapping(source = "patient.name", target = "patientName")
    DoctorAppointmentDto toDto(DoctorAppointment doctorAppointment);
}
