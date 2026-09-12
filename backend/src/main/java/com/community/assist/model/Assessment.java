package com.community.assist.model;

import com.community.assist.model.Enums.AssessmentStatus;
import com.community.assist.model.Enums.CaregiverAbility;
import com.community.assist.model.Enums.DeviceCategory;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入户评估：门宽、电梯、床边空间、卫生间尺寸、照护人操作能力 → 辅具建议。
 */
@Data
@Entity
@Table(name = "assessment")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long elderlyId;

    /** 指派的评估师（可空，待指派） */
    private Long assessorId;

    @Column(length = 64)
    private String assessorName;

    private LocalDate visitDate;

    /** 门宽（厘米） */
    private Integer doorWidthCm;

    /** 是否有电梯 */
    private Boolean hasElevator;

    private Integer floor;

    /** 床边空间（厘米） */
    private Integer bedsideSpaceCm;

    private Integer bathroomWidthCm;

    private Integer bathroomLengthCm;

    /** 照护人操作能力 */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CaregiverAbility caregiverAbility;

    @Column(length = 1000)
    private String notes;

    /** 建议辅具类别 */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeviceCategory recommendedCategory;

    /** 建议型号 */
    private Long recommendedModelId;

    @Column(length = 1000)
    private String recommendationNote;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AssessmentStatus status = AssessmentStatus.PENDING;

    /** 触发再次评估的反馈 id（可空） */
    private Long fromFeedbackId;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;
}
