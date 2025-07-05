package com.brinta.hcms.request.updateRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDoctorRequest {

    private String name;

    private String email;

    private String specialization;

    private String contactNumber;

    private String qualification;

}

