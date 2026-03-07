package com.esg.proposal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "工號不得為空")
    private String employeeId;

    @NotBlank(message = "姓名不得為空")
    private String name;

    @NotBlank(message = "部門不得為空")
    @Pattern(regexp = "AAID|BSID|ICSD|TSID|PLED|PEID", message = "部門代號不正確")
    private String department;

    @NotBlank(message = "密碼不得為空")
    @Size(min = 8, message = "密碼至少 8 個字元")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&]).{8,}$",
        message = "密碼需包含英文字母、數字及特殊符號"
    )
    private String password;
}
