package com.community.assist.repo;

import com.community.assist.model.RepairOrder;
import com.community.assist.model.Enums.RepairStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairOrderRepository extends JpaRepository<RepairOrder, Long> {
    List<RepairOrder> findByDeviceUnitIdOrderByIdDesc(Long deviceUnitId);
    List<RepairOrder> findByStatusOrderByIdDesc(RepairStatus status);
    List<RepairOrder> findAllByOrderByIdDesc();
    long countByStatus(RepairStatus status);
}
