package com.esg.proposal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "settings")
public class Setting {

    @Id
    private String id;

    private String key;    // e.g. "DEADLINE"
    private String value;  // ISO 8601 string
}
