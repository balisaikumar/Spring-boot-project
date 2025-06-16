package com.brinta.hcms.request.updateRequest;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RescheduleAppointmentRequest {

    @NotNull
    private LocalDateTime newAppointmentTime;

}

