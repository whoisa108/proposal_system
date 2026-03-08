package com.esg.proposal.controller;

import com.esg.proposal.audit.Audited;
import com.esg.proposal.dto.ProposalRequest;
import com.esg.proposal.model.Proposal;
import com.esg.proposal.service.MinioService;
import com.esg.proposal.service.ProposalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;
    private final MinioService minioService;
    private final ObjectMapper objectMapper;

    // 查看自己的提案
    @GetMapping("/my")
    public ResponseEntity<List<Proposal>> getMyProposals(
            @AuthenticationPrincipal String employeeId) {
        return ResponseEntity.ok(proposalService.getMyProposals(employeeId));
    }

    // 新增提案（multipart/form-data）
    @PostMapping
    @Audited(action = "CREATE_PROPOSAL")
    public ResponseEntity<Proposal> create(
            @AuthenticationPrincipal String employeeId,
            @RequestParam("data") String dataJson,   // ProposalRequest 以 JSON string 傳入
            @RequestParam("file") MultipartFile file) throws Exception {

        ProposalRequest req = objectMapper.readValue(dataJson, ProposalRequest.class);
        Proposal proposal = proposalService.create(employeeId, req, file);
        return ResponseEntity.ok(proposal);
    }

    // 編輯自己的提案
    @PutMapping("/{id}")
    @Audited(action = "EDIT_PROPOSAL")
    public ResponseEntity<Proposal> update(
            @PathVariable String id,
            @RequestParam("data") String dataJson,
            @RequestParam(value = "file", required = false) MultipartFile file) throws Exception {

        ProposalRequest req = objectMapper.readValue(dataJson, ProposalRequest.class);
        Proposal proposal = proposalService.update(id, req, file);
        return ResponseEntity.ok(proposal);
    }

    // 刪除自己的提案
    @DeleteMapping("/{id}")
    @Audited(action = "DELETE_PROPOSAL")
    public ResponseEntity<Map<String, String>> delete(@PathVariable String id) throws Exception {
        proposalService.delete(id);
        return ResponseEntity.ok(Map.of("message", "刪除成功"));
    }

    // 下載提案檔案
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("id") String id) throws Exception {
        Proposal proposal = proposalService.getById(id);
        InputStream is = minioService.getObject(proposal.getFilePath());
        String encodedName = URLEncoder.encode(proposal.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(is));
    }
}
