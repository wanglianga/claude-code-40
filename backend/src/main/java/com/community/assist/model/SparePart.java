package com.community.assist.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * 备件库存：维修排程时判断备件是否有货，维修完成后扣减。
 */
@Data
@Entity
@Table(name = "spare_part")
public class SparePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 32)
    private String partCode;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false)
    private Integer stock = 0;

    @Column(length = 16)
    private String unit = "件";
}
