package com.brinta.hcms.mappers;

import com.brinta.hcms.dto.UserDto;
import com.brinta.hcms.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

}

