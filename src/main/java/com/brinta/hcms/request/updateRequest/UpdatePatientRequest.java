package com.brinta.hcms.request.updateRequest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePatientRequest {

    private String name;

    private String email;

    private String address;

    private String contactNumber;

}
