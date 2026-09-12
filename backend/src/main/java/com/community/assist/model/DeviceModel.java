package com.community.assist.model;

import com.community.assist.model.Enums.DeviceCategory;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 辅具型号（含押金与月租金标准）。
 */
@Data
@Entity
@Table(name = "device_model")
public class DeviceModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeviceCategory category;

    @Column(length = 500)
    private String spec;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(length = 1000)
    private String description;
}
