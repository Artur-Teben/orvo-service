package com.orvo.emailgenerator.service;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DnsLookupServiceTest {

    private final DnsLookupService dnsLookupService = new DnsLookupService();

    @Test
    public void testGetMXServerWithNullDomain() {
        Optional<String> result = dnsLookupService.getMXServer(null);
        assertFalse(result.isPresent(), "Expected empty result for null domain.");
    }

    @Test
    public void testGetMXServerWithEmptyDomain() {
        Optional<String> result = dnsLookupService.getMXServer("");
        assertFalse(result.isPresent(), "Expected empty result for empty domain.");
    }

    @Test
    public void testGetMXServerWithNonExistentDomain() {
        Optional<String> result = dnsLookupService.getMXServer("nonexistent.invaliddomain");
        assertFalse(result.isPresent(), "Expected empty result for nonexistent domain.");
    }

    @Test
    public void testGetMXServerWithValidDomain() {
        Optional<String> result = dnsLookupService.getMXServer("google.com");
        assertTrue(result.isPresent(), "Expected a valid MX record for google.com.");
    }

}