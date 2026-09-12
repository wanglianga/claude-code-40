package com.community.assist.model;

import com.community.assist.model.Enums.FeedbackStatus;
import com.community.assist.model.Enums.FeedbackType;
import com.community.assist.model.Enums.Resolution;
import com.community.assist.model.Enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 使用反馈：磨损/异响/尺寸不适/摔倒/不会操作，按风险分级处置。
 */
@Data
@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long rentalOrderId;

    @Column(nullable = false)
    private Long elderlyId;

    @Column(nullable = false)
    private Long deviceUnitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private FeedbackType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private RiskLevel riskLevel;

    @Column(length = 1000)
    private String description;

    /** 提交人（家属/照护员） */
    @Column(length = 64)
    private String submitter;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Resolution resolution;

    @Column(length = 1000)
    private String handleNote;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime resolvedAt;
}
