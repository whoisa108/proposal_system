package com.esg.proposal.service;

import com.esg.proposal.dto.ProposalRequest;
import com.esg.proposal.model.Proposal;
import com.esg.proposal.model.Teammate;
import com.esg.proposal.model.User;
import com.esg.proposal.repository.ProposalRepository;
import com.esg.proposal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final SettingService settingService;

    // 取得目前登入者的所有提案
    public List<Proposal> getMyProposals(String employeeId) {
        return proposalRepository.findByEmployeeId(employeeId);
    }

    // 新增提案
    public Proposal create(String employeeId, ProposalRequest req, MultipartFile file) throws Exception {
        if (settingService.isDeadlinePassed()) {
            throw new RuntimeException("提案已截止，無法新增");
        }

        if (proposalRepository.existsByEmployeeIdAndTitle(employeeId, req.getTitle())) {
            throw new RuntimeException("您已有相同名稱的提案");
        }

        validateTeammates(req.getTeammates());
        validateFile(file);

        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("找不到使用者"));

        // 組合檔名
        String prefix = "酷炫點子獎".equals(req.getCategory()) ? "I" : "O";
        String ext = getExtension(file.getOriginalFilename());
        String fileName = prefix + "_" + user.getDepartment() + "_" + user.getName()
                + "_" + employeeId + "_" + req.getTitle() + ext;
        String objectKey = employeeId + "/" + fileName;

        minioService.upload(file, objectKey);

        Proposal proposal = new Proposal();
        proposal.setProposerId(user.getId());
        proposal.setEmployeeId(employeeId);
        proposal.setProposerName(user.getName());
        proposal.setDepartment(user.getDepartment());
        proposal.setCategory(req.getCategory());
        proposal.setDirection(req.getDirection());
        proposal.setTitle(req.getTitle());
        proposal.setSummary(req.getSummary());
        proposal.setFileName(fileName);
        proposal.setFilePath(objectKey);
        proposal.setTeammates(toTeammateList(req.getTeammates()));

        return proposalRepository.save(proposal);
    }

    // 編輯提案（USER 本人 或 ADMIN 皆呼叫此方法）
    public Proposal update(String proposalId, ProposalRequest req, MultipartFile file) throws Exception {
        if (settingService.isDeadlinePassed()) {
            throw new RuntimeException("提案已截止，無法編輯");
        }

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("找不到提案"));

        validateTeammates(req.getTeammates());

        // 若有上傳新檔案，刪掉舊的再上傳新的
        if (file != null && !file.isEmpty()) {
            validateFile(file);

            // 刪舊檔
            if (proposal.getFilePath() != null) {
                minioService.delete(proposal.getFilePath());
            }

            String prefix = "酷炫點子獎".equals(req.getCategory()) ? "I" : "O";
            String ext = getExtension(file.getOriginalFilename());
            String fileName = prefix + "_" + proposal.getDepartment() + "_" + proposal.getProposerName()
                    + "_" + proposal.getEmployeeId() + "_" + req.getTitle() + ext;
            String objectKey = proposal.getEmployeeId() + "/" + fileName;

            minioService.upload(file, objectKey);
            proposal.setFileName(fileName);
            proposal.setFilePath(objectKey);
        }

        proposal.setCategory(req.getCategory());
        proposal.setDirection(req.getDirection());
        proposal.setTitle(req.getTitle());
        proposal.setSummary(req.getSummary());
        proposal.setTeammates(toTeammateList(req.getTeammates()));
        proposal.setUpdatedAt(Instant.now());

        return proposalRepository.save(proposal);
    }

    // 刪除提案
    public void delete(String proposalId) throws Exception {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("找不到提案"));

        if (proposal.getFilePath() != null) {
            minioService.delete(proposal.getFilePath());
        }
        proposalRepository.deleteById(proposalId);
    }

    // 取得單一提案（by id）
    public Proposal getById(String proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("找不到提案"));
    }

    // 查看所有提案（Admin 用）
    public List<Proposal> getAll() {
        return proposalRepository.findAll();
    }

    // ---- 私有工具方法 ----

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("請上傳提案檔案");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".pdf") && !name.endsWith(".ppt") && !name.endsWith(".pptx")) {
            throw new RuntimeException("檔案格式只接受 PDF、PPT、PPTX");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("檔案大小不得超過 5MB");
        }
    }

    private void validateTeammates(List<ProposalRequest.TeammateDto> teammates) {
        if (teammates != null && teammates.size() > 4) {
            throw new RuntimeException("隊友最多 4 人");
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private List<Teammate> toTeammateList(List<ProposalRequest.TeammateDto> dtos) {
        if (dtos == null) return List.of();
        return dtos.stream().map(dto -> {
            Teammate t = new Teammate();
            t.setName(dto.getName());
            t.setEmployeeId(dto.getEmployeeId());
            return t;
        }).collect(Collectors.toList());
    }
}
