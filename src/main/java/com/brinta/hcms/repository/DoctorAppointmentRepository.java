package com.brinta.hcms.repository;

import com.brinta.hcms.entity.DoctorAppointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorAppointmentRepository extends JpaRepository<DoctorAppointment, Long> {

    Page<DoctorAppointment> findByDoctorId(Long doctorId, Pageable pageable);

}

