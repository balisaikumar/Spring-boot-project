package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.AdminProfileDto;
import com.brinta.hcms.entity.AdminProfile;
import com.brinta.hcms.request.registerRequest.RegisterAdminRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user.email", source = "email")
    @Mapping(target = "user.username", source = "userName")
    @Mapping(target = "user.role", ignore = true)
    @Mapping(target = "user.password", ignore = true)
    AdminProfile register(RegisterAdminRequest registerAdminRequest);

    AdminProfileDto toDto(AdminProfile admin);

}
