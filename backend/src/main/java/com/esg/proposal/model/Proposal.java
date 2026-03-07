package com.esg.proposal.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "proposals")
@CompoundIndex(name = "unique_proposer_title", def = "{'employeeId': 1, 'title': 1}", unique = true)
public class Proposal {

    @Id
    private String id;

    // 提案人資訊
    private String proposerId;      // users._id
    private String employeeId;      // 冗餘存，方便查詢
    private String proposerName;
    private String department;

    // 提案內容
    private String category;        // 酷炫點子獎 | 卓越影響獎
    private String direction;       // 五大方向
    private String title;           // ≤ 50 字
    private String summary;         // ≤ 300 字

    // 檔案
    private String fileName;        // 顯示用，例如 I_AAID_王大陸_14554_綠能水.pdf
    private String filePath;        // MinIO object key

    // 隊友 (0-4人)
    private List<Teammate> teammates;

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
