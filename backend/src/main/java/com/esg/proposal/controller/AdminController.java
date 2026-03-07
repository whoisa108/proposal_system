package com.esg.proposal.controller;

import com.esg.proposal.audit.Audited;
import com.esg.proposal.dto.ProposalRequest;
import com.esg.proposal.model.AuditLog;
import com.esg.proposal.model.Proposal;
import com.esg.proposal.model.User;
import com.esg.proposal.service.AdminService;
import com.esg.proposal.service.ProposalService;
import com.esg.proposal.service.SettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ProposalService proposalService;
    private final SettingService settingService;
    private final ObjectMapper objectMapper;

    // ---- 使用者管理 ----

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    @Audited(action = "DELETE_USER")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "使用者已刪除"));
    }

    // ---- 提案管理 ----

    @GetMapping("/proposals")
    public ResponseEntity<List<Proposal>> getAllProposals() {
        return ResponseEntity.ok(proposalService.getAll());
    }

    @PutMapping("/proposals/{id}")
    @Audited(action = "ADMIN_EDIT_PROPOSAL")
    public ResponseEntity<Proposal> updateProposal(
            @PathVariable String id,
            @RequestParam("data") String dataJson,
            @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {

        ProposalRequest req = objectMapper.readValue(dataJson, ProposalRequest.class);
        Proposal proposal = proposalService.update(id, req, file);
        return ResponseEntity.ok(proposal);
    }

    @DeleteMapping("/proposals/{id}")
    @Audited(action = "ADMIN_DELETE_PROPOSAL")
    public ResponseEntity<Map<String, String>> deleteProposal(@PathVariable String id) throws Exception {
        proposalService.delete(id);
        return ResponseEntity.ok(Map.of("message", "提案已刪除"));
    }

    // ---- 截止時間 ----

    @PutMapping("/deadline")
    @Audited(action = "SET_DEADLINE")
    public ResponseEntity<Map<String, String>> setDeadline(
            @RequestBody Map<String, String> body) {
        settingService.setDeadline(body.get("deadline"));
        return ResponseEntity.ok(Map.of("message", "截止時間已更新"));
    }

    // ---- Audit Log ----

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(adminService.getAuditLogs());
    }
}
