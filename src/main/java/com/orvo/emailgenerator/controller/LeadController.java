package com.orvo.emailgenerator.controller;

import com.orvo.emailgenerator.model.LeadResponseDto;
import com.orvo.emailgenerator.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@RestController("/leads")
public class LeadController {

    private final LeadService leadService;

    @PostMapping("/generate")
    public List<LeadResponseDto> generateLeads(@RequestParam("file") MultipartFile file) {
        return leadService.generateLeads(file);
    }
}
