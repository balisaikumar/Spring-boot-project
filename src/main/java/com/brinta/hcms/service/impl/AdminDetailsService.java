package com.brinta.hcms.service.impl;

import com.brinta.hcms.entity.AdminProfile;
import com.brinta.hcms.entity.UserPrinciple;
import com.brinta.hcms.exception.exceptionHandler.InvalidRequestException;
import com.brinta.hcms.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminDetailsService implements UserDetailsService {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AdminProfile adminProfile = adminRepository.findByEmail(email);
        if (adminProfile == null){
            throw new InvalidRequestException("Email not found");
        }
        return new UserPrinciple(adminProfile);
    }

}
