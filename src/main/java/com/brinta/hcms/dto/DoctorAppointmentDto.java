package com.brinta.hcms.dto;

import com.brinta.hcms.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorAppointmentDto {

    private LocalDateTime appointmentTime;

    private AppointmentStatus status;

    private String patientName;

}

