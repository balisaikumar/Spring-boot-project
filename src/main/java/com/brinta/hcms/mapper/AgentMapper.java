package com.brinta.hcms.mapper;

import com.brinta.hcms.dto.AgentDto;
import com.brinta.hcms.entity.Agent;
import com.brinta.hcms.request.updateRequest.AgentUpdate;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AgentMapper {

    // [DTO CONVERSION] Entity to DTO for API response
    AgentDto toDto(Agent agent);

    // [RAW MAPPING] Only use if you want to convert update request into new Agent (not ideal for update)
    Agent toEntity(AgentUpdate updateRequest);

    // [MERGE LOGIC] Apply only non-null fields from updateRequest to an existing Agent object
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateAgentFromDto(AgentUpdate updateRequest, @MappingTarget Agent agent);

}

