package com.brinta.hcms.repository;

import com.brinta.hcms.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    boolean existsByEmail(String email);

    boolean existsByContactNumber(String contactNumber);

    Optional<Admin> findByEmail(String email);

    Optional<Admin> findByUserId(Long userId);

}
