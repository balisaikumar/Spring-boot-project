    package com.brinta.hcms.dto;

    import com.brinta.hcms.enums.Roles;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class UserDto {

        private String username;

        private String email;

        private Roles role;

    }
