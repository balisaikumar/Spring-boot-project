package com.brinta.hcms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorProfileDto {

    private Long id;

    private String name;

    private String contactNumber;

    private String specialization;

    private String qualification;

}

