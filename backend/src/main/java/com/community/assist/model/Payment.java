package com.community.assist.model;

import com.community.assist.model.Enums.PaymentDirection;
import com.community.assist.model.Enums.PaymentStatus;
import com.community.assist.model.Enums.PaymentType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用流水：押金、租金、补贴、维修费分账核算，
 * 每笔费用关联老人、租赁单、辅具与对应的服务动作。
 */
@Data
@Entity
@Table(name = "payment", indexes = {
        @Index(name = "idx_pay_elderly", columnList = "elderlyId"),
        @Index(name = "idx_pay_rental", columnList = "rentalOrderId")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String paymentNo;

    /** 关联老人；平台支出类费用（如维修费）可为空 */
    private Long elderlyId;

    private Long rentalOrderId;

    private Long deviceUnitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PaymentDirection direction;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private PaymentStatus status = PaymentStatus.PENDING;

    /** 支付方式 */
    @Column(length = 32)
    private String method;

    /** 对应的服务动作（如 租赁确认/首月租金/维修完成/结案退押金） */
    @Column(length = 128)
    private String relatedAction;

    @Column(length = 500)
    private String remark;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime paidAt;
}
