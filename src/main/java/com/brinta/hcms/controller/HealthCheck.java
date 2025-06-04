package com.brinta.hcms.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/hcms-java/health")
public class HealthCheck {

    @GetMapping("/check")
    public String healthCheck() {
        return "OK";
    }

}
