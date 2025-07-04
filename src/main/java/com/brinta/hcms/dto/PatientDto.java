package com.brinta.hcms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientDto {

    private String name;

    private String age;

    private String gender;

    private String contactNumber;

    private String address;

}

