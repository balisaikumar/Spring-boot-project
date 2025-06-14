package com.brinta.hcms.service;

import com.brinta.hcms.dto.DoctorDto;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterDoctorRequest;
import com.brinta.hcms.request.updateRequest.UpdateDoctorRequest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface DoctorService {

    Doctor register(RegisterDoctorRequest registerDoctor);

    Map<String, Object> doctorLogin(LoginRequest request);

    Doctor update(Long doctorId, UpdateDoctorRequest updateDoctorRequest);

    List<DoctorDto> findBy(Long doctorId, String contactNumber, String email);

    Page<DoctorDto> getWithPagination(int page, int size);

    void delete(Long doctorId);

}
