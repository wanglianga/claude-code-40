package com.community.assist.repo;

import com.community.assist.model.RentalOrder;
import com.community.assist.model.Enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {
    List<RentalOrder> findByElderlyIdOrderByIdDesc(Long elderlyId);
    List<RentalOrder> findByDeviceUnitIdOrderByIdDesc(Long deviceUnitId);
    Optional<RentalOrder> findFirstByDeviceUnitIdOrderByIdDesc(Long deviceUnitId);
    List<RentalOrder> findByStatusOrderByIdDesc(RentalStatus status);
    List<RentalOrder> findAllByOrderByIdDesc();
    long countByStatus(RentalStatus status);
}
