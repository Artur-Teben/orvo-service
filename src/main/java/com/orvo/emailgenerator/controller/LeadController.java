package com.orvo.emailgenerator.controller;

import com.orvo.emailgenerator.model.dto.response.LeadGenerationResponse;
import com.orvo.emailgenerator.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/leads")
public class LeadController {

    private final LeadService leadService;

    @PostMapping("/upload")
    public ResponseEntity<LeadGenerationResponse> generateLeads(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(leadService.generateLeads(file));
    }
}
