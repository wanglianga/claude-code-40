package com.community.assist.web;

import com.community.assist.model.*;
import com.community.assist.model.Enums.*;
import com.community.assist.repo.*;
import com.community.assist.service.BizException;
import com.community.assist.service.CurrentUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 补贴：申请 → 审核（到账抵扣） → 租赁结案且辅具回收质检后核销。
 */
@RestController
@RequestMapping("/api/subsidies")
public class SubsidyController {

    private final SubsidyRepository subsidyRepo;
    private final RentalOrderRepository rentalRepo;
    private final ElderlyRepository elderlyRepo;
    private final PaymentRepository paymentRepo;
    private final CurrentUser currentUser;

    public SubsidyController(SubsidyRepository subsidyRepo, RentalOrderRepository rentalRepo,
                             ElderlyRepository elderlyRepo, PaymentRepository paymentRepo,
                             CurrentUser currentUser) {
        this.subsidyRepo = subsidyRepo;
        this.rentalRepo = rentalRepo;
        this.elderlyRepo = elderlyRepo;
        this.paymentRepo = paymentRepo;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) SubsidyStatus status) {
        User u = currentUser.get();
        List<SubsidyApplication> src;
        if (u.getRole() == Role.FAMILY) {
            src = u.getElderlyId() == null ? List.of() : subsidyRepo.findByElderlyIdOrderByIdDesc(u.getElderlyId());
        } else if (status != null) {
            src = subsidyRepo.findByStatusOrderByIdDesc(status);
        } else {
            src = subsidyRepo.findAllByOrderByIdDesc();
        }
        List<Map<String, Object>> res = new ArrayList<>();
        for (SubsidyApplication s : src) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subsidy", s);
            elderlyRepo.findById(s.getElderlyId()).ifPresent(e -> m.put("elderlyName", e.getName()));
            rentalRepo.findById(s.getRentalOrderId()).ifPresent(o -> m.put("orderNo", o.getOrderNo()));
            res.add(m);
        }
        return res;
    }

    public record ApplyReq(Long rentalOrderId, BigDecimal amount, String applyReason) {
    }

    @PostMapping
    public Map<String, Object> apply(@RequestBody ApplyReq req) {
        currentUser.requireAny(Role.STAFF, Role.ADMIN);
        RentalOrder o = rentalRepo.findById(req.rentalOrderId())
                .orElseThrow(() -> BizException.notFound("租赁订单不存在"));
        Elderly e = elderlyRepo.findById(o.getElderlyId()).orElseThrow();
        if (!Boolean.TRUE.equals(e.getSubsidyEligible())) {
            throw BizException.badRequest("该老人不具备补贴资格，请先在档案中维护");
        }
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BizException.badRequest("补贴金额必须大于 0");
        }
        SubsidyApplication s = new SubsidyApplication();
        s.setElderlyId(e.getId());
        s.setRentalOrderId(o.getId());
        s.setAmount(req.amount());
        s.setApplyReason(req.applyReason());
        subsidyRepo.save(s);
        return Map.of("subsidy", s);
    }

    public record ReviewReq(Boolean approve, String reviewNote) {
    }

    /** 审核通过 → 生成补贴到账费用（与押金/租金分账） */
    @PostMapping("/{id}/review")
    @Transactional
    public Map<String, Object> review(@PathVariable Long id, @RequestBody ReviewReq req) {
        currentUser.requireAny(Role.ADMIN);
        SubsidyApplication s = subsidyRepo.findById(id).orElseThrow(() -> BizException.notFound("补贴申请不存在"));
        if (s.getStatus() != SubsidyStatus.PENDING) {
            throw BizException.badRequest("该申请已审核");
        }
        boolean approve = req.approve() != null && req.approve();
        s.setStatus(approve ? SubsidyStatus.APPROVED : SubsidyStatus.REJECTED);
        s.setReviewNote(req.reviewNote());
        s.setReviewedAt(LocalDateTime.now());
        subsidyRepo.save(s);
        if (approve) {
            RentalOrder o = rentalRepo.findById(s.getRentalOrderId()).orElseThrow();
            Payment p = RentalController.newPayment(s.getElderlyId(), s.getRentalOrderId(), o.getDeviceUnitId(),
                    PaymentType.SUBSIDY, PaymentDirection.INCOME, s.getAmount(), "补贴审核通过-到账抵扣");
            p.setStatus(PaymentStatus.PAID);
            p.setPaidAt(LocalDateTime.now());
            p.setRemark("补贴申请 #" + s.getId());
            paymentRepo.save(p);
        }
        return Map.of("subsidy", s);
    }

    /** 核销：要求租赁已结案（辅具回收质检后由系统自动核销，此处支持人工核销） */
    @PostMapping("/{id}/writeoff")
    @Transactional
    public Map<String, Object> writeOff(@PathVariable Long id) {
        currentUser.requireAny(Role.ADMIN, Role.STAFF);
        SubsidyApplication s = subsidyRepo.findById(id).orElseThrow(() -> BizException.notFound("补贴申请不存在"));
        if (s.getStatus() != SubsidyStatus.APPROVED) {
            throw BizException.badRequest("仅已通过的补贴可核销");
        }
        RentalOrder o = rentalRepo.findById(s.getRentalOrderId()).orElseThrow();
        if (o.getStatus() != RentalStatus.CLOSED) {
            throw BizException.badRequest("租赁结案后方可核销补贴");
        }
        s.setStatus(SubsidyStatus.WRITTEN_OFF);
        s.setWrittenOffAt(LocalDateTime.now());
        subsidyRepo.save(s);
        return Map.of("subsidy", s);
    }
}
