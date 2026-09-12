package com.community.assist.model;

import com.community.assist.model.Enums.SubsidyStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 补贴申请：申请 → 审核 → 租赁结案且辅具完成回收质检后核销。
 */
@Data
@Entity
@Table(name = "subsidy_application")
public class SubsidyApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long elderlyId;

    @Column(nullable = false)
    private Long rentalOrderId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SubsidyStatus status = SubsidyStatus.PENDING;

    @Column(length = 500)
    private String applyReason;

    @Column(length = 500)
    private String reviewNote;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime reviewedAt;

    private LocalDateTime writtenOffAt;
}
