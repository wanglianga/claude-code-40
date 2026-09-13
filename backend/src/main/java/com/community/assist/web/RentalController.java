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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租赁订单全流程：建单锁库存 → 家属确认(押金) → 配送 → 安装起租 → 结案回收(退押金)。
 */
@RestController
@RequestMapping("/api/rentals")
public class RentalController {

    private final RentalOrderRepository rentalRepo;
    private final ElderlyRepository elderlyRepo;
    private final DeviceModelRepository modelRepo;
    private final DeviceUnitRepository unitRepo;
    private final AssessmentRepository assessmentRepo;
    private final PaymentRepository paymentRepo;
    private final FeedbackRepository feedbackRepo;
    private final ServiceEventRepository eventRepo;
    private final FitReviewRepository fitReviewRepo;
    private final EventService eventService;
    private final CurrentUser currentUser;

    public RentalController(RentalOrderRepository rentalRepo, ElderlyRepository elderlyRepo,
                            DeviceModelRepository modelRepo, DeviceUnitRepository unitRepo,
                            AssessmentRepository assessmentRepo, PaymentRepository paymentRepo,
                            FeedbackRepository feedbackRepo, ServiceEventRepository eventRepo,
                            FitReviewRepository fitReviewRepo,
                            EventService eventService, CurrentUser currentUser) {
        this.rentalRepo = rentalRepo;
        this.elderlyRepo = elderlyRepo;
        this.modelRepo = modelRepo;
        this.unitRepo = unitRepo;
        this.assessmentRepo = assessmentRepo;
        this.paymentRepo = paymentRepo;
        this.feedbackRepo = feedbackRepo;
        this.eventRepo = eventRepo;
        this.fitReviewRepo = fitReviewRepo;
        this.eventService = eventService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) RentalStatus status) {
        User u = currentUser.get();
        List<RentalOrder> src;
        if (u.getRole() == Role.FAMILY) {
            src = u.getElderlyId() == null ? List.of() : rentalRepo.findByElderlyIdOrderByIdDesc(u.getElderlyId());
        } else if (status != null) {
            src = rentalRepo.findByStatusOrderByIdDesc(status);
        } else {
            src = rentalRepo.findAllByOrderByIdDesc();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (RentalOrder o : src) {
            res.add(brief(o));
        }
        return res;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        currentUser.checkFamilyScope(currentUser.get(), o.getElderlyId());
        Map<String, Object> m = brief(o);
        m.put("payments", paymentRepo.findByRentalOrderIdOrderByIdAsc(id));
        m.put("events", eventRepo.findByRentalOrderIdOrderByIdAsc(id));
        m.put("feedback", feedbackRepo.findByRentalOrderIdOrderByIdDesc(id));
        m.put("fitReviews", fitReviewRepo.findByRentalOrderIdOrderByIdDesc(id));
        if (o.getAssessmentId() != null) {
            assessmentRepo.findById(o.getAssessmentId()).ifPresent(a -> m.put("assessment", a));
        }
        return m;
    }

    public record CreateReq(Long elderlyId, Long modelId, Long assessmentId) {
    }

    /** 社区工作人员按评估建议建单，自动锁定一件在库辅具 */
    @PostMapping
    @Transactional
    public Map<String, Object> create(@RequestBody CreateReq req) {
        User op = currentUser.requireAny(Role.STAFF, Role.ADMIN);
        Elderly e = elderlyRepo.findById(req.elderlyId()).orElseThrow(() -> BizException.notFound("老人档案不存在"));
        DeviceModel model = modelRepo.findById(req.modelId()).orElseThrow(() -> BizException.notFound("辅具型号不存在"));
        DeviceUnit unit = unitRepo.findFirstByModelIdAndStatusOrderByIdAsc(model.getId(), DeviceStatus.IN_STOCK)
                .orElseThrow(() -> BizException.badRequest("型号「" + model.getName() + "」暂无可用库存"));

        unit.setStatus(DeviceStatus.RESERVED);
        unitRepo.save(unit);

        RentalOrder o = new RentalOrder();
        o.setOrderNo(genNo("R"));
        o.setElderlyId(e.getId());
        o.setAssessmentId(req.assessmentId());
        o.setDeviceUnitId(unit.getId());
        o.setModelId(model.getId());
        o.setStatus(RentalStatus.PENDING_CONFIRM);
        o.setDepositAmount(model.getDepositAmount());
        o.setMonthlyRent(model.getMonthlyRent());
        o.setDeliveryAddress(e.getAddress());
        rentalRepo.save(o);

        Payment deposit = newPayment(e.getId(), o.getId(), unit.getId(), PaymentType.DEPOSIT,
                PaymentDirection.INCOME, model.getDepositAmount(), "租赁创建-收取押金");
        paymentRepo.save(deposit);

        eventService.record(unit.getId(), o.getId(), e.getId(), ServiceEventType.CREATE, "租赁订单创建",
                "为老人「" + e.getName() + "」锁定辅具 " + unit.getSerialNo()
                        + "（" + model.getName() + "），待家属确认", op.getName());
        return detail(o.getId());
    }

    /** 家属确认租赁 → 押金到账 */
    @PostMapping("/{id}/confirm")
    @Transactional
    public Map<String, Object> confirm(@PathVariable Long id) {
        User op = currentUser.get();
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        currentUser.checkFamilyScope(op, o.getElderlyId());
        if (o.getStatus() != RentalStatus.PENDING_CONFIRM) {
            throw BizException.badRequest("当前状态不可确认");
        }
        o.setStatus(RentalStatus.CONFIRMED);
        o.setConfirmTime(LocalDateTime.now());
        rentalRepo.save(o);
        paymentRepo.findFirstByRentalOrderIdAndTypeOrderByIdAsc(id, PaymentType.DEPOSIT).ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.PENDING) {
                p.setStatus(PaymentStatus.PAID);
                p.setPaidAt(LocalDateTime.now());
                p.setMethod("线上支付");
                p.setRelatedAction("家属确认租赁-押金已收");
                paymentRepo.save(p);
            }
        });
        return detail(id);
    }

    /** 仓储配送上门 */
    @PostMapping("/{id}/deliver")
    @Transactional
    public Map<String, Object> deliver(@PathVariable Long id) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN, Role.STAFF);
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        if (o.getStatus() != RentalStatus.CONFIRMED) {
            throw BizException.badRequest("仅已确认的订单可配送");
        }
        o.setStatus(RentalStatus.DELIVERED);
        o.setDeliveryTime(LocalDateTime.now());
        rentalRepo.save(o);
        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        unit.setStatus(DeviceStatus.LEASED);
        unitRepo.save(unit);
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.DELIVERY,
                "辅具配送上门", "辅具 " + unit.getSerialNo() + " 已配送至 " + o.getDeliveryAddress(), op.getName());
        return detail(id);
    }

    /** 安装调试完成，正式起租并生成首月租金 */
    @PostMapping("/{id}/install")
    @Transactional
    public Map<String, Object> install(@PathVariable Long id) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN, Role.STAFF);
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        if (o.getStatus() != RentalStatus.DELIVERED) {
            throw BizException.badRequest("仅已配送的订单可安装起租");
        }
        o.setStatus(RentalStatus.ACTIVE);
        o.setInstallTime(LocalDateTime.now());
        o.setStartDate(LocalDate.now());
        rentalRepo.save(o);
        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.INSTALL,
                "安装调试完成", "辅具 " + unit.getSerialNo() + " 安装调试完成，正式起租，月租金 ¥" + o.getMonthlyRent(),
                op.getName());
        Payment rent = newPayment(o.getElderlyId(), o.getId(), unit.getId(), PaymentType.RENT,
                PaymentDirection.INCOME, o.getMonthlyRent(),
                "首月租金 " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        paymentRepo.save(rent);
        return detail(id);
    }

    public record HandoverReq(String elderlyCondition, String fittingAdvice) {
    }

    /** 出库适配记录：登记老人身体状况与适配建议（缺一拒绝；维修时据此判断是否误用） */
    @PostMapping("/{id}/handover")
    @Transactional
    public Map<String, Object> handover(@PathVariable Long id, @RequestBody HandoverReq req) {
        User op = currentUser.requireAny(Role.WAREHOUSE, Role.ADMIN, Role.STAFF);
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        if (o.getStatus() != RentalStatus.ACTIVE && o.getStatus() != RentalStatus.DELIVERED) {
            throw BizException.badRequest("仅在租/已配送订单可登记出库适配");
        }
        List<String> missing = new ArrayList<>();
        if (req.elderlyCondition() == null || req.elderlyCondition().isBlank()) {
            missing.add("老人身体状况");
        }
        if (req.fittingAdvice() == null || req.fittingAdvice().isBlank()) {
            missing.add("适配建议");
        }
        if (!missing.isEmpty()) {
            throw BizException.badRequest("出库适配记录不完整，缺少：" + String.join("、", missing)
                    + "。该记录是维修误用判定的依据，请补齐后再保存");
        }
        o.setElderlyCondition(req.elderlyCondition());
        o.setFittingAdvice(req.fittingAdvice());
        o.setFamilyConfirmed(false);
        o.setFamilyConfirmTime(null);
        o.setFamilyConfirmNote(null);
        rentalRepo.save(o);
        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.HANDOVER,
                "出库适配记录", "老人身体状况：" + req.elderlyCondition()
                        + (req.fittingAdvice() == null ? "" : "；适配建议：" + req.fittingAdvice()), op.getName());
        return detail(id);
    }

    public record HandoverConfirmReq(String note) {
    }

    /** 家属确认出库适配记录 */
    @PostMapping("/{id}/handover-confirm")
    @Transactional
    public Map<String, Object> handoverConfirm(@PathVariable Long id,
                                               @RequestBody(required = false) HandoverConfirmReq req) {
        User op = currentUser.get();
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        currentUser.checkFamilyScope(op, o.getElderlyId());
        if (o.getElderlyCondition() == null) {
            throw BizException.badRequest("尚未登记出库适配记录");
        }
        if (Boolean.TRUE.equals(o.getFamilyConfirmed())) {
            throw BizException.badRequest("家属已确认过");
        }
        o.setFamilyConfirmed(true);
        o.setFamilyConfirmTime(LocalDateTime.now());
        o.setFamilyConfirmNote(req == null ? null : req.note());
        rentalRepo.save(o);
        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.HANDOVER,
                "家属确认出库适配", req == null || req.note() == null ? "家属已确认适配记录" : req.note(), op.getName());
        return detail(id);
    }

    /** 恢复租赁（复评暂停后） */
    @PostMapping("/{id}/resume")
    @Transactional
    public Map<String, Object> resume(@PathVariable Long id) {
        User op = currentUser.requireAny(Role.STAFF, Role.ADMIN);
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        if (o.getStatus() != RentalStatus.SUSPENDED) {
            throw BizException.badRequest("仅已暂停的订单可恢复");
        }
        o.setStatus(RentalStatus.ACTIVE);
        rentalRepo.save(o);
        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.RESUME,
                "恢复租赁", "复评问题已处理，恢复租赁", op.getName());
        return detail(id);
    }

    public record CloseReq(String reason, String note) {
    }

    /** 结案：住院/去世/搬家/补贴变化/损坏/到期 → 辅具进入回收流程，生成押金退还 */
    @PostMapping("/{id}/close")
    @Transactional
    public Map<String, Object> close(@PathVariable Long id, @RequestBody CloseReq req) {
        User op = currentUser.requireAny(Role.STAFF, Role.ADMIN);
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        if (o.getStatus() != RentalStatus.ACTIVE && o.getStatus() != RentalStatus.DELIVERED
                && o.getStatus() != RentalStatus.CONFIRMED && o.getStatus() != RentalStatus.SUSPENDED) {
            throw BizException.badRequest("当前状态不可结案");
        }
        CloseReason reason = req.reason() == null ? CloseReason.NORMAL : CloseReason.valueOf(req.reason());
        o.setStatus(RentalStatus.CLOSED);
        o.setCloseReason(reason);
        o.setCloseNote(req.note());
        o.setEndDate(LocalDate.now());
        o.setClosedAt(LocalDateTime.now());
        rentalRepo.save(o);

        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        unit.setStatus(DeviceStatus.RECALLED);
        unitRepo.save(unit);
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.RECALL,
                "租赁结案-辅具回收", "结案原因：" + reasonLabel(reason)
                        + (req.note() == null || req.note().isBlank() ? "" : "；" + req.note())
                        + "。辅具 " + unit.getSerialNo() + " 回收待消毒", op.getName());

        Payment refund = newPayment(o.getElderlyId(), o.getId(), unit.getId(), PaymentType.DEPOSIT_REFUND,
                PaymentDirection.EXPENSE, o.getDepositAmount(), "租赁结案-退还押金");
        paymentRepo.save(refund);
        return detail(id);
    }

    /** 取消未配送的订单，辅具回库 */
    @PostMapping("/{id}/cancel")
    @Transactional
    public Map<String, Object> cancel(@PathVariable Long id) {
        User op = currentUser.requireAny(Role.STAFF, Role.ADMIN);
        RentalOrder o = rentalRepo.findById(id).orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        if (o.getStatus() != RentalStatus.PENDING_CONFIRM && o.getStatus() != RentalStatus.CONFIRMED) {
            throw BizException.badRequest("仅待确认/已确认订单可取消");
        }
        o.setStatus(RentalStatus.CANCELLED);
        rentalRepo.save(o);
        DeviceUnit unit = unitRepo.findById(o.getDeviceUnitId()).orElseThrow();
        unit.setStatus(DeviceStatus.IN_STOCK);
        unitRepo.save(unit);
        paymentRepo.findFirstByRentalOrderIdAndTypeOrderByIdAsc(id, PaymentType.DEPOSIT).ifPresent(p -> {
            if (p.getStatus() == PaymentStatus.PAID) {
                Payment refund = newPayment(o.getElderlyId(), o.getId(), unit.getId(), PaymentType.DEPOSIT_REFUND,
                        PaymentDirection.EXPENSE, p.getAmount(), "订单取消-退还押金");
                paymentRepo.save(refund);
            } else {
                p.setStatus(PaymentStatus.REFUNDED);
                p.setRemark("订单取消，押金无需支付");
                paymentRepo.save(p);
            }
        });
        eventService.record(unit.getId(), o.getId(), o.getElderlyId(), ServiceEventType.RESTOCK,
                "订单取消-辅具回库", "辅具 " + unit.getSerialNo() + " 解除锁定重新上架", op.getName());
        return detail(id);
    }

    static Payment newPayment(Long elderlyId, Long rentalOrderId, Long deviceUnitId, PaymentType type,
                              PaymentDirection direction, BigDecimal amount, String relatedAction) {
        Payment p = new Payment();
        p.setPaymentNo(genNo("P"));
        p.setElderlyId(elderlyId);
        p.setRentalOrderId(rentalOrderId);
        p.setDeviceUnitId(deviceUnitId);
        p.setType(type);
        p.setDirection(direction);
        p.setAmount(amount);
        p.setRelatedAction(relatedAction);
        return p;
    }

    static String genNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + (int) (Math.random() * 900 + 100);
    }

    static String reasonLabel(CloseReason r) {
        return switch (r) {
            case NORMAL -> "租期结束";
            case HOSPITALIZED -> "老人住院";
            case DECEASED -> "老人去世";
            case MOVED -> "老人搬离";
            case SUBSIDY_CHANGE -> "补贴资格变化";
            case DAMAGED -> "辅具损坏";
        };
    }

    private Map<String, Object> brief(RentalOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", o);
        elderlyRepo.findById(o.getElderlyId()).ifPresent(e -> m.put("elderlyName", e.getName()));
        modelRepo.findById(o.getModelId()).ifPresent(dm -> {
            m.put("modelName", dm.getName());
            m.put("category", dm.getCategory().name());
        });
        unitRepo.findById(o.getDeviceUnitId()).ifPresent(u -> m.put("serialNo", u.getSerialNo()));
        return m;
    }
}
