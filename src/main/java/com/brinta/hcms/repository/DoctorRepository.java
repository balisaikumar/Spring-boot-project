package com.brinta.hcms.repository;

import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    boolean existsByEmail(String mail);

    boolean existsByContactNumber(String contactNumber);

    Optional<Doctor> findByIdOrContactNumberOrEmail(Long doctorID, String contactNumber, String email);

    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByUserId(Long userId);

    Optional<Doctor> findByEmailAndContactNumber(String email, String contactNumber);

    Optional<Doctor> findByAgent(Agent agent);

}

