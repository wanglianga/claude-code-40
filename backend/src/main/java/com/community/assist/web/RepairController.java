package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.*;
import com.community.assist.repo.*;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import com.community.assist.service.EventService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修单：上门维修 → 完成（记录维修费用，单独核算）。
 */
@RestController
@RequestMapping("/api/repairs")
public class RepairController {

    private final RepairOrderRepository repairRepo;
    private final DeviceUnitRepository unitRepo;
    private final DeviceModelRepository modelRepo;
    private final RentalOrderRepository rentalRepo;
    private final PaymentRepository paymentRepo;
    private final EventService eventService;
    private final CurrentUser currentUser;

    public RepairController(RepairOrderRepository repairRepo, DeviceUnitRepository unitRepo,
                            DeviceModelRepository modelRepo, RentalOrderRepository rentalRepo,
                            PaymentRepository paymentRepo, EventService eventService,
                            CurrentUser currentUser) {
        this.repairRepo = repairRepo;
        this.unitRepo = unitRepo;
        this.modelRepo = modelRepo;
        this.rentalRepo = rentalRepo;
        this.paymentRepo = paymentRepo;
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) RepairStatus status) {
        currentUser.get();
        List<RepairOrder> src = status != null
                ? repairRepo.findByStatusOrderByIdDesc(status)
                : repairRepo.findAllByOrderByIdDesc();
        List<Map<String, Object>> res = new ArrayList<>();
        for (RepairOrder ro : src) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("repair", ro);
            unitRepo.findById(ro.getDeviceUnitId()).ifPresent(u -> {
                m.put("serialNo", u.getSerialNo());
                modelRepo.findById(u.getModelId()).ifPresent(dm -> m.put("modelName", dm.getName()));
            });
            // 关联租赁的出库适配记录（用于判断是否误用）
            if (ro.getRentalOrderId() != null) {
                rentalRepo.findById(ro.getRentalOrderId()).ifPresent(o -> {
                    Map<String, Object> handover = new LinkedHashMap<>();
                    handover.put("elderlyCondition", o.getElderlyCondition());
                    handover.put("fittingAdvice", o.getFittingAdvice());
                    handover.put("familyConfirmed", o.getFamilyConfirmed());
                    m.put("handover", handover);
                });
            }
            res.add(m);
        }
        return res;
    }

    @PostMapping("/{id}/start")
    @Transactional
    public Map<String, Object> start(@PathVariable Long id) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN, Role.STAFF);
        RepairOrder ro = repairRepo.findById(id).orElseThrow(() -> BizException.notFound("维修单不存在"));
        if (ro.getStatus() != RepairStatus.PENDING) {
            throw BizException.badRequest("仅待处理维修单可开始维修");
        }
        ro.setStatus(RepairStatus.IN_PROGRESS);
        repairRepo.save(ro);
        DeviceUnit unit = unitRepo.findById(ro.getDeviceUnitId()).orElseThrow();
        eventService.record(unit.getId(), ro.getRentalOrderId(), null, ServiceEventType.REPAIR,
                "开始上门维修", "维修单 " + ro.getRepairNo(), op.getName());
        return Map.of("repair", ro);
    }

    public record FinishReq(BigDecimal cost, String resultNote, Boolean misuse, String misuseNote) {
    }

    @PostMapping("/{id}/finish")
    @Transactional
    public Map<String, Object> finish(@PathVariable Long id, @RequestBody FinishReq req) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN, Role.STAFF);
        RepairOrder ro = repairRepo.findById(id).orElseThrow(() -> BizException.notFound("维修单不存在"));
        if (ro.getStatus() == RepairStatus.DONE) {
            throw BizException.badRequest("维修单已完成");
        }
        ro.setStatus(RepairStatus.DONE);
        ro.setCost(req.cost() == null ? BigDecimal.ZERO : req.cost());
        ro.setResultNote(req.resultNote());
        ro.setMisuse(req.misuse());
        ro.setMisuseNote(req.misuseNote());
        ro.setFinishedAt(LocalDateTime.now());
        repairRepo.save(ro);

        DeviceUnit unit = unitRepo.findById(ro.getDeviceUnitId()).orElseThrow();
        // 租约仍在 → 辅具回到租赁中；否则回到回收待消毒
        boolean rentalActive = ro.getRentalOrderId() != null
                && rentalRepo.findById(ro.getRentalOrderId())
                .map(o -> o.getStatus() == RentalStatus.ACTIVE || o.getStatus() == RentalStatus.DELIVERED)
                .orElse(false);
        unit.setStatus(rentalActive ? DeviceStatus.LEASED : DeviceStatus.RECALLED);
        unitRepo.save(unit);

        Long elderlyId = rentalActive ? rentalRepo.findById(ro.getRentalOrderId()).get().getElderlyId() : null;
        eventService.record(unit.getId(), ro.getRentalOrderId(), elderlyId, ServiceEventType.REPAIR,
                "维修完成", "维修单 " + ro.getRepairNo() + " 完成，费用 ¥" + ro.getCost()
                        + (req.resultNote() == null ? "" : "；" + req.resultNote())
                        + (req.misuse() == null ? "" : (req.misuse() ? "；判定：家属误用" : "；判定：非误用（正常磨损）")),
                op.getName());

        // 维修费用单独核算（平台支出）
        Payment p = RentalController.newPayment(elderlyId, ro.getRentalOrderId(), unit.getId(),
                PaymentType.REPAIR, PaymentDirection.EXPENSE, ro.getCost(), "维修完成-维修费用");
        p.setStatus(PaymentStatus.PAID);
        p.setPaidAt(LocalDateTime.now());
        p.setRemark("维修单 " + ro.getRepairNo());
        paymentRepo.save(p);
        return Map.of("repair", ro);
    }
}
