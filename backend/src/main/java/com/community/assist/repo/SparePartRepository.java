package com.community.assist.repo;

import com.community.assist.model.SparePart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SparePartRepository extends JpaRepository<SparePart, Long> {
    Optional<SparePart> findByPartCode(String partCode);
}
