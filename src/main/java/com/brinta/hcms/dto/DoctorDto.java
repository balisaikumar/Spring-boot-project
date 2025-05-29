package com.brinta.hcms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DoctorDto {

    private String firstName;

    private String lastName;

    private String email;

    private String contact;

}
