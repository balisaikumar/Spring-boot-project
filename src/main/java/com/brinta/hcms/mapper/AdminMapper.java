package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.AdminDto;
import com.brinta.hcms.entity.Admin;
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
    Admin register(RegisterAdminRequest registerAdminRequest);

    AdminDto toDto(Admin admin);

}
