package com.community.assist.repo;

import com.community.assist.model.Assessment;
import com.community.assist.model.Enums.AssessmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByElderlyIdOrderByIdDesc(Long elderlyId);
    List<Assessment> findByAssessorIdOrderByIdDesc(Long assessorId);
    List<Assessment> findByStatusOrderByIdDesc(AssessmentStatus status);
    List<Assessment> findAllByOrderByIdDesc();
}
