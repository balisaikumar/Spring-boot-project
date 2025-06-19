package com.brinta.hcms.service.impl;

import com.brinta.hcms.entity.Branch;
import com.brinta.hcms.exception.exceptionHandler.DuplicateEntryException;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.exception.exceptionHandler.ResourceNotFoundException;
import com.brinta.hcms.mapper.BranchMapper;
import com.brinta.hcms.repository.BranchRepository;
import com.brinta.hcms.request.registerRequest.RegisterBranchRequest;
import com.brinta.hcms.request.updateRequest.UpdateBranchRequest;
import com.brinta.hcms.service.BranchService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@AllArgsConstructor
@NoArgsConstructor
public class BranchServiceImpl implements BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchMapper branchMapper;

    @Override
    public Branch registerBranch(RegisterBranchRequest branchRequest) {

        if(branchRepository.existsByBranchCode(branchRequest.getBranchCode())){
            throw new DuplicateEntryException("Branch with Code" + branchRequest.getBranchCode() + "already exists");
        }

        if (branchRepository.existsByBranchName(branchRequest.getBranchName())) {
            throw new DuplicateEntryException("Branch with name '" + branchRequest.getBranchName() + "' already exists.");
        }

        return branchRepository.save(branchMapper.toEntity(branchRequest));
    }

    @Override
    public Page<Branch> getAllBranches(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new InvalidRequestException("Page index must not be negative and size must be greater than zero.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Branch> branchPage = branchRepository.findAll(pageable);

        if (branchPage.isEmpty()) {
            return Page.empty();
        }

        return branchPage;
    }

    @Override
    public List<Branch> findBy(Long branchId, String branchCode, String branchName) {

        if (branchId == null &&
                (branchCode == null || branchCode.isBlank()) &&
                (branchName == null || branchName.isBlank())) {
            throw new InvalidRequestException("At least one search parameter must be provided");
        }

        Optional<Branch> branch = branchRepository
                .findByIdOrBranchCodeIgnoreCaseOrBranchName(branchId, branchCode, branchName);

        return branch.map(List::of).orElse(List.of());
    }

    @Override
    public Branch updateBranch(Long branchId, UpdateBranchRequest request) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with ID: " + branchId));

        branchMapper.updateBranch(request, branch);

        return branchRepository.save(branch);
    }

    @Override
    public void deleteBranch(Long id) {

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
        branchRepository.delete(branch);

    }

}

