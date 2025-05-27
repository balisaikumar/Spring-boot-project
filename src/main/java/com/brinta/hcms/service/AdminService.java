package com.brinta.hcms.service;

import com.brinta.hcms.dto.AdminProfileDto;
import com.brinta.hcms.entity.AdminProfile;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import com.brinta.hcms.request.registerRequest.LoginRequest;

public interface AdminService {

    AdminProfile registerAdmin(RegisterAdminRequest request);

    AdminProfileDto loginAdmin(LoginRequest request);

}
