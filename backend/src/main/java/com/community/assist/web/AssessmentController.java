package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.AssessmentStatus;
import com.community.assist.model.Enums.CaregiverAbility;
import com.community.assist.model.Enums.DeviceCategory;
import com.community.assist.model.Enums.Role;
import com.community.assist.repo.*;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final AssessmentRepository assessmentRepo;
    private final ElderlyRepository elderlyRepo;
    private final UserRepository userRepo;
    private final DeviceModelRepository modelRepo;
    private final CurrentUser currentUser;

    public AssessmentController(AssessmentRepository assessmentRepo, ElderlyRepository elderlyRepo,
                                UserRepository userRepo, DeviceModelRepository modelRepo,
                                CurrentUser currentUser) {
        this.assessmentRepo = assessmentRepo;
        this.elderlyRepo = elderlyRepo;
        this.userRepo = userRepo;
        this.modelRepo = modelRepo;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) AssessmentStatus status) {
        User u = currentUser.get();
        List<Assessment> src;
        if (u.getRole() == Role.ASSESSOR) {
            src = assessmentRepo.findByAssessorIdOrderByIdDesc(u.getId());
        } else if (u.getRole() == Role.FAMILY) {
            src = u.getElderlyId() == null ? List.of() : assessmentRepo.findByElderlyIdOrderByIdDesc(u.getElderlyId());
        } else if (status != null) {
            src = assessmentRepo.findByStatusOrderByIdDesc(status);
        } else {
            src = assessmentRepo.findAllByOrderByIdDesc();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (Assessment a : src) {
            res.add(view(a));
        }
        return res;
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable Long id) {
        Assessment a = assessmentRepo.findById(id).orElseThrow(() -> BizException.notFound("评估单不存在"));
        return view(a);
    }

    public record CreateReq(Long elderlyId, Long assessorId) {
    }

    /** 社区工作人员发起入户评估并指派评估师 */
    @PostMapping
    public Map<String, Object> create(@RequestBody CreateReq req) {
        currentUser.requireAny(Role.STAFF, Role.ADMIN);
        Elderly e = elderlyRepo.findById(req.elderlyId()).orElseThrow(() -> BizException.notFound("老人档案不存在"));
        Assessment a = new Assessment();
        a.setElderlyId(e.getId());
        if (req.assessorId() != null) {
            User assessor = userRepo.findById(req.assessorId())
                    .orElseThrow(() -> BizException.notFound("评估师不存在"));
            if (assessor.getRole() != Role.ASSESSOR) {
                throw BizException.badRequest("指派对象不是评估师");
            }
            a.setAssessorId(assessor.getId());
            a.setAssessorName(assessor.getName());
        }
        return view(assessmentRepo.save(a));
    }

    public record CompleteReq(java.time.LocalDate visitDate, Integer doorWidthCm, Boolean hasElevator,
                              Integer floor, Integer bedsideSpaceCm, Integer bathroomWidthCm,
                              Integer bathroomLengthCm, String caregiverAbility, String notes,
                              String recommendedCategory, Long recommendedModelId, String recommendationNote) {
    }

    /** 评估师入户后回填测量数据并生成辅具建议 */
    @PutMapping("/{id}/complete")
    @Transactional
    public Map<String, Object> complete(@PathVariable Long id, @RequestBody CompleteReq req) {
        User u = currentUser.requireAny(Role.ASSESSOR, Role.ADMIN);
        Assessment a = assessmentRepo.findById(id).orElseThrow(() -> BizException.notFound("评估单不存在"));
        if (a.getStatus() == AssessmentStatus.COMPLETED) {
            throw BizException.badRequest("该评估已完成");
        }
        if (u.getRole() == Role.ASSESSOR && a.getAssessorId() != null && !a.getAssessorId().equals(u.getId())) {
            throw BizException.forbidden("该评估未指派给当前评估师");
        }
        a.setVisitDate(req.visitDate());
        a.setDoorWidthCm(req.doorWidthCm());
        a.setHasElevator(req.hasElevator());
        a.setFloor(req.floor());
        a.setBedsideSpaceCm(req.bedsideSpaceCm());
        a.setBathroomWidthCm(req.bathroomWidthCm());
        a.setBathroomLengthCm(req.bathroomLengthCm());
        if (req.caregiverAbility() != null) {
            a.setCaregiverAbility(CaregiverAbility.valueOf(req.caregiverAbility()));
        }
        a.setNotes(req.notes());
        if (req.recommendedCategory() != null) {
            a.setRecommendedCategory(DeviceCategory.valueOf(req.recommendedCategory()));
        }
        a.setRecommendedModelId(req.recommendedModelId());
        a.setRecommendationNote(req.recommendationNote());
        a.setStatus(AssessmentStatus.COMPLETED);
        a.setCompletedAt(LocalDateTime.now());
        if (a.getAssessorId() == null) {
            a.setAssessorId(u.getId());
            a.setAssessorName(u.getName());
        }
        return view(assessmentRepo.save(a));
    }

    private Map<String, Object> view(Assessment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("assessment", a);
        elderlyRepo.findById(a.getElderlyId()).ifPresent(e -> m.put("elderlyName", e.getName()));
        if (a.getRecommendedModelId() != null) {
            modelRepo.findById(a.getRecommendedModelId()).ifPresent(dm -> m.put("modelName", dm.getName()));
        }
        return m;
    }
}
