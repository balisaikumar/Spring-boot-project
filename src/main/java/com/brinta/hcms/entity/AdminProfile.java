package com.brinta.hcms.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "admin_profiles")

public class AdminProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String contactNumber;

    @Column(nullable = false)
    private String email;

    @OneToOne(cascade = CascadeType.ALL)
    @JsonBackReference
    @JoinColumn(name = "user_id")
    private User user;

}

