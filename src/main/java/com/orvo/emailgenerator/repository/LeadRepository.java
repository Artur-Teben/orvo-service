package com.orvo.emailgenerator.repository;

import com.orvo.emailgenerator.model.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, Long> {
}