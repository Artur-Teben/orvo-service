package com.orvo.emailgenerator.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.orvo.emailgenerator.model.EmailComponents;
import com.orvo.emailgenerator.model.LeadResponseDto;
import com.orvo.emailgenerator.model.MailboxlayerResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Service
public class LeadService {

    private static final String MAILBOXLAYER_API_KEY = "7229e4484a4b08f32bd196fcf4674dcf";
    private static final String MAILBOXLAYER_API_URL_TEMPLATE = "https://apilayer.net/api/check?access_key=%s&email=%s";


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


    public List<LeadResponseDto> generateLeads(MultipartFile file) {
        List<LeadResponseDto> leads = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream())) {
            CSVReader csvReader = new CSVReader(reader);
            List<String[]> rows = csvReader.readAll();

            // Validate headers
            if (rows.isEmpty() || !isValidHeader(rows.getFirst())) {
                throw new IllegalArgumentException("Invalid CSV headers. Expected: first_name, last_name, company_name, [company_domain]");
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
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }

        return leads;
    }

    /**
     * A stub for deriving the domain from a company name.
     * In a real implementation, this might query a public API or use a lookup table.
     */
    private String deriveDomainFromCompany(String companyName) {
        // For now, simply return a dummy domain or try a basic conversion.
        // E.g., remove spaces and append ".com"
        return companyName.toLowerCase().replaceAll("\\s+", "") + ".com";
    }

    private boolean isValidHeader(String[] header) {
        // Simple check for expected columns
        return header.length >= 3 &&
                header[0].equalsIgnoreCase("first_name") &&
                header[1].equalsIgnoreCase("last_name") &&
                header[2].equalsIgnoreCase("company_name");
    }

    private LeadResponseDto generateLead(String firstName, String lastName, String companyName, String companyDomain) {
        // In a real-world scenario, you might call an external verification API here.
        // For now, simply default to a basic pattern.
        Optional<String> email = generateEmail(EmailComponents.builder()
                .firstName(firstName)
                .lastName(lastName)
                .companyDomain(companyDomain)
                .build());

        // Here, you could add logic to verify the email and set an appropriate confidence score/status.
        return LeadResponseDto.builder()
                .firstName(firstName)
                .lastName(lastName)
                .companyName(companyName)
                .companyDomain(companyDomain)
                .generatedEmail(email.toString())
                .build();
    }

    public Optional<String> generateEmail(EmailComponents components) {
        for (Function<EmailComponents, String> pattern : EMAIL_PATTERNS) {
            String candidateEmail = pattern.apply(components);
            if (isEmailVerified(candidateEmail)) {
                return Optional.of(candidateEmail);
            }
        }
        return Optional.empty();
    }

    private boolean isEmailVerified(String email) {
        String apiUrl = String.format(MAILBOXLAYER_API_URL_TEMPLATE, MAILBOXLAYER_API_KEY, email);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<MailboxlayerResponse> response =
                    restTemplate.getForEntity(apiUrl, MailboxlayerResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                MailboxlayerResponse result = response.getBody();

                // Determine verification based on criteria. For instance:
                // - Email format is valid
                // - MX records exist
                // - SMTP check is true
                // - And optionally, a high score (e.g., score >= 0.7)
                if (result.isFormat_valid() && result.isMx_found() && result.isSmtp_check() && result.getScore() >= 0.7f) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
