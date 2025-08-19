package com.brinta.hcms.repository;

import com.brinta.hcms.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {

    boolean existsByEmail(String email);

    boolean existsByContactNumber(String contactNumber);

    boolean existsByAgentCode(String agentCode);

    Optional<Agent> findByEmail(String email);

}

