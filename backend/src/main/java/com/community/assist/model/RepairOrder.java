package com.community.assist.model;

import com.community.assist.model.Enums.FeedbackType;
import com.community.assist.model.Enums.RepairPriority;
import com.community.assist.model.Enums.RepairStatus;
import com.community.assist.model.Enums.VisitResult;
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

    /** 误用判定：结合出库适配记录判断是否家属误用 */
    private Boolean misuse;

    @Column(length = 500)
    private String misuseNote;

    // ---------- 上门排程 ----------
    /** 故障类型（来自反馈） */
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private FeedbackType faultType;

    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private RepairPriority priority;

    @Column(length = 255)
    private String priorityReason;

    /** 老人是否独居（排程时快照） */
    private Boolean livesAlone;

    @Column(length = 32)
    private String assigneeName;

    /** 维修员距离（km） */
    private Double distanceKm;

    private LocalDateTime scheduledAt;

    @Column(length = 64)
    private String sparePartName;

    /** 备件是否有库存 */
    private Boolean spareReady;

    /** 通知照护人的临时安全措施 */
    @Column(length = 500)
    private String safetyNotice;

    /** 提示照护人的临时替代办法 */
    @Column(length = 500)
    private String alternativeNotice;

    // ---------- 到场回传 ----------
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private VisitResult visitResult;

    @Column(length = 500)
    private String visitNote;

    private LocalDateTime visitAt;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime finishedAt;
}
