package com.community.assist.repo;

import com.community.assist.model.Feedback;
import com.community.assist.model.Enums.FeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByElderlyIdOrderByIdDesc(Long elderlyId);
    List<Feedback> findByRentalOrderIdOrderByIdDesc(Long rentalOrderId);
    List<Feedback> findByStatusOrderByIdDesc(FeedbackStatus status);
    List<Feedback> findAllByOrderByIdDesc();
    long countByStatus(FeedbackStatus status);
}
