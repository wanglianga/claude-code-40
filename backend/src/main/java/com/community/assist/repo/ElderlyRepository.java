package com.community.assist.repo;

import com.community.assist.model.Elderly;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ElderlyRepository extends JpaRepository<Elderly, Long> {
    List<Elderly> findByNameContainingOrderByIdDesc(String keyword);
    List<Elderly> findAllByOrderByIdDesc();
}
