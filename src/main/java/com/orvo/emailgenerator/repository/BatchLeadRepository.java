package com.orvo.emailgenerator.repository;

import com.orvo.emailgenerator.model.entity.BatchLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BatchLeadRepository extends JpaRepository<BatchLead, Long> {

    List<BatchLead> findAllByBatchId(UUID batchId);

}