package com.community.assist.model;

import com.community.assist.model.Enums.CloseReason;
import com.community.assist.model.Enums.RentalStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 租赁订单：库存锁定 → 家属确认 → 配送 → 安装 → 在租 → 结案回收。
 */
@Data
@Entity
@Table(name = "rental_order")
public class RentalOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String orderNo;

    @Column(nullable = false)
    private Long elderlyId;

    private Long assessmentId;

    @Column(nullable = false)
    private Long deviceUnitId;

    @Column(nullable = false)
    private Long modelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RentalStatus status = RentalStatus.PENDING_CONFIRM;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 255)
    private String deliveryAddress;

    private LocalDateTime confirmTime;

    private LocalDateTime deliveryTime;

    private LocalDateTime installTime;

    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private CloseReason closeReason;

    @Column(length = 1000)
    private String closeNote;

    private LocalDateTime createdAt = LocalDateTime.now();
}
