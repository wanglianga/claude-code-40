package com.community.assist.model;

import com.community.assist.model.Enums.Role;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 32)
    private String phone;

    /** 家属账号关联的老人档案 id */
    private Long elderlyId;

    private LocalDateTime createdAt = LocalDateTime.now();
}
