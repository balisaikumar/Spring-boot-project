package com.brinta.hcms.service;

import com.brinta.hcms.dto.DoctorProfileDto;
import com.brinta.hcms.entity.DoctorProfile;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DoctorService {

    DoctorProfile register(RegisterDoctorRequest registerDoctor);

    DoctorProfile update(Long doctorId, UpdateDoctorRequest updateDoctorRequest);

    List<DoctorProfileDto> findBy(Long doctorId, String contactNumber, String email);

    Page<DoctorProfileDto> getWithPagination(int page, int size);

    void delete(Long doctorId);

}

