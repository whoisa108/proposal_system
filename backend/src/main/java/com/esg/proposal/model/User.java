package com.esg.proposal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String employeeId;

    private String name;
    private String department;   // AAID | BSID | ICSD | TSID | PLED | PEID
    private String password;     // bcrypt hash
    private String role;         // USER | ADMIN
    private Instant createdAt = Instant.now();
}
