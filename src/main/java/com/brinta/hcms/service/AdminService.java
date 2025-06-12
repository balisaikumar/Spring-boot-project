package com.brinta.hcms.service;

import com.brinta.hcms.dto.AdminProfileDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.AdminProfile;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import org.springframework.data.domain.Page;

public interface AdminService {

    AdminProfile registerAdmin(RegisterAdminRequest request);

    TokenPair loginAdmin(LoginRequest request);

    Page<AdminProfileDto> getWithPagination(int age, int size);

    void delete(Long adminId);

}
