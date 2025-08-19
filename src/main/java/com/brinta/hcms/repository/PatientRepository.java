package com.brinta.hcms.repository;

import com.brinta.hcms.entity.Patient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByEmail(String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE " +
            "u.patient.contactNumber = :contactNumber")
    boolean existsByPatientContactNumber(@Param("contactNumber") String contactNumber);

    @Query("SELECT p FROM Patient p JOIN FETCH p.user WHERE " +
            "(:id IS NULL OR p.id = :id) AND " +
            "(:contactNumber IS NULL OR p.contactNumber = :contactNumber) AND " +
            "(:email IS NULL OR p.email = :email)")
    List<Patient> findByParams(
            @Param("id") Long id,
            @Param("contactNumber") String contactNumber,
            @Param("email") String email
    );

    @Query("SELECT p FROM Patient p JOIN FETCH p.user")
        List<Patient> findAllWithUser(Pageable pageable);

    Optional<Patient> findByContactNumber(String contactNumber);

}

