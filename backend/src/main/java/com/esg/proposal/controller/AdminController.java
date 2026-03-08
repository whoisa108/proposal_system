package com.esg.proposal.controller;

import com.esg.proposal.audit.Audited;
import com.esg.proposal.dto.ProposalRequest;
import com.esg.proposal.model.AuditLog;
import com.esg.proposal.model.Proposal;
import com.esg.proposal.model.User;
import com.esg.proposal.service.AdminService;
import com.esg.proposal.service.MinioService;
import com.esg.proposal.service.ProposalService;
import com.esg.proposal.service.SettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final ProposalService proposalService;
    private final MinioService minioService;
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

    // 下載單一提案檔案
    @GetMapping("/proposals/{id}/download")
    public ResponseEntity<InputStreamResource> downloadProposalFile(@PathVariable("id") String id) throws Exception {
        Proposal proposal = proposalService.getById(id);
        InputStream is = minioService.getObject(proposal.getFilePath());
        String encodedName = URLEncoder.encode(proposal.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(is));
    }

    // 打包下載所有提案檔案（ZIP）
    @GetMapping("/proposals/download-all")
    public ResponseEntity<InputStreamResource> downloadAllProposals() throws Exception {
        List<Proposal> proposals = proposalService.getAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Proposal p : proposals) {
                if (p.getFilePath() == null) continue;
                try (InputStream is = minioService.getObject(p.getFilePath())) {
                    zos.putNextEntry(new ZipEntry(p.getFileName()));
                    is.transferTo(zos);
                    zos.closeEntry();
                } catch (Exception e) {
                    // skip missing files
                }
            }
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"proposals.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(new InputStreamResource(bais));
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
