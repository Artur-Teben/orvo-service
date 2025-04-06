package com.orvo.emailgenerator.repository;

import com.orvo.emailgenerator.model.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {

    @Query("SELECT l FROM Lead l WHERE LOWER(l.firstName) = LOWER(:firstName) AND LOWER(l.lastName) = LOWER(:lastName)")
    Optional<Lead> findByFirstNameIgnoreCaseAndLastNameIgnoreCase(@Param("firstName") String firstName, @Param("lastName") String lastName);

    Optional<Lead> findByGeneratedEmail(String email);

}