package com.community.assist.web;

import com.community.assist.model.Enums.*;
import com.community.assist.model.Payment;
import com.community.assist.repo.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ElderlyRepository elderlyRepo;
    private final DeviceUnitRepository unitRepo;
    private final RentalOrderRepository rentalRepo;
    private final FeedbackRepository feedbackRepo;
    private final SubsidyRepository subsidyRepo;
    private final PaymentRepository paymentRepo;
    private final ServiceEventRepository eventRepo;
    private final RepairOrderRepository repairRepo;

    public DashboardController(ElderlyRepository elderlyRepo, DeviceUnitRepository unitRepo,
                               RentalOrderRepository rentalRepo, FeedbackRepository feedbackRepo,
                               SubsidyRepository subsidyRepo, PaymentRepository paymentRepo,
                               ServiceEventRepository eventRepo, RepairOrderRepository repairRepo) {
        this.elderlyRepo = elderlyRepo;
        this.unitRepo = unitRepo;
        this.rentalRepo = rentalRepo;
        this.feedbackRepo = feedbackRepo;
        this.subsidyRepo = subsidyRepo;
        this.paymentRepo = paymentRepo;
        this.eventRepo = eventRepo;
        this.repairRepo = repairRepo;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("elderlyCount", elderlyRepo.count());
        m.put("activeRentals", rentalRepo.countByStatus(RentalStatus.ACTIVE));
        m.put("pendingConfirmRentals", rentalRepo.countByStatus(RentalStatus.PENDING_CONFIRM));
        m.put("pendingFeedback", feedbackRepo.countByStatus(FeedbackStatus.PENDING));
        m.put("pendingSubsidies", subsidyRepo.countByStatus(SubsidyStatus.PENDING));
        m.put("pendingRepairs", repairRepo.countByStatus(RepairStatus.PENDING)
                + repairRepo.countByStatus(RepairStatus.IN_PROGRESS));

        Map<String, Long> unitStatus = new LinkedHashMap<>();
        for (DeviceStatus s : DeviceStatus.values()) {
            unitStatus.put(s.name(), unitRepo.countByStatus(s));
        }
        m.put("unitStatus", unitStatus);

        // 本月收支（分账：租金/押金/补贴/维修费）
        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        BigDecimal rentIncome = BigDecimal.ZERO, depositIncome = BigDecimal.ZERO,
                subsidyIncome = BigDecimal.ZERO, repairExpense = BigDecimal.ZERO;
        List<Payment> payments = paymentRepo.findAllByOrderByIdDesc();
        for (Payment p : payments) {
            if (p.getPaidAt() == null || p.getPaidAt().isBefore(monthStart)) {
                continue;
            }
            switch (p.getType()) {
                case RENT -> rentIncome = rentIncome.add(p.getAmount());
                case DEPOSIT -> depositIncome = depositIncome.add(p.getAmount());
                case SUBSIDY -> subsidyIncome = subsidyIncome.add(p.getAmount());
                case REPAIR -> repairExpense = repairExpense.add(p.getAmount());
                default -> {
                }
            }
        }
        Map<String, BigDecimal> month = new LinkedHashMap<>();
        month.put("rentIncome", rentIncome);
        month.put("depositIncome", depositIncome);
        month.put("subsidyIncome", subsidyIncome);
        month.put("repairExpense", repairExpense);
        m.put("monthFinance", month);

        m.put("recentEvents", eventRepo.findTop10ByOrderByIdDesc());
        return m;
    }
}
