package com.community.assist.model;

import com.community.assist.model.Enums.DeviceStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单件辅具（库存最小单位），全生命周期围绕它展开。
 */
@Data
@Entity
@Table(name = "device_unit")
public class DeviceUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String serialNo;

    @Column(nullable = false)
    private Long modelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeviceStatus status = DeviceStatus.IN_STOCK;

    @Column(length = 500)
    private String conditionNote;

    private LocalDate purchaseDate;

    /** 累计服务租期次数（回收再上架 +1） */
    private Integer rentalCount = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
}
