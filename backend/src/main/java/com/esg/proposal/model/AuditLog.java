package com.esg.proposal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    private String operatorId;    // 工號
    private String operatorName;
    private String action;        // CREATE_PROPOSAL | EDIT_PROPOSAL | DELETE_PROPOSAL ...
    private String targetId;      // 被操作的資料 id
    private String detail;        // 摘要 JSON string
    private String ip;
    private Instant timestamp = Instant.now();
}
