package com.brinta.hcms.service;

import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.request.registerRequest.RegisterDoctor;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DoctorService {

    Doctor register(RegisterDoctor registerDoctor);

    Doctor update(Long doctorId, UpdateDoctorRequest updateDoctorRequest);

    List<DoctorDto> findBy(Long doctorId, String contact, String email);

    Page<DoctorDto> getWithPagination(int page, int size);

    void delete(Long doctorId);

}
