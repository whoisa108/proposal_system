package com.esg.proposal.controller;

import com.esg.proposal.dto.ProposalRequest;
import com.esg.proposal.model.AuditLog;
import com.esg.proposal.model.Proposal;
import com.esg.proposal.model.User;
import com.esg.proposal.service.AdminService;
import com.esg.proposal.service.MinioService;
import com.esg.proposal.service.ProposalService;
import com.esg.proposal.service.SettingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private ProposalService proposalService;

    @Mock
    private MinioService minioService;

    @Mock
    private SettingService settingService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminController adminController;

    private Proposal proposal;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-1");
        user.setEmployeeId("EMP001");

        proposal = new Proposal();
        proposal.setId("prop-1");
        proposal.setFileName("I_AAID_王大陸_EMP001_綠能水.pdf");
        proposal.setFilePath("EMP001/I_AAID_王大陸_EMP001_綠能水.pdf");
    }

    // --- Users ---

    @Test
    void getAllUsers_returns200() {
        when(adminService.getAllUsers()).thenReturn(List.of(user));

        ResponseEntity<List<User>> response = adminController.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void deleteUser_returns200WithMessage() {
        doNothing().when(adminService).deleteUser("user-1");

        ResponseEntity<Map<String, String>> response = adminController.deleteUser("user-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "使用者已刪除");
    }

    // --- Proposals ---

    @Test
    void getAllProposals_returns200() {
        when(proposalService.getAll()).thenReturn(List.of(proposal));

        ResponseEntity<List<Proposal>> response = adminController.getAllProposals();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void updateProposal_returns200() throws Exception {
        ProposalRequest req = new ProposalRequest();
        when(objectMapper.readValue(anyString(), eq(ProposalRequest.class))).thenReturn(req);
        when(proposalService.update("prop-1", req, null)).thenReturn(proposal);

        ResponseEntity<Proposal> response = adminController.updateProposal("prop-1", "{}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteProposal_returns200WithMessage() throws Exception {
        doNothing().when(proposalService).delete("prop-1");

        ResponseEntity<Map<String, String>> response = adminController.deleteProposal("prop-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "提案已刪除");
    }

    @Test
    void downloadProposalFile_returns200WithContentDisposition() throws Exception {
        when(proposalService.getById("prop-1")).thenReturn(proposal);
        InputStream is = new ByteArrayInputStream("pdf".getBytes());
        when(minioService.getObject(proposal.getFilePath())).thenReturn(is);

        ResponseEntity<InputStreamResource> response = adminController.downloadProposalFile("prop-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition")).contains("attachment");
    }

    @Test
    void downloadAllProposals_withProposals_returns200Zip() throws Exception {
        Proposal p1 = new Proposal();
        p1.setFilePath("EMP001/file1.pdf");
        p1.setFileName("file1.pdf");
        Proposal p2 = new Proposal();  // no filePath - should be skipped

        when(proposalService.getAll()).thenReturn(List.of(p1, p2));
        InputStream is = new ByteArrayInputStream("pdf content".getBytes());
        when(minioService.getObject("EMP001/file1.pdf")).thenReturn(is);

        ResponseEntity<InputStreamResource> response = adminController.downloadAllProposals();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .contains("proposals.zip");
    }

    @Test
    void downloadAllProposals_minioThrows_skipsFile() throws Exception {
        Proposal p1 = new Proposal();
        p1.setFilePath("EMP001/file1.pdf");
        p1.setFileName("file1.pdf");

        when(proposalService.getAll()).thenReturn(List.of(p1));
        when(minioService.getObject("EMP001/file1.pdf")).thenThrow(new RuntimeException("minio error"));

        ResponseEntity<InputStreamResource> response = adminController.downloadAllProposals();

        // Should still return 200 even when file fetch fails
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // --- Deadline ---

    @Test
    void setDeadline_returns200WithMessage() {
        doNothing().when(settingService).setDeadline("2030-01-01T00:00:00Z");

        ResponseEntity<Map<String, String>> response = adminController.setDeadline(
                Map.of("deadline", "2030-01-01T00:00:00Z"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "截止時間已更新");
        verify(settingService).setDeadline("2030-01-01T00:00:00Z");
    }

    // --- Audit Logs ---

    @Test
    void getAuditLogs_returns200() {
        when(adminService.getAuditLogs()).thenReturn(List.of(new AuditLog()));

        ResponseEntity<List<AuditLog>> response = adminController.getAuditLogs();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }
}
