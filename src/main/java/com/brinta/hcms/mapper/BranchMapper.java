package com.brinta.hcms.mapper;

import com.brinta.hcms.entity.Branch;
import com.brinta.hcms.request.registerRequest.RegisterBranchRequest;
import com.brinta.hcms.request.updateRequest.UpdateBranchRequest;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    @Mapping(target = "id", ignore = true)
    Branch toEntity(RegisterBranchRequest registerBranch);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branchCode", ignore = true)
    @Mapping(target = "establishedDate", ignore = true)
    void updateBranch(UpdateBranchRequest request, @MappingTarget Branch entity);

}

