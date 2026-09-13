package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.*;
import com.community.assist.repo.*;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import com.community.assist.service.EventService;
import com.community.assist.service.FitReviewService;
import com.community.assist.service.RepairSchedulingService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 使用反馈：家属/照护员提交 → 按风险分级 → 维修/换型/再次评估。
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackRepository feedbackRepo;
    private final RentalOrderRepository rentalRepo;
    private final ElderlyRepository elderlyRepo;
    private final DeviceUnitRepository unitRepo;
    private final DeviceModelRepository modelRepo;
    private final RepairOrderRepository repairRepo;
    private final AssessmentRepository assessmentRepo;
    private final EventService eventService;
    private final FitReviewService fitReviewService;
    private final RepairSchedulingService schedulingService;
    private final CurrentUser currentUser;

    public FeedbackController(FeedbackRepository feedbackRepo, RentalOrderRepository rentalRepo,
                              ElderlyRepository elderlyRepo, DeviceUnitRepository unitRepo,
                              DeviceModelRepository modelRepo, RepairOrderRepository repairRepo,
                              AssessmentRepository assessmentRepo, EventService eventService,
                              FitReviewService fitReviewService, RepairSchedulingService schedulingService,
                              CurrentUser currentUser) {
        this.feedbackRepo = feedbackRepo;
        this.rentalRepo = rentalRepo;
        this.elderlyRepo = elderlyRepo;
        this.unitRepo = unitRepo;
        this.modelRepo = modelRepo;
        this.repairRepo = repairRepo;
        this.assessmentRepo = assessmentRepo;
        this.eventService = eventService;
        this.fitReviewService = fitReviewService;
        this.schedulingService = schedulingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) FeedbackStatus status) {
        User u = currentUser.get();
        List<Feedback> src;
        if (u.getRole() == Role.FAMILY) {
            src = u.getElderlyId() == null ? List.of() : feedbackRepo.findByElderlyIdOrderByIdDesc(u.getElderlyId());
        } else if (status != null) {
            src = feedbackRepo.findByStatusOrderByIdDesc(status);
        } else {
            src = feedbackRepo.findAllByOrderByIdDesc();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (Feedback f : src) {
            res.add(view(f));
        }
        return res;
    }

    public record CreateReq(Long rentalOrderId, String type, String description, String submitter) {
    }

    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody CreateReq req) {
        User op = currentUser.get();
        RentalOrder o = rentalRepo.findById(req.rentalOrderId())
                .orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        currentUser.checkFamilyScope(op, o.getElderlyId());
        if (o.getStatus() != RentalStatus.ACTIVE && o.getStatus() != RentalStatus.DELIVERED) {
            throw BizException.badRequest("仅在租订单可提交使用反馈");
        }
        FeedbackType type = FeedbackType.valueOf(req.type());
        Feedback f = new Feedback();
        f.setRentalOrderId(o.getId());
        f.setElderlyId(o.getElderlyId());
        f.setDeviceUnitId(o.getDeviceUnitId());
        f.setType(type);
        f.setRiskLevel(riskOf(type));
        f.setDescription(req.description());
        f.setSubmitter(req.submitter() == null || req.submitter().isBlank() ? op.getName() : req.submitter());
        feedbackRepo.save(f);

        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.FEEDBACK,
                "使用反馈-" + typeLabel(type),
                "风险等级 " + f.getRiskLevel() + "：" + (req.description() == null ? "" : req.description()),
                f.getSubmitter());
        return view(f);
    }

    public record HandleReq(String resolution, String note) {
    }

    /** 社区工作人员按风险处置：维修 / 换型 / 再次评估 / 无需处理 */
    @PostMapping("/{id}/handle")
    @Transactional
    public Map<String, Object> handle(@PathVariable Long id, @RequestBody HandleReq req) {
        User op = currentUser.requireAny(Role.STAFF, Role.ADMIN);
        Feedback f = feedbackRepo.findById(id).orElseThrow(() -> BizException.notFound("反馈不存在"));
        if (f.getStatus() == FeedbackStatus.RESOLVED) {
            throw BizException.badRequest("该反馈已处理");
        }
        Resolution resolution = Resolution.valueOf(req.resolution());
        RentalOrder o = rentalRepo.findById(f.getRentalOrderId())
                .orElseThrow(() -> BizException.notFound("关联租赁订单不存在"));
        DeviceUnit unit = unitRepo.findById(f.getDeviceUnitId()).orElseThrow();
        String note = req.note() == null ? "" : req.note();

        switch (resolution) {
            case REPAIR -> {
                RepairOrder ro = new RepairOrder();
                ro.setRepairNo(RentalController.genNo("RO"));
                ro.setDeviceUnitId(unit.getId());
                ro.setFeedbackId(f.getId());
                ro.setRentalOrderId(o.getId());
                ro.setFaultType(f.getType());
                ro.setDescription("反馈触发维修：" + (f.getDescription() == null ? "" : f.getDescription()));
                repairRepo.save(ro);
                // 自动按风险/独居/距离/备件排程，并通知照护人临时措施
                Elderly elderly = elderlyRepo.findById(o.getElderlyId()).orElseThrow();
                schedulingService.schedule(ro, elderly, f.getType(), op.getName());
                unit.setStatus(DeviceStatus.MAINTENANCE);
                unitRepo.save(unit);
                eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.REPAIR,
                        "生成维修单 " + ro.getRepairNo(), note, op.getName());
            }
            case EXCHANGE -> {
                DeviceUnit repl = unitRepo
                        .findFirstByModelIdAndStatusOrderByIdAsc(o.getModelId(), DeviceStatus.IN_STOCK)
                        .orElseThrow(() -> BizException.badRequest("同型号暂无可用库存，无法换型，请改用维修"));
                unit.setStatus(DeviceStatus.RECALLED);
                unitRepo.save(unit);
                eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.RECALL,
                        "换型退回", "辅具 " + unit.getSerialNo() + " 退回待消毒。" + note, op.getName());
                repl.setStatus(DeviceStatus.LEASED);
                unitRepo.save(repl);
                eventService.record(repl.getId(), o.getId(), o.getElderlyId(), ServiceEventType.EXCHANGE,
                        "换型出库", "辅具 " + repl.getSerialNo() + " 替换 " + unit.getSerialNo() + "。" + note,
                        op.getName());
                o.setDeviceUnitId(repl.getId());
                rentalRepo.save(o);
            }
            case REASSESS -> {
                Assessment a = new Assessment();
                a.setElderlyId(o.getElderlyId());
                a.setFromFeedbackId(f.getId());
                if (o.getAssessmentId() != null) {
                    assessmentRepo.findById(o.getAssessmentId()).ifPresent(old -> {
                        a.setAssessorId(old.getAssessorId());
                        a.setAssessorName(old.getAssessorName());
                    });
                }
                a.setNotes("反馈触发的再次评估：" + note);
                assessmentRepo.save(a);
                eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.REASSESS,
                        "发起再次评估", note, op.getName());
            }
            case FIT_REVIEW -> {
                DeviceModel model = modelRepo.findById(o.getModelId()).orElseThrow();
                fitReviewService.create(o, unit, model, f.getId(), op.getName());
            }
            case NONE -> eventService.record(unit.getId(), o.getId(), o.getElderlyId(),
                    ServiceEventType.FEEDBACK, "反馈已答复", note, op.getName());
        }

        f.setResolution(resolution);
        f.setHandleNote(note);
        f.setStatus(FeedbackStatus.RESOLVED);
        f.setResolvedAt(LocalDateTime.now());
        feedbackRepo.save(f);
        return view(f);
    }

    static RiskLevel riskOf(FeedbackType t) {
        return RepairSchedulingService.riskOf(t);
    }

    static String typeLabel(FeedbackType t) {
        return switch (t) {
            case WEAR -> "磨损";
            case NOISE -> "异响";
            case SIZE_MISFIT -> "尺寸不适";
            case FALL -> "老人摔倒";
            case CANT_OPERATE -> "不会操作";
            case BRAKE_FAILURE -> "刹车失灵";
            case AIR_LEAK -> "气垫漏气";
            case OTHER -> "其他";
        };
    }

    private Map<String, Object> view(Feedback f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("feedback", f);
        elderlyRepo.findById(f.getElderlyId()).ifPresent(e -> m.put("elderlyName", e.getName()));
        unitRepo.findById(f.getDeviceUnitId()).ifPresent(u -> m.put("serialNo", u.getSerialNo()));
        rentalRepo.findById(f.getRentalOrderId()).ifPresent(o -> m.put("orderNo", o.getOrderNo()));
        return m;
    }
}
