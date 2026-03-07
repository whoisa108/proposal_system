package com.esg.proposal.repository;

import com.esg.proposal.model.AuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findAllByOrderByTimestampDesc();
}
