package com.brinta.hcms.request.updateRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    private List<Long> branchIds;

}

