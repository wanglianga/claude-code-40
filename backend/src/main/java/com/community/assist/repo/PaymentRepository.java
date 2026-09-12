package com.community.assist.repo;

import com.community.assist.model.Payment;
import com.community.assist.model.Enums.PaymentStatus;
import com.community.assist.model.Enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByElderlyIdOrderByIdDesc(Long elderlyId);
    List<Payment> findByRentalOrderIdOrderByIdAsc(Long rentalOrderId);
    Optional<Payment> findFirstByRentalOrderIdAndTypeOrderByIdAsc(Long rentalOrderId, PaymentType type);
    List<Payment> findByStatusOrderByIdDesc(PaymentStatus status);
    List<Payment> findAllByOrderByIdDesc();
}
