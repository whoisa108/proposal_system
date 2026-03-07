package com.esg.proposal.repository;

import com.esg.proposal.model.Setting;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SettingRepository extends MongoRepository<Setting, String> {

    Optional<Setting> findByKey(String key);
}
