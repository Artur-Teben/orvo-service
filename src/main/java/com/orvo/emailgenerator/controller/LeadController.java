package com.orvo.emailgenerator.controller;

import com.orvo.emailgenerator.model.dto.response.LeadGenerationResponse;
import com.orvo.emailgenerator.model.dto.response.LeadResponseDto;
import com.orvo.emailgenerator.service.LeadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/leads")
public class LeadController {

    private final LeadService leadService;

    @PostMapping("/upload")
    public ResponseEntity<LeadGenerationResponse> generateLeads(@RequestParam("file") MultipartFile file) {
        log.info("Received request to generate leads from file: {}", file.getOriginalFilename());
        LeadGenerationResponse response = leadService.generateLeads(file);
        log.info("Generated leads with batchId: {}", response.getBatchId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{batchId}")
    public ResponseEntity<List<LeadResponseDto>> getLeads(@PathVariable("batchId") String batchId) {
        log.info("Received request to get leads for batchId: {}", batchId);
        List<LeadResponseDto> leads = leadService.getLeads(batchId);
        log.info("Retrieved {} leads for batchId: {}", leads.size(), batchId);
        return ResponseEntity.ok(leads);
    }

    @GetMapping("/{batchId}/download")
    public void downloadLeadsCsv(@PathVariable("batchId") String batchId, HttpServletResponse response) {
        log.info("Received request to download CSV for batchId: {}", batchId);

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"leads-" + batchId + ".csv\"");

        leadService.writeLeadsToCsv(batchId, response);
    }

}