package com.orvo.emailgenerator.model.mapper;

import com.orvo.emailgenerator.model.LeadStatus;
import com.orvo.emailgenerator.model.dto.response.LeadResponseDto;
import com.orvo.emailgenerator.model.entity.Lead;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface LeadMapper {

//    @Mapping(ignore = true, target = "id")
//    @Mapping(ignore = true, target = "batchId")
//    @Mapping(ignore = true, target = "createdAt")
    @Mapping(source = "status", target = "status", qualifiedByName = "mapLeadStatus")
    LeadResponseDto toLeadResponseDto(Lead lead);

    @Named("mapLeadStatus")
    default String mapLeadStatus(LeadStatus status) {
        return status != null ? status.getDescription() : "UNKNOWN";
    }
}
