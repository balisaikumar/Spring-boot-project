package com.brinta.hcms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "branch")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String branchName;

    @Column(nullable = false, unique = true)
    private String branchCode;

    @Column(nullable = false)
    private String branchManager;

    @Column(nullable = false)
    private LocalDate establishedDate;

    // Flat address string for now, will be refactored to Address entity in future
    @Column(nullable = false)
    private String address;

    @ManyToMany(mappedBy = "branches")
    @JsonIgnore
    private Set<Doctor> doctors = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "admin_id", referencedColumnName = "id")
    private User admin;

    @OneToMany(mappedBy = "branch", cascade = CascadeType.ALL)
    @JsonIgnore
    private Set<Patient> patients = new HashSet<>();

}

