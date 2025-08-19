package com.brinta.hcms.service;

import com.brinta.hcms.dto.AdminDto;
import com.brinta.hcms.dto.TokenPair;
import com.brinta.hcms.entity.Admin;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import org.springframework.data.domain.Page;

public interface AdminService {

    Admin registerAdmin(RegisterAdminRequest request);

    TokenPair loginAdmin(LoginRequest request);

    Page<AdminDto> getWithPagination(int age, int size);

    void delete(Long adminId);

}

