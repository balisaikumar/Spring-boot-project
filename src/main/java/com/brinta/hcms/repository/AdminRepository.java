package com.brinta.hcms.repository;

import com.brinta.hcms.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<AdminProfile, Long> {

    boolean existsByEmail(String email);

    boolean existsByContactNumber(String contactNumber);

    Optional<AdminProfile> findByEmail(String email);
}
