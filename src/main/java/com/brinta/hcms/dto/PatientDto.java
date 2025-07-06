package com.brinta.hcms.dto;

import com.brinta.hcms.enums.ProfileStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatientDto {

    private Long id;

    private String name;

    private String age;

    private String gender;

    private String contactNumber;

    private String address;

    private ProfileStatus profileStatus;

    private String branchName;

    private String adminEmail;

}

