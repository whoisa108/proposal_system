package com.esg.proposal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ProposalRequest {

    @NotBlank(message = "點子類別不得為空")
    private String category;   // 酷炫點子獎 | 卓越影響獎

    @NotBlank(message = "點子方向不得為空")
    private String direction;

    @NotBlank(message = "點子名稱不得為空")
    @Size(max = 50, message = "點子名稱不得超過 50 字")
    private String title;

    @NotBlank(message = "點子摘要不得為空")
    @Size(max = 300, message = "點子摘要不得超過 300 字")
    private String summary;

    // 隊友 0-4 人，由前端以 JSON string 傳入，後端解析
    private List<TeammateDto> teammates;

    @Data
    public static class TeammateDto {
        private String name;
        private String employeeId;
    }
}
