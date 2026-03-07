package com.esg.proposal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "工號不得為空")
    private String employeeId;

    @NotBlank(message = "密碼不得為空")
    private String password;
}
