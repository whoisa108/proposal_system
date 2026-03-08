package com.esg.proposal.config;

import com.esg.proposal.repository.SettingRepository;
import com.esg.proposal.repository.UserRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SettingRepository settingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dataInitializer, "adminEmployeeId", "ADMIN001");
        ReflectionTestUtils.setField(dataInitializer, "adminPassword", "admin-pass");
        ReflectionTestUtils.setField(dataInitializer, "adminName", "Admin");
        ReflectionTestUtils.setField(dataInitializer, "adminDepartment", "AAID");
        ReflectionTestUtils.setField(dataInitializer, "userEmployeeId", "USER001");
        ReflectionTestUtils.setField(dataInitializer, "userPassword", "user-pass");
        ReflectionTestUtils.setField(dataInitializer, "userName", "DefaultUser");
        ReflectionTestUtils.setField(dataInitializer, "userDepartment", "BSID");
        ReflectionTestUtils.setField(dataInitializer, "deadlineEndDate", "2030-12-31T23:59:59Z");
        ReflectionTestUtils.setField(dataInitializer, "bucket", "proposals");
    }

    @Test
    void run_allEntitiesNotExist_createsAll() throws Exception {
        when(userRepository.existsByEmployeeId("ADMIN001")).thenReturn(false);
        when(userRepository.existsByEmployeeId("USER001")).thenReturn(false);
        when(settingRepository.existsByKey("DEADLINE")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        dataInitializer.run();

        verify(userRepository, times(2)).save(any());
        verify(settingRepository).save(any());
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void run_allEntitiesAlreadyExist_skipsCreation() throws Exception {
        when(userRepository.existsByEmployeeId("ADMIN001")).thenReturn(true);
        when(userRepository.existsByEmployeeId("USER001")).thenReturn(true);
        when(settingRepository.existsByKey("DEADLINE")).thenReturn(true);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, never()).save(any());
        verify(settingRepository, never()).save(any());
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void run_adminExistsButUserDoesNot_createsUserOnly() throws Exception {
        when(userRepository.existsByEmployeeId("ADMIN001")).thenReturn(true);
        when(userRepository.existsByEmployeeId("USER001")).thenReturn(false);
        when(settingRepository.existsByKey("DEADLINE")).thenReturn(true);
        when(passwordEncoder.encode("user-pass")).thenReturn("encoded-user");
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, times(1)).save(argThat(u -> u.getRole().equals("USER")));
    }

    @Test
    void run_deadlineNotSet_createsDeadlineSetting() throws Exception {
        when(userRepository.existsByEmployeeId(anyString())).thenReturn(true);
        when(settingRepository.existsByKey("DEADLINE")).thenReturn(false);
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        dataInitializer.run();

        verify(settingRepository).save(argThat(s ->
                s.getKey().equals("DEADLINE") && s.getValue().equals("2030-12-31T23:59:59Z")));
    }
}
