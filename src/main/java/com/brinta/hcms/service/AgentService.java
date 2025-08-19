package com.brinta.hcms.service;

import com.brinta.hcms.dto.AgentDto;
import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.request.registerRequest.LoginRequest;
import com.brinta.hcms.request.registerRequest.RegisterAgentRequest;
import com.brinta.hcms.request.updateRequest.AgentUpdate;

import java.util.Map;

public interface AgentService {

    AgentDto registerAgent(RegisterAgentRequest registerAgentRequest);

    Map<String, Object> agentLogin(LoginRequest request);

    Agent updateAgent(Long agentId, AgentUpdate updateRequest);

}

