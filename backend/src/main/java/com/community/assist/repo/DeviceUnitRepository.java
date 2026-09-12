package com.community.assist.repo;

import com.community.assist.model.DeviceUnit;
import com.community.assist.model.Enums.DeviceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceUnitRepository extends JpaRepository<DeviceUnit, Long> {
    Optional<DeviceUnit> findFirstByModelIdAndStatusOrderByIdAsc(Long modelId, DeviceStatus status);
    List<DeviceUnit> findByModelIdOrderByIdDesc(Long modelId);
    List<DeviceUnit> findByStatusOrderByIdDesc(DeviceStatus status);
    List<DeviceUnit> findAllByOrderByIdDesc();
    long countByStatus(DeviceStatus status);
    long countByModelId(Long modelId);
    long countByModelIdAndStatus(Long modelId, DeviceStatus status);
}
