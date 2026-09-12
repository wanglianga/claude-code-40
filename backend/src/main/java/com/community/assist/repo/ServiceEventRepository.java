package com.community.assist.repo;

import com.community.assist.model.ServiceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServiceEventRepository extends JpaRepository<ServiceEvent, Long> {
    List<ServiceEvent> findByDeviceUnitIdOrderByIdDesc(Long deviceUnitId);
    List<ServiceEvent> findByElderlyIdOrderByIdDesc(Long elderlyId);
    List<ServiceEvent> findByRentalOrderIdOrderByIdAsc(Long rentalOrderId);
    List<ServiceEvent> findTop10ByOrderByIdDesc();
}
