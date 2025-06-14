package com.brinta.hcms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientDto {

    private String name;

    private String contactNumber;

    private String address;

    private UserDto user;

}
