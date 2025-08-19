package com.brinta.hcms.service;

import com.brinta.hcms.entity.Branch;
import com.brinta.hcms.request.registerRequest.RegisterBranchRequest;
import com.brinta.hcms.request.updateRequest.UpdateBranchRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BranchService {

    Branch registerBranch(RegisterBranchRequest branchRequest);

    Page<Branch> getAllBranches(int page, int size);

    List<Branch> findBy(Long branchId, String branchCode, String branchName);

    Branch updateBranch(Long branchId, UpdateBranchRequest request);

    void deleteBranch(Long id);

}

