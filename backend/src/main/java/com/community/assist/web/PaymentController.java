package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.PaymentStatus;
import com.community.assist.model.Enums.PaymentType;
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

/**
 * 费用中心：押金/租金/补贴/维修费分账查询，家属可在线支付，社区可办理押金退还。
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepo;
    private final ElderlyRepository elderlyRepo;
    private final DeviceUnitRepository unitRepo;
    private final DeviceModelRepository modelRepo;
    private final RentalOrderRepository rentalRepo;
    private final CurrentUser currentUser;

    public PaymentController(PaymentRepository paymentRepo, ElderlyRepository elderlyRepo,
                             DeviceUnitRepository unitRepo, DeviceModelRepository modelRepo,
                             RentalOrderRepository rentalRepo, CurrentUser currentUser) {
        this.paymentRepo = paymentRepo;
        this.elderlyRepo = elderlyRepo;
        this.unitRepo = unitRepo;
        this.modelRepo = modelRepo;
        this.rentalRepo = rentalRepo;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) Long elderlyId,
                                          @RequestParam(required = false) PaymentType type,
                                          @RequestParam(required = false) PaymentStatus status) {
        User u = currentUser.get();
        List<Payment> src;
        if (u.getRole() == Role.FAMILY) {
            src = u.getElderlyId() == null ? List.of() : paymentRepo.findByElderlyIdOrderByIdDesc(u.getElderlyId());
        } else if (elderlyId != null) {
            src = paymentRepo.findByElderlyIdOrderByIdDesc(elderlyId);
        } else if (status != null) {
            src = paymentRepo.findByStatusOrderByIdDesc(status);
        } else {
            src = paymentRepo.findAllByOrderByIdDesc();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (Payment p : src) {
            if (type != null && p.getType() != type) {
                continue;
            }
            res.add(view(p));
        }
        return res;
    }

    public record PayReq(String method) {
    }

    /** 家属/社区支付待缴费用（押金、租金） */
    @PostMapping("/{id}/pay")
    @Transactional
    public Map<String, Object> pay(@PathVariable Long id, @RequestBody(required = false) PayReq req) {
        User u = currentUser.get();
        Payment p = paymentRepo.findById(id).orElseThrow(() -> BizException.notFound("费用记录不存在"));
        currentUser.checkFamilyScope(u, p.getElderlyId());
        if (p.getStatus() != PaymentStatus.PENDING) {
            throw BizException.badRequest("该费用不在待支付状态");
        }
        p.setStatus(PaymentStatus.PAID);
        p.setPaidAt(LocalDateTime.now());
        p.setMethod(req != null && req.method() != null ? req.method() : "线上支付");
        return view(paymentRepo.save(p));
    }

    /** 社区办理押金退还 */
    @PostMapping("/{id}/refund")
    @Transactional
    public Map<String, Object> refund(@PathVariable Long id) {
        currentUser.requireAny(Role.STAFF, Role.ADMIN);
        Payment p = paymentRepo.findById(id).orElseThrow(() -> BizException.notFound("费用记录不存在"));
        if (p.getType() != PaymentType.DEPOSIT_REFUND) {
            throw BizException.badRequest("仅押金退还单可办理退款");
        }
        if (p.getStatus() != PaymentStatus.PENDING) {
            throw BizException.badRequest("该退款已办理");
        }
        p.setStatus(PaymentStatus.REFUNDED);
        p.setPaidAt(LocalDateTime.now());
        p.setMethod("原路退回");
        return view(paymentRepo.save(p));
    }

    private Map<String, Object> view(Payment p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("payment", p);
        if (p.getElderlyId() != null) {
            elderlyRepo.findById(p.getElderlyId()).ifPresent(e -> m.put("elderlyName", e.getName()));
        }
        if (p.getDeviceUnitId() != null) {
            unitRepo.findById(p.getDeviceUnitId()).ifPresent(u -> {
                m.put("serialNo", u.getSerialNo());
                m.put("unitStatus", u.getStatus().name());
                modelRepo.findById(u.getModelId()).ifPresent(dm -> m.put("modelName", dm.getName()));
            });
        }
        if (p.getRentalOrderId() != null) {
            rentalRepo.findById(p.getRentalOrderId()).ifPresent(o -> m.put("orderNo", o.getOrderNo()));
        }
        return m;
    }
}
