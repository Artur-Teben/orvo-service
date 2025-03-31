package com.orvo.emailgenerator.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeadResponseDto {

    private String firstName;
    private String lastName;
    private String companyName;
    private String companyDomain;
    private String generatedEmail;
    private String status;

}
