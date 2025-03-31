package com.orvo.emailgenerator.model;

import lombok.Getter;

@Getter
public enum LeadStatus {

    EMAIL_CREATED("Email Created"),
    INSUFFICIENT_COMPANY_INFO("Insufficient Company Info"),
    INCOMPLETE_LEAD_DATA("Incomplete Lead Data"),
    PROCESSING_ERROR("Processing Error");

    private final String description;

    LeadStatus(String description) {
        this.description = description;
    }

    public static LeadStatus getLeadStatus(String description) {
        for (LeadStatus leadStatus : LeadStatus.values()) {
            if (leadStatus.description.equals(description)) {
                return leadStatus;
            }
        }
        return null;
    }

}
