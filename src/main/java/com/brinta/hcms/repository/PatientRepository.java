package com.brinta.hcms.repository;

import com.brinta.hcms.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE " +
            "u.patient.contactNumber = :contactNumber")
    boolean existsByPatientContactNumber(@Param("contactNumber") String contactNumber);

}
