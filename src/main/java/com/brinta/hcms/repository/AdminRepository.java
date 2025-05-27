package com.brinta.hcms.repository;

import com.brinta.hcms.entity.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<AdminProfile, Long> {

    boolean existsByEmail(String email);

    boolean existsByContactNumber(String contactNumber);

    AdminProfile findByEmail(String email);
}
