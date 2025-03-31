package com.orvo.emailgenerator.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.orvo.emailgenerator.model.*;
import com.orvo.emailgenerator.model.dto.response.LeadGenerationResponse;
import com.orvo.emailgenerator.model.dto.response.LeadResponseDto;
import com.orvo.emailgenerator.model.entity.Lead;
import com.orvo.emailgenerator.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private static final List<Function<EmailComponents, String>> EMAIL_PATTERNS = Arrays.asList(
            // Pattern: {first}.{last}@domain.com
            comp -> comp.getFirstName().toLowerCase() + "." + comp.getLastName().toLowerCase() + "@" + comp.getCompanyDomain(),
            // Pattern: {first}{last}@domain.com
            comp -> comp.getFirstName().toLowerCase() + comp.getLastName().toLowerCase() + "@" + comp.getCompanyDomain(),
            // Pattern: {firstInitial}{last}@domain.com
            comp -> comp.getFirstName().substring(0, 1).toLowerCase() + comp.getLastName().toLowerCase() + "@" + comp.getCompanyDomain(),
            // Pattern: {first}@domain.com
            comp -> comp.getFirstName().toLowerCase() + "@" + comp.getCompanyDomain(),
            // Pattern: {first}.{lastInitial}@domain.com
            comp -> comp.getFirstName().toLowerCase() + "." + comp.getLastName().substring(0, 1).toLowerCase() + "@" + comp.getCompanyDomain()
    );

    private final LeadRepository leadRepository;
    private final DnsLookupService dnsLookupService;
    private final SmtpEmailVerifier smtpEmailVerifier;

    public LeadGenerationResponse generateLeads(MultipartFile file) {
        List<LeadResponseDto> leads = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            CSVReader csvReader = new CSVReader(reader);
            List<String[]> rows = csvReader.readAll();

            // Validate headers
            if (rows.isEmpty() || !isValidHeader(rows.getFirst())) {
                throw new IllegalArgumentException("Invalid CSV headers. Expected: first_name, last_name, company_name, company_domain");
            }

            // Process each data row
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                String firstName = row[0];
                String lastName = row[1];
                String companyName = row[2];
                String companyDomain = (row.length > 3 && !row[3].isEmpty()) ? row[3] : deriveDomainFromCompany(companyName);

                LeadResponseDto dto = generateLead(firstName, lastName, companyName, companyDomain);
                leads.add(dto);
            }

            UUID batchId = save(leads);

            // Calculate response metrics
            int totalLeads = leads.size();
            int validEmails = (int) leads.stream()
                    .filter(lead -> lead.getStatus().equals(LeadStatus.EMAIL_CREATED.getDescription()))
                    .count();
            return LeadGenerationResponse.builder()
                    .batchId(batchId)
                    .totalLeads(totalLeads)
                    .validEmails(validEmails)
                    .build();
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
    }

    private UUID save(List<LeadResponseDto> leads) {
        UUID batchId = UUID.randomUUID();

        List<Lead> entities = leads.stream()
                .map(dto -> Lead.builder()
                        .batchId(batchId)
                        .firstName(dto.getFirstName())
                        .lastName(dto.getLastName())
                        .companyName(dto.getCompanyName())
                        .companyDomain(dto.getCompanyDomain())
                        .generatedEmail(dto.getGeneratedEmail())
                        .status(LeadStatus.getLeadStatus(dto.getStatus()))
                        .build())
                .toList();

        leadRepository.saveAll(entities);
        return batchId;
    }

    /**
     * A stub for deriving the domain from a company name.
     * In a real implementation, this might query a public API or use a lookup table.
     */
    private String deriveDomainFromCompany(String companyName) {
        // For now, simply return a dummy domain or try a basic conversion.
        // E.g., remove spaces and append ".com"
        // TODO: Implement a more robust domain derivation logic.
        return companyName.toLowerCase().replaceAll("\\s+", "") + ".com";
    }

    private boolean isValidHeader(String[] header) {
        return header.length >= 3 &&
                header[0].equalsIgnoreCase("first_name") &&
                header[1].equalsIgnoreCase("last_name") &&
                header[2].equalsIgnoreCase("company_name") &&
                header[3].equalsIgnoreCase("company_domain");
    }

    private LeadResponseDto generateLead(String firstName, String lastName, String companyName, String companyDomain) {
        Optional<String> email = generateEmail(EmailComponents.builder()
                .firstName(firstName)
                .lastName(lastName)
                .companyDomain(companyDomain)
                .build());

        return LeadResponseDto.builder()
                .firstName(firstName)
                .lastName(lastName)
                .companyName(companyName)
                .companyDomain(companyDomain)
                .generatedEmail(email.orElse(null))
                // TODO: Add logic to set status based on email verification
                .status(LeadStatus.EMAIL_CREATED.getDescription())
                .build();
    }

    public Optional<String> generateEmail(EmailComponents components) {
        for (Function<EmailComponents, String> pattern : EMAIL_PATTERNS) {
            String candidateEmail = pattern.apply(components);

            if (isValid(candidateEmail)) {
                return Optional.of(candidateEmail);
            }
        }
        return Optional.empty();
    }

    /**
     * Validates an email address by:
     *   1. Checking basic format.
     *   2. Looking up the domain's mail server (MX, with fallback to A record).
     *   3. Performing an SMTP handshake to see if the server accepts the email.
     *
     * @param email the email address to validate
     * @return true if valid; false otherwise
     */
    private boolean isValid(String email) {
        if (email == null || !email.contains("@")) {
            log.warn("Invalid email format: {}", email);
            return false;
        }

        // Extract domain from email.
        String domain = email.substring(email.indexOf("@") + 1);
        log.info("Verifying email {} for domain {}", email, domain);

        // Use DnsLookupService to get the mail server.
        Optional<String> recipientMailServerOpt = dnsLookupService.getMailServer(domain);

        if (recipientMailServerOpt.isEmpty()) {
            log.warn("No mail server found for domain: {}", domain);
            return false;
        }

        String recipientMailServer = recipientMailServerOpt.get();
        log.info("Found mail server: {} for domain: {}", recipientMailServer, domain);

        boolean smtpValid = smtpEmailVerifier.verifyEmail(recipientMailServer, email);

        if (smtpValid) {
            log.info("SMTP verification SUCCEEDED for {}\n", email.toUpperCase());
        } else {
            log.warn("SMTP verification FAILED for {}\n", email.toUpperCase());
        }
        return smtpValid;
    }

}
