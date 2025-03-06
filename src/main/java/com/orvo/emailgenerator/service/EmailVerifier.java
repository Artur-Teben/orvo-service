package com.orvo.emailgenerator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerifier {

    private final DnsLookupService dnsLookupService;

    public Optional<String> fetchMXServer(String domain) {
        return dnsLookupService.getMXServer(domain);
    }

    public static void main(String[] args) {
        DnsLookupService dnsLookupService = new DnsLookupService();
        EmailVerifier emailVerifier = new EmailVerifier(dnsLookupService);
        Optional<String> mxServer = emailVerifier.fetchMXServer("gmail.com");

        mxServer.ifPresent(System.out::println);
    }

}
