package com.orvo.emailgenerator.util;

import com.orvo.emailgenerator.model.EmailComponents;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class Constants {

    public static final List<Function<EmailComponents, String>> EMAIL_PATTERNS = Arrays.asList(
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

}
