package com.orvo.emailgenerator.service;

import com.orvo.emailgenerator.config.property.EmailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.sun.mail.smtp.SMTPTransport;
import jakarta.mail.Session;
import jakarta.mail.MessagingException;

import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailVerifier {

    private final EmailProperties emailProperties;

    /**
     * Verifies an email address by using Jakarta Mail to perform an SMTP handshake.
     * It constructs a minimal email message and attempts to send it.
     * If the recipient is rejected during the handshake, a SendFailedException is thrown.
     *
     * @param smtpHost the SMTP server hostname (e.g. "mx-in.g.apple.com")
     * @param toEmail the email address to verify
     * @return true if the SMTP handshake accepts the recipient, false otherwise
     */
    public boolean verifyEmail(String smtpHost, String toEmail) {
        Properties props = getProperties(smtpHost);
        Session session = Session.getInstance(props);

        SMTPTransport transport = null;
        try {
            transport = (SMTPTransport) session.getTransport("smtp");
            transport.connect();

            // MAIL FROM
            String mailFrom = "<" + emailProperties.getUsername() + ">";
            transport.issueCommand("MAIL FROM:" + mailFrom, 250);
            int mailFromCode = transport.getLastReturnCode();
            log.info("MAIL FROM response: {}", mailFromCode);

            if (mailFromCode != 250) {
                log.warn("MAIL FROM not accepted: {}", mailFromCode);
                return false;
            }

            // RCPT TO
            String rcptTo = "<" + toEmail.trim() + ">";
            transport.issueCommand("RCPT TO:" + rcptTo, 250);
            int rcptToCode = transport.getLastReturnCode();
            log.info("RCPT TO response: {}", rcptToCode);

            return rcptToCode == 250 || rcptToCode == 251;

        } catch (MessagingException e) {
            log.error("SMTP handshake error for {}: {}", toEmail, e.getMessage());
            return false;
        } finally {
            if (transport != null && transport.isConnected()) {
                try {
                    transport.issueCommand("QUIT", 221);
                    transport.close();
                } catch (Exception e) {
                    log.warn("Error closing SMTP connection: {}", e.getMessage());
                }
            }
        }
    }

    private Properties getProperties(String smtpHost) {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", emailProperties.getPort());
        props.put("mail.smtp.connectiontimeout", emailProperties.getSmtp().getConnectiontimeout());
        props.put("mail.smtp.timeout", emailProperties.getSmtp().getTimeout());
        props.put("mail.smtp.writetimeout", emailProperties.getSmtp().getWritetimeout());
        return props;
    }

}
