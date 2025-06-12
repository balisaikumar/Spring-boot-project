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

    private String firstName;

    private String lastName;

    private String email;

    private String contactNumber;

}
