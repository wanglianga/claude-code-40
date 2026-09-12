package com.community.assist.model;

import com.community.assist.model.Enums.ServiceEventType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 辅具生命周期事件（租赁、配送、安装、维修、换型、回收、消毒、质检、再上架、报废……），
 * 同一件辅具的所有事件串成它的完整履历。
 */
@Data
@Entity
@Table(name = "service_event", indexes = {
        @Index(name = "idx_event_unit", columnList = "deviceUnitId"),
        @Index(name = "idx_event_elderly", columnList = "elderlyId")
})
public class ServiceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long deviceUnitId;

    private Long rentalOrderId;

    private Long elderlyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServiceEventType type;

    @Column(length = 128)
    private String title;

    @Column(length = 1000)
    private String detail;

    @Column(length = 64)
    private String operatorName;

    private LocalDateTime createdAt = LocalDateTime.now();
}
