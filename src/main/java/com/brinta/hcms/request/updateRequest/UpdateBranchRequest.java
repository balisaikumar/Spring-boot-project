package com.brinta.hcms.request.updateRequest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateBranchRequest {

    private String branchName;

    private String branchManager;

    private String address;

}

