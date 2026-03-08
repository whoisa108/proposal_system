package com.esg.proposal.service;

import com.esg.proposal.dto.ProposalRequest;
import com.esg.proposal.model.Proposal;
import com.esg.proposal.model.User;
import com.esg.proposal.repository.ProposalRepository;
import com.esg.proposal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private SettingService settingService;

    @Mock
    private MultipartFile file;

    @InjectMocks
    private ProposalService proposalService;

    private User user;
    private ProposalRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-id-1");
        user.setEmployeeId("EMP001");
        user.setName("王大陸");
        user.setDepartment("AAID");

        request = new ProposalRequest();
        request.setCategory("酷炫點子獎");
        request.setDirection("節能減碳");
        request.setTitle("綠能水");
        request.setSummary("This is a summary.");
        request.setTeammates(List.of());
    }

    // --- getMyProposals ---

    @Test
    void getMyProposals_returnsProposalsForEmployee() {
        Proposal p = new Proposal();
        p.setEmployeeId("EMP001");
        when(proposalRepository.findByEmployeeId("EMP001")).thenReturn(List.of(p));

        List<Proposal> result = proposalService.getMyProposals("EMP001");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeId()).isEqualTo("EMP001");
    }

    // --- create ---

    @Test
    void create_success() throws Exception {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        when(proposalRepository.existsByEmployeeIdAndTitle("EMP001", "綠能水")).thenReturn(false);
        when(userRepository.findByEmployeeId("EMP001")).thenReturn(Optional.of(user));
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("proposal.pdf");
        when(file.getSize()).thenReturn(1024L);
        Proposal saved = new Proposal();
        saved.setId("prop-1");
        when(proposalRepository.save(any())).thenReturn(saved);

        Proposal result = proposalService.create("EMP001", request, file);

        assertThat(result.getId()).isEqualTo("prop-1");
        verify(minioService).upload(eq(file), anyString());
    }

    @Test
    void create_deadlinePassed_throwsException() {
        when(settingService.isDeadlinePassed()).thenReturn(true);

        assertThatThrownBy(() -> proposalService.create("EMP001", request, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("提案已截止，無法新增");

        verify(proposalRepository, never()).save(any());
    }

    @Test
    void create_duplicateTitle_throwsException() {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        when(proposalRepository.existsByEmployeeIdAndTitle("EMP001", "綠能水")).thenReturn(true);

        assertThatThrownBy(() -> proposalService.create("EMP001", request, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("您已有相同名稱的提案");
    }

    @Test
    void create_tooManyTeammates_throwsException() {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        when(proposalRepository.existsByEmployeeIdAndTitle(anyString(), anyString())).thenReturn(false);

        List<ProposalRequest.TeammateDto> teammates = List.of(
                new ProposalRequest.TeammateDto(),
                new ProposalRequest.TeammateDto(),
                new ProposalRequest.TeammateDto(),
                new ProposalRequest.TeammateDto(),
                new ProposalRequest.TeammateDto()
        );
        request.setTeammates(teammates);

        assertThatThrownBy(() -> proposalService.create("EMP001", request, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("隊友最多 4 人");
    }

    @Test
    void create_invalidFileType_throwsException() {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        when(proposalRepository.existsByEmployeeIdAndTitle("EMP001", "綠能水")).thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("proposal.docx");

        assertThatThrownBy(() -> proposalService.create("EMP001", request, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("檔案格式只接受 PDF、PPT、PPTX");
    }

    @Test
    void create_fileTooLarge_throwsException() {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        when(proposalRepository.existsByEmployeeIdAndTitle("EMP001", "綠能水")).thenReturn(false);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("proposal.pdf");
        when(file.getSize()).thenReturn(6L * 1024 * 1024);

        assertThatThrownBy(() -> proposalService.create("EMP001", request, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("檔案大小不得超過 5MB");
    }

    @Test
    void create_fileIsNull_throwsException() {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        when(proposalRepository.existsByEmployeeIdAndTitle("EMP001", "綠能水")).thenReturn(false);

        assertThatThrownBy(() -> proposalService.create("EMP001", request, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("請上傳提案檔案");
    }

    // --- update ---

    @Test
    void update_withoutNewFile_success() throws Exception {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        Proposal existing = new Proposal();
        existing.setId("prop-1");
        existing.setEmployeeId("EMP001");
        existing.setDepartment("AAID");
        existing.setProposerName("王大陸");
        existing.setFilePath("EMP001/old_file.pdf");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(existing));
        when(proposalRepository.save(any())).thenReturn(existing);

        Proposal result = proposalService.update("prop-1", request, null);

        assertThat(result).isNotNull();
        verify(minioService, never()).delete(any());
        verify(minioService, never()).upload(any(), any());
    }

    @Test
    void update_withNewFile_replacesOldFile() throws Exception {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        Proposal existing = new Proposal();
        existing.setId("prop-1");
        existing.setEmployeeId("EMP001");
        existing.setDepartment("AAID");
        existing.setProposerName("王大陸");
        existing.setFilePath("EMP001/old_file.pdf");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(existing));
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("new_proposal.pptx");
        when(file.getSize()).thenReturn(1024L);
        when(proposalRepository.save(any())).thenReturn(existing);

        proposalService.update("prop-1", request, file);

        verify(minioService).delete("EMP001/old_file.pdf");
        verify(minioService).upload(eq(file), anyString());
    }

    @Test
    void update_deadlinePassed_throwsException() {
        when(settingService.isDeadlinePassed()).thenReturn(true);

        assertThatThrownBy(() -> proposalService.update("prop-1", request, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("提案已截止，無法編輯");
    }

    @Test
    void update_proposalNotFound_throwsException() {
        when(settingService.isDeadlinePassed()).thenReturn(false);
        when(proposalRepository.findById("not-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.update("not-exist", request, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("找不到提案");
    }

    // --- delete ---

    @Test
    void delete_success() throws Exception {
        Proposal existing = new Proposal();
        existing.setId("prop-1");
        existing.setFilePath("EMP001/file.pdf");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(existing));

        proposalService.delete("prop-1");

        verify(minioService).delete("EMP001/file.pdf");
        verify(proposalRepository).deleteById("prop-1");
    }

    @Test
    void delete_proposalNotFound_throwsException() {
        when(proposalRepository.findById("not-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.delete("not-exist"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("找不到提案");

        verify(proposalRepository, never()).deleteById(any());
    }

    // --- getById ---

    @Test
    void getById_found_returnsProposal() {
        Proposal p = new Proposal();
        p.setId("prop-1");
        when(proposalRepository.findById("prop-1")).thenReturn(Optional.of(p));

        Proposal result = proposalService.getById("prop-1");

        assertThat(result.getId()).isEqualTo("prop-1");
    }

    @Test
    void getById_notFound_throwsException() {
        when(proposalRepository.findById("not-exist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proposalService.getById("not-exist"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("找不到提案");
    }

    // --- getAll ---

    @Test
    void getAll_returnsAllProposals() {
        when(proposalRepository.findAll()).thenReturn(List.of(new Proposal(), new Proposal()));

        List<Proposal> result = proposalService.getAll();

        assertThat(result).hasSize(2);
    }
}
