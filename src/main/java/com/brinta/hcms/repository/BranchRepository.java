package com.brinta.hcms.repository;

import com.brinta.hcms.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch,Long> {

    boolean existsByBranchCode(String branchCode);

    boolean existsByBranchName(String branchName);

    Optional<Branch> findByIdOrBranchCodeIgnoreCaseOrBranchName(Long branchId,
                                                                String branchCode, String branchName);

}

