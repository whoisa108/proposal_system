package com.esg.proposal.controller;

import com.esg.proposal.dto.ProposalRequest;
import com.esg.proposal.model.Proposal;
import com.esg.proposal.service.MinioService;
import com.esg.proposal.service.ProposalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalControllerTest {

    @Mock
    private ProposalService proposalService;

    @Mock
    private MinioService minioService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private ProposalController proposalController;

    private Proposal proposal;

    @BeforeEach
    void setUp() {
        proposal = new Proposal();
        proposal.setId("prop-1");
        proposal.setEmployeeId("EMP001");
        proposal.setTitle("綠能水");
        proposal.setFileName("I_AAID_王大陸_EMP001_綠能水.pdf");
        proposal.setFilePath("EMP001/I_AAID_王大陸_EMP001_綠能水.pdf");
    }

    @Test
    void getMyProposals_returns200WithList() {
        when(proposalService.getMyProposals("EMP001")).thenReturn(List.of(proposal));

        ResponseEntity<List<Proposal>> response = proposalController.getMyProposals("EMP001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void create_success_returns200() throws Exception {
        ProposalRequest req = new ProposalRequest();
        req.setTitle("綠能水");
        when(objectMapper.readValue(anyString(), eq(ProposalRequest.class))).thenReturn(req);
        when(proposalService.create("EMP001", req, file)).thenReturn(proposal);

        ResponseEntity<Proposal> response = proposalController.create("EMP001", "{}", file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getId()).isEqualTo("prop-1");
    }

    @Test
    void update_success_returns200() throws Exception {
        ProposalRequest req = new ProposalRequest();
        when(objectMapper.readValue(anyString(), eq(ProposalRequest.class))).thenReturn(req);
        when(proposalService.update("prop-1", req, null)).thenReturn(proposal);

        ResponseEntity<Proposal> response = proposalController.update("prop-1", "{}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void delete_success_returns200WithMessage() throws Exception {
        doNothing().when(proposalService).delete("prop-1");

        ResponseEntity<Map<String, String>> response = proposalController.delete("prop-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("message", "刪除成功");
    }

    @Test
    void downloadFile_success_returns200WithContentDisposition() throws Exception {
        when(proposalService.getById("prop-1")).thenReturn(proposal);
        InputStream is = new ByteArrayInputStream("pdf content".getBytes());
        when(minioService.getObject("EMP001/I_AAID_王大陸_EMP001_綠能水.pdf")).thenReturn(is);

        ResponseEntity<?> response = proposalController.downloadFile("prop-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("Content-Disposition"))
                .contains("attachment");
    }
}
