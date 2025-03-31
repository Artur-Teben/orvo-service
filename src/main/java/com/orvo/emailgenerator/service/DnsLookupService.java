package com.orvo.emailgenerator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.AbstractMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * Service for performing DNS lookups to retrieve MX (Mail Exchange) records,
 * with a fallback to A record if no MX records are found.
 */
@Slf4j
@Service
public class DnsLookupService {

    private static final String DNS_CONTEXT_FACTORY = "com.sun.jndi.dns.DnsContextFactory";
    private static final String DNS_PREFIX = "dns:/";
    private static final String MX_RECORD_TYPE = "MX";

    /**
     * Retrieves the mail server (MX or, if none, A record) for the specified domain.
     *
     * @param domain the domain name to lookup the mail server for
     * @return an Optional containing the mail server host if found
     */
    public Optional<String> getMailServer(String domain) {
        if (domain == null || domain.isEmpty()) {
            log.warn("Domain must not be null or empty");
            return Optional.empty();
        }

        try {
            DirContext context = createDnsContext();

            // First, attempt to get MX records.
            Optional<String> mxServer = getMXRecord(context, domain);

            if (mxServer.isPresent()) {
                return mxServer;
            }
            // Fallback to A record if no MX records are found.
            log.warn("No MX records found for {}. Falling back to A record.", domain);
            return getARecord(context, domain);
        } catch (NamingException e) {
            log.error("Error during mail server lookup for {}: {}", domain, e.getMessage());
            return Optional.empty();
        }
    }

    private DirContext createDnsContext() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, DNS_CONTEXT_FACTORY);
        return new InitialDirContext(env);
    }

    private Optional<String> getMXRecord(DirContext context, String domain) throws NamingException {
        Attributes attributes = context.getAttributes(DNS_PREFIX + domain, new String[]{MX_RECORD_TYPE});
        Attribute servers = attributes.get(MX_RECORD_TYPE);

        if (servers == null) {
            return Optional.empty();
        }
        return parseMXRecords(servers);
    }

    private Optional<String> parseMXRecords(Attribute servers) throws NamingException {
        Queue<Map.Entry<Integer, String>> mxHeap = new PriorityQueue<>(Map.Entry.comparingByKey());
        NamingEnumeration<?> hostsWithPriorities = servers.getAll();

        while (hostsWithPriorities.hasMore()) {
            String record = hostsWithPriorities.next().toString().trim();
            String[] parts = record.split("\\s+", 2);

            if (parts.length < 2) {
                continue;
            }

            try {
                Integer priority = Integer.valueOf(parts[0].trim());
                String mxHost = parts[1].trim();
                mxHeap.add(new AbstractMap.SimpleEntry<>(priority, mxHost));
            } catch (NumberFormatException e) {
                log.warn("Skipping record with invalid priority: {}", record);
            }
        }
        return !mxHeap.isEmpty() ? Optional.of(mxHeap.poll().getValue()) : Optional.empty();
    }

    private Optional<String> getARecord(DirContext context, String domain) throws NamingException {
        Attributes attributes = context.getAttributes(DNS_PREFIX + domain, new String[]{"A"});
        Attribute aRecord = attributes.get("A");

        if (aRecord != null) {
            try {
                String ipAddress = aRecord.get(0).toString().trim();
                return Optional.of(ipAddress);
            } catch (NamingException e) {
                log.error("Error retrieving A record for {}: {}", domain, e.getMessage());
            }
        }
        return Optional.empty();
    }

}
