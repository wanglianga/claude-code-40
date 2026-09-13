package com.community.assist.service;

import com.community.assist.model.*;
import com.community.assist.model.Enums.*;
import com.community.assist.repo.RepairOrderRepository;
import com.community.assist.repo.SparePartRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 维修上门排程：按风险等级、老人是否独居、维修员距离、备件库存计算优先级与上门时间，
 * 并同步生成照护人临时安全措施与临时替代办法通知。
 */
@Service
public class RepairSchedulingService {

    /** 维修员池（姓名, 距离km），演示用静态数据 */
    private static final List<String[]> REPAIRMEN = List.of(
            new String[]{"张工", "2.3"},
            new String[]{"刘工", "5.1"},
            new String[]{"陈工", "8.7"});

    private final SparePartRepository sparePartRepo;
    private final RepairOrderRepository repairRepo;
    private final EventService eventService;

    public RepairSchedulingService(SparePartRepository sparePartRepo, RepairOrderRepository repairRepo,
                                   EventService eventService) {
        this.sparePartRepo = sparePartRepo;
        this.repairRepo = repairRepo;
        this.eventService = eventService;
    }

    /** 故障类型 → 所需备件编码（null 表示无需备件） */
    public static String partCodeOf(FeedbackType fault) {
        if (fault == null) {
            return null;
        }
        return switch (fault) {
            case BRAKE_FAILURE, FALL -> "BRK-01";
            case AIR_LEAK -> "PUMP-01";
            case NOISE -> "BRG-01";
            case WEAR -> "CUSHION-01";
            case CANT_OPERATE -> "REMOTE-01";
            default -> null;
        };
    }

    public static RiskLevel riskOf(FeedbackType t) {
        return switch (t) {
            case FALL, BRAKE_FAILURE -> RiskLevel.HIGH;
            case SIZE_MISFIT, CANT_OPERATE, AIR_LEAK -> RiskLevel.MEDIUM;
            default -> RiskLevel.LOW;
        };
    }

    public void schedule(RepairOrder ro, Elderly e, FeedbackType fault, String operatorName) {
        RiskLevel risk = riskOf(fault);
        boolean alone = (e.getLivingEnv() != null && e.getLivingEnv().contains("独居"))
                || e.getCaregiverName() == null || e.getCaregiverName().isBlank();

        // 维修员：取距离最近者
        String[] man = REPAIRMEN.get(0);
        double distance = Double.parseDouble(man[1]);

        // 备件库存
        SparePart part = null;
        String partCode = partCodeOf(fault);
        if (partCode != null) {
            part = sparePartRepo.findByPartCode(partCode).orElse(null);
        }
        boolean spareReady = part == null || part.getStock() > 0;

        // 优先级：高风险故障优先；独居加分；备件缺货降权
        int score = switch (risk) {
            case HIGH -> 100;
            case MEDIUM -> 50;
            default -> 10;
        };
        if (alone) {
            score += 30;
        }
        if (!spareReady) {
            score -= 20;
        }
        RepairPriority priority = risk == RiskLevel.HIGH
                ? RepairPriority.URGENT
                : (score >= 60 ? RepairPriority.HIGH : RepairPriority.NORMAL);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime when = switch (priority) {
            case URGENT -> now.plusHours(2);
            case HIGH -> now.plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
            case NORMAL -> now.plusDays(2).withHour(9).withMinute(0).withSecond(0).withNano(0);
        };

        StringBuilder reason = new StringBuilder();
        reason.append("风险").append(riskLabel(risk));
        if (alone) {
            reason.append("，老人独居");
        }
        reason.append("，维修员").append(man[0]).append("（").append(man[1]).append("km）");
        reason.append("，备件").append(part == null ? "无需" : part.getName() + (spareReady ? "有库存" : "缺货"));

        ro.setFaultType(fault);
        ro.setLivesAlone(alone);
        ro.setPriority(priority);
        ro.setPriorityReason(reason.toString());
        ro.setAssigneeName(man[0]);
        ro.setDistanceKm(distance);
        ro.setScheduledAt(when);
        ro.setSparePartName(part == null ? null : part.getName());
        ro.setSpareReady(spareReady);
        ro.setSafetyNotice(safetyOf(fault));
        ro.setAlternativeNotice(alternativeOf(fault));
        ro.setStatus(RepairStatus.SCHEDULED);
        repairRepo.save(ro);

        eventService.record(ro.getDeviceUnitId(), ro.getRentalOrderId(), e.getId(), ServiceEventType.REPAIR,
                "维修排程-" + priorityLabel(priority),
                reason + "；预计 " + when.toLocalDate() + " " + when.toLocalTime().withNano(0)
                        + " 上门；已通知照护人临时安全措施与替代办法",
                operatorName);
    }

    static String riskLabel(RiskLevel r) {
        return switch (r) {
            case HIGH -> "高";
            case MEDIUM -> "中";
            default -> "低";
        };
    }

    static String priorityLabel(RepairPriority p) {
        return switch (p) {
            case URGENT -> "紧急";
            case HIGH -> "优先";
            default -> "常规";
        };
    }

    static String safetyOf(FeedbackType fault) {
        if (fault == null) {
            return "请照看好老人，等待维修上门";
        }
        return switch (fault) {
            case BRAKE_FAILURE -> "刹车失灵高风险：请立即停用轮椅自行移动，老人起身转移需两人搀扶，夜间床边放置便椅";
            case FALL -> "防再跌倒：暂停老人独自使用辅具，转移时专人陪护，保持通道无障碍";
            case AIR_LEAK -> "气垫漏气：请每2小时协助老人翻身，暂停使用气垫并垫普通棉褥，防压疮";
            case NOISE -> "可继续小心使用，避免超重与颠簸路面，留意异响是否加重";
            case WEAR -> "注意磨损部位，避免刮伤老人皮肤";
            case SIZE_MISFIT -> "暂停长时间使用，关注老人坐姿与受压部位";
            case CANT_OPERATE -> "暂停老人独自操作，等待上门指导";
            default -> "请照看好老人，等待维修上门";
        };
    }

    static String alternativeOf(FeedbackType fault) {
        if (fault == null) {
            return "如有急需请联系社区协调备用辅具";
        }
        return switch (fault) {
            case BRAKE_FAILURE -> "临时替代：改用助行器短距离移动，或联系社区借用备用轮椅";
            case FALL -> "临时替代：卧床期间使用床栏，离床需专人陪护";
            case AIR_LEAK -> "临时替代：普通床垫+翻身枕，定时翻身防压疮";
            case NOISE -> "临时替代：如有备用助行器可暂用";
            case WEAR -> "临时替代：磨损处垫软布，等待上门";
            case SIZE_MISFIT -> "临时替代：垫枕调整坐姿，等待复评";
            case CANT_OPERATE -> "临时替代：由照护人手动辅助，等待培训";
            default -> "如有急需请联系社区协调备用辅具";
        };
    }
}
