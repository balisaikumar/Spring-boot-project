package com.brinta.hcms.repository;

import com.brinta.hcms.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<DoctorProfile, Long> {

    boolean existsByEmail(String mail);

    boolean existsByContactNumber(String contactNumber);

    Optional<DoctorProfile> findByIdOrContactNumberOrEmail(Long doctorID, String contactNumber, String email);

    Optional<DoctorProfile> findByEmail(String email);

}
