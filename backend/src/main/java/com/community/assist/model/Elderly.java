package com.community.assist.model;

import com.community.assist.model.Enums.DisabilityLevel;
import com.community.assist.model.Enums.ElderlyStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 失能老人档案：失能等级、居住环境、照护人、医保/补贴资格、所需辅具类别。
 */
@Data
@Entity
@Table(name = "elderly")
public class Elderly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 8)
    private String gender;

    private Integer age;

    @Column(length = 32)
    private String idCard;

    @Column(length = 32)
    private String phone;

    @Column(length = 64)
    private String community;

    @Column(length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DisabilityLevel disabilityLevel;

    /** 居住环境描述（楼层/电梯/房屋结构等） */
    @Column(length = 1000)
    private String livingEnv;

    @Column(length = 64)
    private String caregiverName;

    @Column(length = 32)
    private String caregiverRelation;

    @Column(length = 32)
    private String caregiverPhone;

    /** 医保类型 */
    @Column(length = 64)
    private String insuranceType;

    /** 是否具备补贴资格 */
    private Boolean subsidyEligible = false;

    /** 补贴类型（长护险/民政补贴等） */
    @Column(length = 64)
    private String subsidyType;

    /** 所需辅具类别，逗号分隔的 DeviceCategory */
    @Column(length = 500)
    private String neededDevices;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ElderlyStatus status = ElderlyStatus.ACTIVE;

    @Column(length = 1000)
    private String remark;

    private LocalDateTime createdAt = LocalDateTime.now();
}
