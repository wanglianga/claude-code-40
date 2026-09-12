package com.community.assist.repo;

import com.community.assist.model.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceModelRepository extends JpaRepository<DeviceModel, Long> {
    Optional<DeviceModel> findByCode(String code);
}
