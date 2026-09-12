package com.community.assist.model;

import com.community.assist.model.Enums.RepairStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 维修单：由反馈处置触发，记录维修费用。
 */
@Data
@Entity
@Table(name = "repair_order")
public class RepairOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String repairNo;

    @Column(nullable = false)
    private Long deviceUnitId;

    private Long feedbackId;

    private Long rentalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RepairStatus status = RepairStatus.PENDING;

    /** 维修费用 */
    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String resultNote;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime finishedAt;
}
