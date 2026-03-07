package com.esg.proposal.config;

import com.esg.proposal.model.User;
import com.esg.proposal.repository.UserRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioClient minioClient;

    @Value("${admin.default-employee-id}")
    private String adminEmployeeId;

    @Value("${admin.default-password}")
    private String adminPassword;

    @Value("${admin.default-name}")
    private String adminName;

    @Value("${admin.default-department}")
    private String adminDepartment;

    @Value("${minio.bucket}")
    private String bucket;

    @Override
    public void run(String... args) throws Exception {
        createAdminIfNotExists();
        createMinioBucketIfNotExists();
    }

    private void createAdminIfNotExists() {
        if (!userRepository.existsByEmployeeId(adminEmployeeId)) {
            User admin = new User();
            admin.setEmployeeId(adminEmployeeId);
            admin.setName(adminName);
            admin.setDepartment(adminDepartment);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("Admin 帳號已建立，工號：{}", adminEmployeeId);
        }
    }

    private void createMinioBucketIfNotExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("MinIO bucket 已建立：{}", bucket);
        }
    }
}
