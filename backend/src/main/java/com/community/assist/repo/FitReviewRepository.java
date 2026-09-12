package com.community.assist.repo;

import com.community.assist.model.FitReview;
import com.community.assist.model.Enums.FitReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FitReviewRepository extends JpaRepository<FitReview, Long> {
    List<FitReview> findByElderlyIdOrderByIdDesc(Long elderlyId);
    List<FitReview> findByRentalOrderIdOrderByIdDesc(Long rentalOrderId);
    List<FitReview> findByStatusOrderByIdDesc(FitReviewStatus status);
    List<FitReview> findAllByOrderByIdDesc();
    long countByStatus(FitReviewStatus status);
}
