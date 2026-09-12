package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.Role;
import com.community.assist.repo.*;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/elderly")
public class ElderlyController {

    private final ElderlyRepository elderlyRepo;
    private final AssessmentRepository assessmentRepo;
    private final RentalOrderRepository rentalRepo;
    private final FeedbackRepository feedbackRepo;
    private final RepairOrderRepository repairRepo;
    private final PaymentRepository paymentRepo;
    private final SubsidyRepository subsidyRepo;
    private final ServiceEventRepository eventRepo;
    private final DeviceModelRepository modelRepo;
    private final DeviceUnitRepository unitRepo;
    private final CurrentUser currentUser;

    public ElderlyController(ElderlyRepository elderlyRepo, AssessmentRepository assessmentRepo,
                             RentalOrderRepository rentalRepo, FeedbackRepository feedbackRepo,
                             RepairOrderRepository repairRepo, PaymentRepository paymentRepo,
                             SubsidyRepository subsidyRepo, ServiceEventRepository eventRepo,
                             DeviceModelRepository modelRepo, DeviceUnitRepository unitRepo,
                             CurrentUser currentUser) {
        this.elderlyRepo = elderlyRepo;
        this.assessmentRepo = assessmentRepo;
        this.rentalRepo = rentalRepo;
        this.feedbackRepo = feedbackRepo;
        this.repairRepo = repairRepo;
        this.paymentRepo = paymentRepo;
        this.subsidyRepo = subsidyRepo;
        this.eventRepo = eventRepo;
        this.modelRepo = modelRepo;
        this.unitRepo = unitRepo;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Elderly> list(@RequestParam(required = false) String keyword) {
        User u = currentUser.get();
        if (u.getRole() == Role.FAMILY) {
            Long eid = u.getElderlyId();
            return eid == null ? List.of() : elderlyRepo.findById(eid).map(List::of).orElse(List.of());
        }
        if (keyword != null && !keyword.isBlank()) {
            return elderlyRepo.findByNameContainingOrderByIdDesc(keyword.trim());
        }
        return elderlyRepo.findAllByOrderByIdDesc();
    }

    @GetMapping("/{id}")
    public Elderly one(@PathVariable Long id) {
        Elderly e = elderlyRepo.findById(id).orElseThrow(() -> BizException.notFound("老人档案不存在"));
        currentUser.checkFamilyScope(currentUser.get(), e.getId());
        return e;
    }

    @PostMapping
    public Elderly create(@RequestBody Elderly req) {
        currentUser.requireAny(Role.STAFF, Role.ADMIN);
        req.setId(null);
        if (req.getName() == null || req.getName().isBlank()) {
            throw BizException.badRequest("老人姓名不能为空");
        }
        return elderlyRepo.save(req);
    }

    @PutMapping("/{id}")
    public Elderly update(@PathVariable Long id, @RequestBody Elderly req) {
        currentUser.requireAny(Role.STAFF, Role.ADMIN);
        Elderly e = elderlyRepo.findById(id).orElseThrow(() -> BizException.notFound("老人档案不存在"));
        req.setId(e.getId());
        req.setCreatedAt(e.getCreatedAt());
        return elderlyRepo.save(req);
    }

    /**
     * 老人维度档案：评估、租赁、反馈、维修、费用、补贴、辅具事件 全量时间线。
     */
    @GetMapping("/{id}/timeline")
    @Transactional(readOnly = true)
    public Map<String, Object> timeline(@PathVariable Long id) {
        User u = currentUser.get();
        currentUser.checkFamilyScope(u, id);
        Elderly e = elderlyRepo.findById(id).orElseThrow(() -> BizException.notFound("老人档案不存在"));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("elderly", e);
        m.put("assessments", assessmentRepo.findByElderlyIdOrderByIdDesc(id));

        List<Map<String, Object>> rentals = new ArrayList<>();
        for (RentalOrder o : rentalRepo.findByElderlyIdOrderByIdDesc(id)) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("order", o);
            modelRepo.findById(o.getModelId()).ifPresent(model -> r.put("modelName", model.getName()));
            unitRepo.findById(o.getDeviceUnitId()).ifPresent(unit -> r.put("serialNo", unit.getSerialNo()));
            rentals.add(r);
        }
        m.put("rentals", rentals);
        m.put("feedback", feedbackRepo.findByElderlyIdOrderByIdDesc(id));
        m.put("payments", paymentRepo.findByElderlyIdOrderByIdDesc(id));
        m.put("subsidies", subsidyRepo.findByElderlyIdOrderByIdDesc(id));
        m.put("events", eventRepo.findByElderlyIdOrderByIdDesc(id));

        // 该老人相关的维修单（通过反馈/租赁关联的辅具）
        List<RepairOrder> repairs = new ArrayList<>();
        for (RentalOrder o : rentalRepo.findByElderlyIdOrderByIdDesc(id)) {
            for (RepairOrder ro : repairRepo.findByDeviceUnitIdOrderByIdDesc(o.getDeviceUnitId())) {
                if (o.getId().equals(ro.getRentalOrderId())) {
                    repairs.add(ro);
                }
            }
        }
        m.put("repairs", repairs);
        return m;
    }
}
