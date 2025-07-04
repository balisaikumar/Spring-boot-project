package com.brinta.hcms.repository;

import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.entity.Doctor;
import com.brinta.hcms.entity.Patient;
import com.brinta.hcms.entity.Referral;
import com.brinta.hcms.enums.AgentType;
import com.brinta.hcms.enums.ProfileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    // Get paginated referrals
    Page<Referral> findAll(Pageable pageable);

    Page<Referral> findByProfileStatus(ProfileStatus profileStatus, Pageable pageable);

    // New method to check if the agent already referred the patient
    boolean existsByAgentAndPatient(Agent agent, Patient patient);

    List<Referral> findByAgent_AgentType(AgentType agentType);

    List<Referral> findByAgent(Agent agent);

}

