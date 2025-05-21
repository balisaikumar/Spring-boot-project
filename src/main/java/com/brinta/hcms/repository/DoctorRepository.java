package com.brinta.hcms.repository;

import com.brinta.hcms.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    boolean existsByEmail(String mail);

    boolean existsByContact(String contact);

    Optional<Doctor> findByIdOrContactOrEmail(Long doctorID, String contact, String email);

}
