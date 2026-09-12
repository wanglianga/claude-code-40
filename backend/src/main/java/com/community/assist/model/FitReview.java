package com.community.assist.model;

import com.community.assist.model.Enums.FitAction;
import com.community.assist.model.Enums.FitConclusion;
import com.community.assist.model.Enums.FitReviewStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 尺寸不适复评单（护理床/轮椅）：
 * 家属上传使用照片、身高体重、房间尺寸、照护人说明 →
 * 评估师判断操作问题/尺寸不合/病情变化 → 培训/换型/暂停租赁。
 */
@Data
@Entity
@Table(name = "fit_review", indexes = {
        @Index(name = "idx_fit_rental", columnList = "rentalOrderId"),
        @Index(name = "idx_fit_elderly", columnList = "elderlyId")
})
public class FitReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 40)
    private String reviewNo;

    /** 触发反馈（可空） */
    private Long feedbackId;

    @Column(nullable = false)
    private Long rentalOrderId;

    @Column(nullable = false)
    private Long elderlyId;

    @Column(nullable = false)
    private Long deviceUnitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FitReviewStatus status = FitReviewStatus.PENDING_INFO;

    // ---------- 家属上传资料 ----------
    /** 使用照片文件名，逗号分隔 */
    @Column(length = 1000)
    private String photoUrls;

    private Integer heightCm;

    private Double weightKg;

    private Integer roomWidthCm;

    private Integer roomLengthCm;

    /** 照护人说明 */
    @Column(length = 1000)
    private String caregiverNote;

    private LocalDateTime submittedAt;

    // ---------- 评估师复评结论 ----------
    private Long assessorId;

    @Column(length = 64)
    private String assessorName;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private FitConclusion conclusion;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FitAction action;

    /** 结论说明（家属可见的换型/培训具体原因） */
    @Column(length = 1000)
    private String conclusionNote;

    private LocalDateTime reviewedAt;

    // ---------- 换型执行 ----------
    private Long newModelId;

    /** 换型后重新确认的门宽（cm） */
    private Integer doorWidthReconfirmedCm;

    /** 换型后照护人操作是否已重新确认/培训 */
    private Boolean caregiverRetrained;

    @Column(length = 500)
    private String exchangeNote;

    private LocalDateTime exchangedAt;

    private LocalDateTime createdAt = LocalDateTime.now();
}
