package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.*;
import com.community.assist.repo.*;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import com.community.assist.service.EventService;
import com.community.assist.service.FitReviewService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 尺寸不适复评：家属上传资料 → 评估师结论（操作问题/尺寸不合/病情变化）
 * → 培训 / 换型（重新确认门宽与照护人操作） / 暂停租赁。结论原因家属可见。
 */
@RestController
@RequestMapping("/api/fit-reviews")
public class FitReviewController {

    private final FitReviewRepository fitReviewRepo;
    private final RentalOrderRepository rentalRepo;
    private final ElderlyRepository elderlyRepo;
    private final DeviceUnitRepository unitRepo;
    private final DeviceModelRepository modelRepo;
    private final FitReviewService fitReviewService;
    private final EventService eventService;
    private final CurrentUser currentUser;

    public FitReviewController(FitReviewRepository fitReviewRepo, RentalOrderRepository rentalRepo,
                               ElderlyRepository elderlyRepo, DeviceUnitRepository unitRepo,
                               DeviceModelRepository modelRepo, FitReviewService fitReviewService,
                               EventService eventService, CurrentUser currentUser) {
        this.fitReviewRepo = fitReviewRepo;
        this.rentalRepo = rentalRepo;
        this.elderlyRepo = elderlyRepo;
        this.unitRepo = unitRepo;
        this.modelRepo = modelRepo;
        this.fitReviewService = fitReviewService;
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) FitReviewStatus status) {
        User u = currentUser.get();
        List<FitReview> src;
        if (u.getRole() == Role.FAMILY) {
            src = u.getElderlyId() == null ? List.of() : fitReviewRepo.findByElderlyIdOrderByIdDesc(u.getElderlyId());
        } else if (status != null) {
            src = fitReviewRepo.findByStatusOrderByIdDesc(status);
        } else {
            src = fitReviewRepo.findAllByOrderByIdDesc();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (FitReview r : src) {
            res.add(view(r));
        }
        return res;
    }

    @GetMapping("/{id}")
    public Map<String, Object> one(@PathVariable Long id) {
        FitReview r = fitReviewRepo.findById(id).orElseThrow(() -> BizException.notFound("复评单不存在"));
        currentUser.checkFamilyScope(currentUser.get(), r.getElderlyId());
        return view(r);
    }

    public record CreateReq(Long rentalOrderId, Long feedbackId) {
    }

    /** 社区工作人员对在租的护理床/轮椅发起尺寸复评 */
    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody CreateReq req) {
        User op = currentUser.requireAny(Role.STAFF, Role.ADMIN);
        RentalOrder o = rentalRepo.findById(req.rentalOrderId())
                .orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        if (o.getStatus() != RentalStatus.ACTIVE && o.getStatus() != RentalStatus.SUSPENDED) {
            throw BizException.badRequest("仅在租/已暂停订单可发起尺寸复评");
        }
        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        DeviceModel model = modelRepo.findById(o.getModelId()).orElseThrow();
        FitReview r = fitReviewService.create(o, unit, model, req.feedbackId(), op.getName());
        return view(r);
    }

    public record InfoReq(String photoUrls, Integer heightCm, Double weightKg,
                          Integer roomWidthCm, Integer roomLengthCm, String caregiverNote) {
    }

    /** 家属上传复评资料：使用照片、身高体重、房间尺寸、照护人说明（缺一拒绝，状态不流转） */
    @PostMapping("/{id}/info")
    @Transactional
    public Map<String, Object> submitInfo(@PathVariable Long id, @RequestBody InfoReq req) {
        User op = currentUser.get();
        FitReview r = fitReviewRepo.findById(id).orElseThrow(() -> BizException.notFound("复评单不存在"));
        currentUser.checkFamilyScope(op, r.getElderlyId());
        if (r.getStatus() != FitReviewStatus.PENDING_INFO) {
            throw BizException.badRequest("该复评单已提交过资料");
        }
        List<String> missing = new ArrayList<>();
        if (req.photoUrls() == null || req.photoUrls().isBlank()) {
            missing.add("使用照片");
        }
        if (req.heightCm() == null || req.heightCm() <= 0) {
            missing.add("身高");
        }
        if (req.weightKg() == null || req.weightKg() <= 0) {
            missing.add("体重");
        }
        if (req.roomWidthCm() == null || req.roomWidthCm() <= 0) {
            missing.add("房间宽度");
        }
        if (req.roomLengthCm() == null || req.roomLengthCm() <= 0) {
            missing.add("房间长度");
        }
        if (req.caregiverNote() == null || req.caregiverNote().isBlank()) {
            missing.add("照护人说明");
        }
        if (!missing.isEmpty()) {
            throw BizException.badRequest("复评资料不完整，缺少：" + String.join("、", missing)
                    + "。请补齐后再提交，复评结论需建立在完整资料上");
        }
        r.setPhotoUrls(req.photoUrls());
        r.setHeightCm(req.heightCm());
        r.setWeightKg(req.weightKg());
        r.setRoomWidthCm(req.roomWidthCm());
        r.setRoomLengthCm(req.roomLengthCm());
        r.setCaregiverNote(req.caregiverNote());
        r.setSubmittedAt(LocalDateTime.now());
        r.setStatus(FitReviewStatus.PENDING_REVIEW);
        fitReviewRepo.save(r);
        eventService.record(r.getDeviceUnitId(), r.getRentalOrderId(), r.getElderlyId(),
                ServiceEventType.FIT_REVIEW, "复评资料已提交",
                "身高 " + req.heightCm() + "cm，体重 " + req.weightKg() + "kg，待评估师复评", op.getName());
        return view(r);
    }

    public record ReviewReq(String conclusion, String action, String conclusionNote) {
    }

    /** 评估师复评：判断操作问题/尺寸不合/病情变化，决定培训/换型/暂停租赁 */
    @PostMapping("/{id}/review")
    @Transactional
    public Map<String, Object> review(@PathVariable Long id, @RequestBody ReviewReq req) {
        User op = currentUser.requireAny(Role.ASSESSOR, Role.ADMIN);
        FitReview r = fitReviewRepo.findById(id).orElseThrow(() -> BizException.notFound("复评单不存在"));
        if (r.getStatus() != FitReviewStatus.PENDING_REVIEW) {
            throw BizException.badRequest("家属尚未提交资料或复评已完成");
        }
        FitConclusion conclusion = FitConclusion.valueOf(req.conclusion());
        FitAction action = FitAction.valueOf(req.action());
        r.setConclusion(conclusion);
        r.setAction(action);
        r.setConclusionNote(req.conclusionNote());
        r.setAssessorId(op.getId());
        r.setAssessorName(op.getName());
        r.setReviewedAt(LocalDateTime.now());
        r.setStatus(action == FitAction.EXCHANGE ? FitReviewStatus.EXCHANGING : FitReviewStatus.COMPLETED);
        fitReviewRepo.save(r);

        RentalOrder o = rentalRepo.findById(r.getRentalOrderId()).orElseThrow();
        DeviceUnit unit = unitRepo.findById(r.getDeviceUnitId()).orElseThrow();
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.FIT_REVIEW,
                "复评结论：" + conclusionLabel(conclusion),
                "处置：" + actionLabel(action) + (req.conclusionNote() == null ? "" : "；" + req.conclusionNote()),
                op.getName());
        if (action == FitAction.TRAINING) {
            eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.TRAINING,
                    "操作培训", req.conclusionNote() == null ? "已安排照护人操作培训" : req.conclusionNote(), op.getName());
        }
        if (action == FitAction.SUSPEND) {
            o.setStatus(RentalStatus.SUSPENDED);
            rentalRepo.save(o);
            eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.SUSPEND,
                    "租赁暂停", "复评结论要求暂停租赁：" + (req.conclusionNote() == null ? "" : req.conclusionNote()),
                    op.getName());
        }
        return view(r);
    }

    public record ExchangeReq(Long newModelId, Integer doorWidthReconfirmedCm,
                              Boolean caregiverRetrained, String note) {
    }

    /** 执行换型：更换型号，并重新确认门宽与照护人操作 */
    @PostMapping("/{id}/exchange")
    @Transactional
    public Map<String, Object> exchange(@PathVariable Long id, @RequestBody ExchangeReq req) {
        User op = currentUser.requireAny(Role.STAFF, Role.ADMIN);
        FitReview r = fitReviewRepo.findById(id).orElseThrow(() -> BizException.notFound("复评单不存在"));
        if (r.getStatus() != FitReviewStatus.EXCHANGING) {
            throw BizException.badRequest("该复评单不在待换型状态");
        }
        if (req.doorWidthReconfirmedCm() == null) {
            throw BizException.badRequest("换型后必须重新确认门宽");
        }
        if (!Boolean.TRUE.equals(req.caregiverRetrained())) {
            throw BizException.badRequest("换型后必须重新确认照护人操作能力");
        }
        RentalOrder o = rentalRepo.findById(r.getRentalOrderId()).orElseThrow();
        DeviceModel oldModel = modelRepo.findById(o.getModelId()).orElseThrow();
        DeviceModel newModel = modelRepo.findById(req.newModelId())
                .orElseThrow(() -> BizException.notFound("新型号不存在"));
        if (newModel.getCategory() != oldModel.getCategory()) {
            throw BizException.badRequest("换型仅限同类别（" + oldModel.getCategory() + "）型号");
        }
        DeviceUnit oldUnit = unitRepo.findById(r.getDeviceUnitId()).orElseThrow();
        DeviceUnit newUnit = unitRepo
                .findFirstByModelIdAndStatusOrderByIdAsc(newModel.getId(), DeviceStatus.IN_STOCK)
                .orElseThrow(() -> BizException.badRequest("型号「" + newModel.getName() + "」暂无可用库存"));

        oldUnit.setStatus(DeviceStatus.RECALLED);
        unitRepo.save(oldUnit);
        eventService.record(oldUnit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.RECALL,
                "复评换型退回", "辅具 " + oldUnit.getSerialNo() + " 退回待消毒", op.getName());

        newUnit.setStatus(DeviceStatus.LEASED);
        unitRepo.save(newUnit);
        eventService.record(newUnit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.EXCHANGE,
                "复评换型出库",
                "辅具 " + newUnit.getSerialNo() + "（" + newModel.getName() + "）替换 " + oldUnit.getSerialNo()
                        + "；门宽复测 " + req.doorWidthReconfirmedCm() + "cm，照护人操作已重新确认",
                op.getName());

        o.setDeviceUnitId(newUnit.getId());
        o.setModelId(newModel.getId());
        o.setDepositAmount(newModel.getDepositAmount());
        o.setMonthlyRent(newModel.getMonthlyRent());
        // 换型后需重新做出库适配记录与家属确认
        o.setElderlyCondition(null);
        o.setFittingAdvice(null);
        o.setFamilyConfirmed(false);
        o.setFamilyConfirmTime(null);
        o.setFamilyConfirmNote(null);
        rentalRepo.save(o);

        r.setNewModelId(newModel.getId());
        r.setDoorWidthReconfirmedCm(req.doorWidthReconfirmedCm());
        r.setCaregiverRetrained(true);
        r.setExchangeNote(req.note());
        r.setExchangedAt(LocalDateTime.now());
        r.setDeviceUnitId(newUnit.getId());
        r.setStatus(FitReviewStatus.COMPLETED);
        fitReviewRepo.save(r);
        return view(r);
    }

    static String conclusionLabel(FitConclusion c) {
        return switch (c) {
            case OPERATION_ISSUE -> "操作问题";
            case SIZE_MISMATCH -> "尺寸不合";
            case CONDITION_CHANGE -> "病情变化";
        };
    }

    static String actionLabel(FitAction a) {
        return switch (a) {
            case TRAINING -> "培训";
            case EXCHANGE -> "换型";
            case SUSPEND -> "暂停租赁";
            case NONE -> "无需处理";
        };
    }

    private Map<String, Object> view(FitReview r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("review", r);
        elderlyRepo.findById(r.getElderlyId()).ifPresent(e -> m.put("elderlyName", e.getName()));
        unitRepo.findById(r.getDeviceUnitId()).ifPresent(u -> m.put("serialNo", u.getSerialNo()));
        rentalRepo.findById(r.getRentalOrderId()).ifPresent(o -> {
            m.put("orderNo", o.getOrderNo());
            modelRepo.findById(o.getModelId()).ifPresent(dm -> {
                m.put("modelName", dm.getName());
                m.put("category", dm.getCategory().name());
            });
        });
        if (r.getNewModelId() != null) {
            modelRepo.findById(r.getNewModelId()).ifPresent(dm -> m.put("newModelName", dm.getName()));
        }
        return m;
    }
}
