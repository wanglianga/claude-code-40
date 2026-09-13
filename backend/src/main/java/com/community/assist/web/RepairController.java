package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.*;
import com.community.assist.repo.*;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import com.community.assist.service.EventService;
import com.community.assist.service.RepairSchedulingService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 维修单：自动排程（风险/独居/距离/备件）→ 上门 → 回传试用结果 → 完成（误用判定、备件扣减）。
 */
@RestController
@RequestMapping("/api/repairs")
public class RepairController {

    private final RepairOrderRepository repairRepo;
    private final DeviceUnitRepository unitRepo;
    private final DeviceModelRepository modelRepo;
    private final RentalOrderRepository rentalRepo;
    private final ElderlyRepository elderlyRepo;
    private final PaymentRepository paymentRepo;
    private final SparePartRepository sparePartRepo;
    private final EventService eventService;
    private final RepairSchedulingService schedulingService;
    private final CurrentUser currentUser;

    public RepairController(RepairOrderRepository repairRepo, DeviceUnitRepository unitRepo,
                            DeviceModelRepository modelRepo, RentalOrderRepository rentalRepo,
                            ElderlyRepository elderlyRepo, PaymentRepository paymentRepo,
                            SparePartRepository sparePartRepo, EventService eventService,
                            RepairSchedulingService schedulingService, CurrentUser currentUser) {
        this.repairRepo = repairRepo;
        this.unitRepo = unitRepo;
        this.modelRepo = modelRepo;
        this.rentalRepo = rentalRepo;
        this.elderlyRepo = elderlyRepo;
        this.paymentRepo = paymentRepo;
        this.sparePartRepo = sparePartRepo;
        this.eventService = eventService;
        this.schedulingService = schedulingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) RepairStatus status) {
        User u = currentUser.get();
        List<RepairOrder> src;
        if (u.getRole() == Role.FAMILY) {
            // 家属仅可见自家老人租赁关联的维修单（含安全提示）
            if (u.getElderlyId() == null) {
                src = List.of();
            } else {
                List<Long> orderIds = rentalRepo.findByElderlyIdOrderByIdDesc(u.getElderlyId())
                        .stream().map(RentalOrder::getId).toList();
                src = repairRepo.findAllByOrderByIdDesc().stream()
                        .filter(r -> r.getRentalOrderId() != null && orderIds.contains(r.getRentalOrderId()))
                        .toList();
            }
        } else {
            src = status != null
                    ? repairRepo.findByStatusOrderByIdDesc(status)
                    : repairRepo.findAllByOrderByIdDesc();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (RepairOrder ro : src) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("repair", ro);
            unitRepo.findById(ro.getDeviceUnitId()).ifPresent(unit -> {
                m.put("serialNo", unit.getSerialNo());
                modelRepo.findById(unit.getModelId()).ifPresent(dm -> m.put("modelName", dm.getName()));
            });
            if (ro.getRentalOrderId() != null) {
                rentalRepo.findById(ro.getRentalOrderId()).ifPresent(o -> {
                    Map<String, Object> handover = new LinkedHashMap<>();
                    handover.put("elderlyCondition", o.getElderlyCondition());
                    handover.put("fittingAdvice", o.getFittingAdvice());
                    handover.put("familyConfirmed", o.getFamilyConfirmed());
                    m.put("handover", handover);
                    elderlyRepo.findById(o.getElderlyId()).ifPresent(e -> m.put("elderlyName", e.getName()));
                });
            }
            res.add(m);
        }
        return res;
    }

    /** 备件库存 */
    @GetMapping("/spare-parts")
    public List<SparePart> spareParts() {
        currentUser.get();
        return sparePartRepo.findAll();
    }

    /** 重新排程（按最新风险/独居/距离/备件重算） */
    @PostMapping("/{id}/schedule")
    @Transactional
    public Map<String, Object> schedule(@PathVariable Long id) {
        User op = currentUser.requireAny(Role.STAFF, Role.ADMIN);
        RepairOrder ro = repairRepo.findById(id).orElseThrow(() -> BizException.notFound("维修单不存在"));
        if (ro.getStatus() == RepairStatus.DONE) {
            throw BizException.badRequest("维修单已完成，无需排程");
        }
        if (ro.getRentalOrderId() == null) {
            throw BizException.badRequest("该维修单未关联租赁，无法排程");
        }
        RentalOrder o = rentalRepo.findById(ro.getRentalOrderId()).orElseThrow();
        Elderly e = elderlyRepo.findById(o.getElderlyId()).orElseThrow();
        schedulingService.schedule(ro, e, ro.getFaultType(), op.getName());
        return Map.of("repair", ro);
    }

    /** 维修员出发/到场开始维修 */
    @PostMapping("/{id}/start")
    @Transactional
    public Map<String, Object> start(@PathVariable Long id) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN, Role.STAFF);
        RepairOrder ro = repairRepo.findById(id).orElseThrow(() -> BizException.notFound("维修单不存在"));
        if (ro.getStatus() != RepairStatus.PENDING && ro.getStatus() != RepairStatus.SCHEDULED) {
            throw BizException.badRequest("仅待处理/已排程维修单可开始维修");
        }
        ro.setStatus(RepairStatus.IN_PROGRESS);
        repairRepo.save(ro);
        DeviceUnit unit = unitRepo.findById(ro.getDeviceUnitId()).orElseThrow();
        eventService.record(unit.getId(), ro.getRentalOrderId(), null, ServiceEventType.REPAIR,
                "维修员到场开始维修", "维修单 " + ro.getRepairNo() + "（" + ro.getAssigneeName() + "）", op.getName());
        return Map.of("repair", ro);
    }

    public record FinishReq(BigDecimal cost, String resultNote, Boolean misuse, String misuseNote,
                            String visitResult, String visitNote) {
    }

    /** 完成上门：必须回传试用结果；试用仍有问题则退回待上门 */
    @PostMapping("/{id}/finish")
    @Transactional
    public Map<String, Object> finish(@PathVariable Long id, @RequestBody FinishReq req) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN, Role.STAFF);
        RepairOrder ro = repairRepo.findById(id).orElseThrow(() -> BizException.notFound("维修单不存在"));
        if (ro.getStatus() == RepairStatus.DONE) {
            throw BizException.badRequest("维修单已完成");
        }
        if (req.visitResult() == null) {
            throw BizException.badRequest("请回传试用结果（试用正常/仍有问题）");
        }
        VisitResult visitResult = VisitResult.valueOf(req.visitResult());
        ro.setVisitResult(visitResult);
        ro.setVisitNote(req.visitNote());
        ro.setVisitAt(LocalDateTime.now());

        DeviceUnit unit = unitRepo.findById(ro.getDeviceUnitId()).orElseThrow();
        if (visitResult == VisitResult.ABNORMAL) {
            // 试用仍有问题 → 退回待上门，重新排程
            ro.setStatus(RepairStatus.PENDING);
            repairRepo.save(ro);
            eventService.record(unit.getId(), ro.getRentalOrderId(), null, ServiceEventType.REPAIR,
                    "上门试用仍有问题", "维修单 " + ro.getRepairNo() + " 需再次上门："
                            + (req.visitNote() == null ? "" : req.visitNote()), op.getName());
            return Map.of("repair", ro);
        }

        ro.setStatus(RepairStatus.DONE);
        ro.setCost(req.cost() == null ? BigDecimal.ZERO : req.cost());
        ro.setResultNote(req.resultNote());
        ro.setMisuse(req.misuse());
        ro.setMisuseNote(req.misuseNote());
        ro.setFinishedAt(LocalDateTime.now());
        repairRepo.save(ro);

        // 备件出库
        String partCode = RepairSchedulingService.partCodeOf(ro.getFaultType());
        if (partCode != null) {
            sparePartRepo.findByPartCode(partCode).ifPresent(p -> {
                if (p.getStock() > 0) {
                    p.setStock(p.getStock() - 1);
                    sparePartRepo.save(p);
                }
            });
        }

        // 租约仍在 → 辅具回到租赁中；否则回到回收待消毒
        boolean rentalActive = ro.getRentalOrderId() != null
                && rentalRepo.findById(ro.getRentalOrderId())
                .map(o -> o.getStatus() == RentalStatus.ACTIVE || o.getStatus() == RentalStatus.DELIVERED)
                .orElse(false);
        unit.setStatus(rentalActive ? DeviceStatus.LEASED : DeviceStatus.RECALLED);
        unitRepo.save(unit);

        Long elderlyId = rentalActive ? rentalRepo.findById(ro.getRentalOrderId()).get().getElderlyId() : null;
        eventService.record(unit.getId(), ro.getRentalOrderId(), elderlyId, ServiceEventType.REPAIR,
                "维修完成（试用正常）", "维修单 " + ro.getRepairNo() + " 完成，费用 ¥" + ro.getCost()
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
