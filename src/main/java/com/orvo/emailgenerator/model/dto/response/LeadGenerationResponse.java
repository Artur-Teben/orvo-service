package com.orvo.emailgenerator.model.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Builder
public class LeadGenerationResponse {

    private UUID batchId;
    private int totalLeads;
    private int validEmails;

    public LeadGenerationResponse(UUID batchId, int totalLeads, int validEmails) {
        this.batchId = batchId;
        this.totalLeads = totalLeads;
        this.validEmails = validEmails;
    }

}