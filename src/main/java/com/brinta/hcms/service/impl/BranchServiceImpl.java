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
import lombok.extern.slf4j.Slf4j;  // SLF4J logger
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
@Slf4j
public class BranchServiceImpl implements BranchService {

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchMapper branchMapper;

    @Override
    public Branch registerBranch(RegisterBranchRequest branchRequest) {
        log.info("Attempting to register branch: name={}, code={}", branchRequest.getBranchName(), branchRequest.getBranchCode());

        if (branchRepository.existsByBranchCode(branchRequest.getBranchCode())) {
            log.warn("Branch registration failed: duplicate branch code={}", branchRequest.getBranchCode());
            throw new DuplicateEntryException("Branch with code " + branchRequest.getBranchCode() + " already exists");
        }

        if (branchRepository.existsByBranchName(branchRequest.getBranchName())) {
            log.warn("Branch registration failed: duplicate branch name={}", branchRequest.getBranchName());
            throw new DuplicateEntryException("Branch with name '" + branchRequest.getBranchName() + "' already exists.");
        }

        Branch savedBranch = branchRepository.save(branchMapper.toEntity(branchRequest));
        log.info("Branch registered successfully: id={}, name={}, code={}",
                savedBranch.getId(), savedBranch.getBranchName(), savedBranch.getBranchCode());
        return savedBranch;
    }

    @Override
    public Page<Branch> getAllBranches(int page, int size) {
        log.info("Fetching all branches with pagination: page={}, size={}", page, size);

        if (page < 0 || size <= 0) {
            log.warn("Invalid pagination request: page={}, size={}", page, size);
            throw new InvalidRequestException("Page index must not be negative and size must be greater than zero.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Branch> branchPage = branchRepository.findAll(pageable);

        if (branchPage.isEmpty()) {
            log.info("No branches found for page={}, size={}", page, size);
            return Page.empty();
        }

        log.info("Fetched {} branches on page={}", branchPage.getNumberOfElements(), branchPage.getNumber());
        return branchPage;
    }

    @Override
    public List<Branch> findBy(Long branchId, String branchCode, String branchName) {
        log.info("Searching branch by params: id={}, code={}, name={}", branchId, branchCode, branchName);

        if (branchId == null &&
                (branchCode == null || branchCode.isBlank()) &&
                (branchName == null || branchName.isBlank())) {
            log.warn("Invalid search: no parameters provided");
            throw new InvalidRequestException("At least one search parameter must be provided");
        }

        Optional<Branch> branch = branchRepository.findByIdOrBranchCodeIgnoreCaseOrBranchName(branchId, branchCode, branchName);

        if (branch.isPresent()) {
            log.info("Branch found: id={}, name={}, code={}", branch.get().getId(),
                    branch.get().getBranchName(), branch.get().getBranchCode());
        } else {
            log.info("No branch found with given criteria");
        }

        return branch.map(List::of).orElse(List.of());
    }

    @Override
    public Branch updateBranch(Long branchId, UpdateBranchRequest request) {
        log.info("Updating branch: id={}, request={}", branchId, request);

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> {
                    log.warn("Update failed: branch not found with id={}", branchId);
                    return new ResourceNotFoundException("Branch not found with ID: " + branchId);
                });

        branchMapper.updateBranch(request, branch);

        Branch updatedBranch = branchRepository.save(branch);

        log.info("Branch updated successfully: id={}, name={}, code={}",
                updatedBranch.getId(), updatedBranch.getBranchName(), updatedBranch.getBranchCode());
        return updatedBranch;
    }

    @Override
    public void deleteBranch(Long id) {
        log.info("Deleting branch with id={}", id);

        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Delete failed: branch not found with id={}", id);
                    return new ResourceNotFoundException("Branch not found with id: " + id);
                });

        branchRepository.delete(branch);

        log.info("Branch deleted successfully: id={}", id);
    }

}

