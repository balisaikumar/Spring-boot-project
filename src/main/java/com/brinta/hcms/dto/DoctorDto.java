package com.brinta.hcms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorDto {

    private String id;

    private String name;

    private String specialization;

    private String email;

    private String contactNumber;

}

