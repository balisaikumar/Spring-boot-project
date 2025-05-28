package com.brinta.hcms.entity;

import com.brinta.hcms.enums.Roles;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false,unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Roles role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private PatientProfile patientProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference
    private DoctorProfile doctorProfile;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private AdminProfile adminProfile;

}
