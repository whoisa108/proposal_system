package com.esg.proposal.repository;

import com.esg.proposal.model.Proposal;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProposalRepository extends MongoRepository<Proposal, String> {

    List<Proposal> findByEmployeeId(String employeeId);

    boolean existsByEmployeeIdAndTitle(String employeeId, String title);
}
