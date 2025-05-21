package com.brinta.hcms.request.RegisterRequest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDoctor {

    @NotBlank(message = "first_name Required")
    private String firstName;

    @NotBlank(message = "last_name Required")
    private String lastName;

    @NotBlank(message = "gender")
    private String gender;

    @NotBlank(message = "email")
    private String email;

    @NotBlank(message = "contactNumber")
    private String contact;

    @NotNull(message = "experience")
    private int experience;

    @NotBlank(message = "qualification")
    private String qualification;

}
