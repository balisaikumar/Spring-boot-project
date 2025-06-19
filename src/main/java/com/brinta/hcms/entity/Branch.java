package com.brinta.hcms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
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

}

