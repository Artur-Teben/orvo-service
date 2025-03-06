package com.orvo.emailgenerator.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EmailComponents {

    private String firstName;
    private String lastName;
    private String companyDomain;

}
