package com.brinta.hcms.entity;

import com.brinta.hcms.enums.Roles;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

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
    private Patient patient;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Doctor doctor;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private Admin admin;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "token_expiry_date")
    private LocalDateTime tokenExpiryDate;

}

