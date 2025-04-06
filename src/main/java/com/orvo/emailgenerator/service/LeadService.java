package com.orvo.emailgenerator.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.orvo.emailgenerator.model.*;
import com.orvo.emailgenerator.model.dto.response.LeadGenerationResponse;
import com.orvo.emailgenerator.model.dto.response.LeadResponseDto;
import com.orvo.emailgenerator.model.entity.BatchLead;
import com.orvo.emailgenerator.model.entity.Lead;
import com.orvo.emailgenerator.model.mapper.LeadMapper;
import com.orvo.emailgenerator.repository.BatchLeadRepository;
import com.orvo.emailgenerator.repository.LeadRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.orvo.emailgenerator.util.Constants.EMAIL_PATTERNS;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;
    private final BatchLeadRepository batchLeadRepository;
    private final DnsLookupService dnsLookupService;
    private final SmtpEmailVerifier smtpEmailVerifier;
    private final LeadMapper leadMapper;

    public LeadGenerationResponse generateLeads(MultipartFile file) {
        UUID batchId = UUID.randomUUID();
        log.info("Generating leads from file: {} with batchId: {}", file.getOriginalFilename(), batchId);

        List<CompletableFuture<BatchLead>> futures = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            CSVReader csvReader = new CSVReader(reader);
            List<String[]> rows = csvReader.readAll();

            if (rows.isEmpty() || !isValidHeader(rows.getFirst())) {
                throw new IllegalArgumentException("Invalid CSV headers.");
            }

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                futures.add(generateBatchLeadAsync(
                        row[0],
                        row[1],
                        row[2],
                        (row.length > 3 && !row[3].isEmpty()) ? row[3] : deriveDomainFromCompany(row[2]),
                        batchId));
            }

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Collect results
            List<BatchLead> batchLeads = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            int validEmails = (int) batchLeads.stream()
                    .filter(bl -> bl.getLead().getStatus().equals(LeadStatus.EMAIL_CREATED))
                    .count();


            log.info("Generated {} leads ({} valid)", batchLeads.size(), validEmails);

            return LeadGenerationResponse.builder()
                    .batchId(batchId)
                    .totalLeads(batchLeads.size())
                    .validEmails(validEmails)
                    .build();

        } catch (IOException e) {
            log.error("Lead generation failed due to IO error", e);
            throw new RuntimeException("Failed to read the file", e);
        } catch (CsvException e) {
            log.error("Lead generation failed due to CSV parsing error", e);
            throw new RuntimeException("Failed to parse the CSV file", e);
        }
    }

    /**
     * A stub for deriving the domain from a company name.
     * In a real implementation, this might query a public API or use a lookup table.
     */
    private String deriveDomainFromCompany(String companyName) {
        // For now, simply return a dummy domain or try a basic conversion.
        // E.g., remove spaces and append ".com"
        // TODO: Implement a more robust domain derivation logic.
        log.debug("Deriving domain from company name: {}", companyName);
        return companyName.toLowerCase().replaceAll("\\s+", "") + ".com";
    }

    private boolean isValidHeader(String[] header) {
        log.debug("Validating CSV header: {}", Arrays.toString(header));
        return header.length >= 4 &&
                header[0].equalsIgnoreCase("first_name") &&
                header[1].equalsIgnoreCase("last_name") &&
                header[2].equalsIgnoreCase("company_name") &&
                header[3].equalsIgnoreCase("company_domain");
    }

    @Async("leadVerificationExecutor")
    public CompletableFuture<BatchLead> generateBatchLeadAsync(String firstName, String lastName, String companyName, String companyDomain, UUID batchId) {
        return CompletableFuture.completedFuture(
                generateBatchLead(firstName, lastName, companyName, companyDomain, batchId)
        );
    }

    private BatchLead generateBatchLead(String firstName, String lastName, String companyName, String companyDomain, UUID batchId) {
        log.debug("Generating lead for: {} {} at {}", firstName, lastName, companyDomain);

        // Check existing record first
        Lead lead = leadRepository
                .findByFirstNameIgnoreCaseAndLastNameIgnoreCase(firstName, lastName)
                .orElseGet(() -> {
                    Optional<String> emailOpt = generateEmail(EmailComponents.builder()
                            .firstName(firstName)
                            .lastName(lastName)
                            .companyDomain(companyDomain)
                            .build());

                    LeadStatus status = emailOpt.isPresent() ? LeadStatus.EMAIL_CREATED : LeadStatus.EMAIL_FAILED;

                    return leadRepository.save(Lead.builder()
                            .firstName(firstName)
                            .lastName(lastName)
                            .companyName(companyName)
                            .companyDomain(companyDomain)
                            .generatedEmail(emailOpt.orElse(null))
                            .status(status)
                            .build());
                });


        return batchLeadRepository.save(BatchLead.builder()
                .batchId(batchId)
                .lead(lead)
                .build());
    }

    public Optional<String> generateEmail(EmailComponents components) {
        log.debug("Generating email for: {} {}", components.getFirstName(), components.getLastName());

        for (Function<EmailComponents, String> pattern : EMAIL_PATTERNS) {
            String candidateEmail = pattern.apply(components);

            if (isValid(candidateEmail)) {
                log.info("Generated valid email: {}", candidateEmail);
                return Optional.of(candidateEmail);
            }
        }
        log.warn("Failed to generate valid email for: {} {}", components.getFirstName(), components.getLastName());
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

    public List<LeadResponseDto> getLeads(String batchId) {
        log.info("Retrieving leads for batchId: {}", batchId);

        List<BatchLead> batchLeads = batchLeadRepository.findAllByBatchId(UUID.fromString(batchId));
        List<LeadResponseDto> leads =  batchLeads.stream()
                .map(bl -> leadMapper.toLeadResponseDto(bl.getLead()))
                .sorted(Comparator.comparing(LeadResponseDto::getGeneratedEmail, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        log.info("Retrieved {} leads for batchId: {}", leads.size(), batchId);

        return leads;
    }

    public void writeLeadsToCsv(String batchId, HttpServletResponse response) {
        List<BatchLead> batchLeads = batchLeadRepository.findAllByBatchId(UUID.fromString(batchId));
        List<LeadResponseDto> leads = batchLeads.stream()
                .map(bl -> leadMapper.toLeadResponseDto(bl.getLead()))
                .toList();

        try (PrintWriter writer = response.getWriter()) {
            writer.println("first_name,last_name,company_name,company_domain,generated_email,status");

            for (LeadResponseDto lead : leads) {
                writer.printf("%s,%s,%s,%s,%s,%s%n",
                        lead.getFirstName(),
                        lead.getLastName(),
                        lead.getCompanyName(),
                        lead.getCompanyDomain(),
                        Optional.ofNullable(lead.getGeneratedEmail()).orElse(""),
                        lead.getStatus()
                );
            }

            log.info("CSV file generated successfully for batchId: {}", batchId);
        } catch (IOException e) {
            log.error("Error generating CSV file for batchId: {}", batchId, e);
            throw new RuntimeException("Error writing CSV", e);
        }
    }

}
