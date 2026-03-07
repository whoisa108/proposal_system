package com.esg.proposal.repository;

import com.esg.proposal.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);
}
