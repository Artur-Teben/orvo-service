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
import java.util.*;

/**
 * Service for performing DNS lookups to retrieve MX (Mail Exchange) records.
 *
 * <p>This class encapsulates the DNS lookup logic using JNDI. It provides methods to retrieve
 * the MX host with the highest priority for a given domain.</p>
 */
@Slf4j
@Service
public class DnsLookupService {

    /**
     * Retrieves the MX server for the specified domain.
     *
     * <p>This method performs a DNS lookup for MX records of the given domain. It validates the input,
     * performs the DNS query, sorts the found records by priority, and returns the MX host with the highest priority.</p>
     *
     * @param domain the domain name to lookup the MX record for; must not be {@code null} or empty
     * @return an {@code Optional} containing the MX host if successful; otherwise, an empty Optional
     */
    public Optional<String> getMXServer(String domain) {
        if (domain == null || domain.isEmpty()) {
            log.warn("Domain must not be null or empty");
            return Optional.empty();
        }

        // Setup JNDI DNS lookup
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        DirContext context = null;

        try {
            context = new InitialDirContext(env);

            // Retrieve MX records
            Attributes attributes = context.getAttributes("dns:/" + domain, new String[]{"MX"});
            Attribute servers = attributes.get("MX");

            if (servers == null) {
                return Optional.empty();
            }

            Queue<Map.Entry<Integer, String>> mxHeap = new PriorityQueue<>(Map.Entry.comparingByKey());

            // Parse and store MX records
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
        } catch (NamingException e) {
            log.error("MX server for {} wasn't found. En error occurred: {}", domain, e.getMessage());
            return Optional.empty();
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    log.warn("Failed to close DirContext", e);
                }
            }
        }
    }
}
