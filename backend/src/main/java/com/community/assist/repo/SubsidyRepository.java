package com.community.assist.repo;

import com.community.assist.model.SubsidyApplication;
import com.community.assist.model.Enums.SubsidyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubsidyRepository extends JpaRepository<SubsidyApplication, Long> {
    List<SubsidyApplication> findByElderlyIdOrderByIdDesc(Long elderlyId);
    List<SubsidyApplication> findByRentalOrderIdOrderByIdDesc(Long rentalOrderId);
    List<SubsidyApplication> findByStatusOrderByIdDesc(SubsidyStatus status);
    List<SubsidyApplication> findAllByOrderByIdDesc();
    long countByStatus(SubsidyStatus status);
}
