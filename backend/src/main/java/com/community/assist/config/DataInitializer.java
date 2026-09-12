package com.community.assist.config;

import com.community.assist.model.*;
import com.community.assist.model.Enums.*;
import com.community.assist.repo.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 首次启动写入演示数据：账号、辅具库存、老人档案、评估、租赁、反馈、维修、费用、补贴与生命周期事件。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final ElderlyRepository elderlyRepo;
    private final AssessmentRepository assessmentRepo;
    private final DeviceModelRepository modelRepo;
    private final DeviceUnitRepository unitRepo;
    private final RentalOrderRepository rentalRepo;
    private final FeedbackRepository feedbackRepo;
    private final RepairOrderRepository repairRepo;
    private final PaymentRepository paymentRepo;
    private final SubsidyRepository subsidyRepo;
    private final ServiceEventRepository eventRepo;
    private final FitReviewRepository fitReviewRepo;
    private final PasswordEncoder encoder;

    private int paySeq = 1;

    public DataInitializer(UserRepository userRepo, ElderlyRepository elderlyRepo,
                           AssessmentRepository assessmentRepo, DeviceModelRepository modelRepo,
                           DeviceUnitRepository unitRepo, RentalOrderRepository rentalRepo,
                           FeedbackRepository feedbackRepo, RepairOrderRepository repairRepo,
                           PaymentRepository paymentRepo, SubsidyRepository subsidyRepo,
                           ServiceEventRepository eventRepo, FitReviewRepository fitReviewRepo,
                           PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.elderlyRepo = elderlyRepo;
        this.assessmentRepo = assessmentRepo;
        this.modelRepo = modelRepo;
        this.unitRepo = unitRepo;
        this.rentalRepo = rentalRepo;
        this.feedbackRepo = feedbackRepo;
        this.repairRepo = repairRepo;
        this.paymentRepo = paymentRepo;
        this.subsidyRepo = subsidyRepo;
        this.eventRepo = eventRepo;
        this.fitReviewRepo = fitReviewRepo;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepo.count() == 0) {
            seedAll();
        }
        seedFitDemo();
    }

    /** 首轮全量演示数据 */
    private void seedAll() {
        LocalDate today = LocalDate.now();

        // ---------- 账号 ----------
        user("admin", "admin123", "系统管理员", Role.ADMIN, "13800000001", null);
        user("staff", "staff123", "王芳（社区工作人员）", Role.STAFF, "13800000002", null);
        user("assessor", "assess123", "李评估（评估师）", Role.ASSESSOR, "13800000003", null);
        user("warehouse", "ware123", "赵仓储（仓储运维）", Role.WAREHOUSE, "13800000004", null);

        // ---------- 老人档案 ----------
        Elderly e1 = elderly("张桂兰", "女", 82, "110101194401011221", "13911110001", "朝阳社区",
                "朝阳区幸福里小区 3 栋 2 单元 301", DisabilityLevel.SEVERE,
                "电梯房 3 楼，两室一厅，卫生间有扶手", "张强", "儿子", "13911110002",
                "城镇职工医保", true, "长期护理保险", "WHEELCHAIR,NURSING_BED,ANTI_BEDSORE_PAD",
                ElderlyStatus.ACTIVE, "重度失能，长期卧床，夜间需定时翻身");
        Elderly e2 = elderly("刘建国", "男", 76, "110101195001013356", "13911110003", "朝阳社区",
                "朝阳区幸福里小区 7 栋 1 单元 202", DisabilityLevel.MODERATE,
                "无电梯 2 楼，一室一厅，门宽偏窄", "刘敏", "女儿", "13911110004",
                "城乡居民医保", true, "民政辅具补贴", "WALKER,BATH_CHAIR",
                ElderlyStatus.ACTIVE, "中度失能，可短距离扶行");
        Elderly e3 = elderly("陈秀珍", "女", 88, "110101193601017782", "13911110005", "和平社区",
                "和平区安康胡同 12 号院 1 门 101", DisabilityLevel.TOTAL,
                "平房，无电梯，卫生间狭小", "陈立", "儿子", "13911110006",
                "城镇职工医保", true, "民政辅具补贴", "BATH_CHAIR,NURSING_BED",
                ElderlyStatus.DECEASED, "2026 年 6 月去世，租赁已结案");
        Elderly e4 = elderly("王德福", "男", 79, "110101194701014490", "13911110007", "和平社区",
                "和平区安康胡同 3 号院 2 门 401", DisabilityLevel.MILD,
                "无电梯 4 楼，独居", "王磊", "孙子", "13911110008",
                "城乡居民医保", false, null, "WHEELCHAIR",
                ElderlyStatus.ACTIVE, "轻度失能，外出需轮椅");

        user("family", "family123", "张强（张桂兰家属）", Role.FAMILY, "13911110002", e1.getId());
        user("family2", "family123", "刘敏（刘建国女儿）", Role.FAMILY, "13911110004", e2.getId());
        User assessor = userRepo.findByUsername("assessor").orElseThrow();

        // ---------- 辅具型号 ----------
        DeviceModel m1 = model("WZ-01", "电动轮椅", DeviceCategory.WHEELCHAIR,
                "可折叠电磁刹车，续航 20km，座宽 46cm", 1500, 120, "适合中重度失能、照护人力量不足的家庭");
        DeviceModel m2 = model("WZ-02", "手动轮椅", DeviceCategory.WHEELCHAIR,
                "铝合金车架，可折叠，座宽 44cm", 800, 60, "适合轻度失能、短距离出行");
        DeviceModel m3 = model("ZX-01", "四轮助行器", DeviceCategory.WALKER,
                "带座椅与手刹，承重 120kg", 300, 25, "适合可扶行的中度失能老人");
        DeviceModel m4 = model("FC-01", "防褥疮气垫", DeviceCategory.ANTI_BEDSORE_PAD,
                "交替充气式，含静音气泵，适配 90cm 床", 500, 40, "长期卧床老人必备");
        DeviceModel m5 = model("HL-01", "电动护理床", DeviceCategory.NURSING_BED,
                "三功能电动起背抬腿，含护栏与床垫", 2000, 180, "重度/完全失能长期卧床");
        DeviceModel m6 = model("XY-01", "洗浴椅", DeviceCategory.BATH_CHAIR,
                "防滑铝合金，高度可调，带靠背", 200, 15, "卫生间洗浴防跌倒");

        // ---------- 辅具库存 ----------
        DeviceUnit u1 = unit("WZ01-001", m1, DeviceStatus.MAINTENANCE, "刹车检修中", 0);
        DeviceUnit u2 = unit("WZ01-002", m1, DeviceStatus.IN_STOCK, "成色良好", 1);
        DeviceUnit u3 = unit("WZ01-003", m1, DeviceStatus.IN_STOCK, "全新", 0);
        DeviceUnit u4 = unit("WZ02-001", m2, DeviceStatus.IN_STOCK, "成色良好", 2);
        DeviceUnit u5 = unit("WZ02-002", m2, DeviceStatus.RESERVED, "已锁定待确认", 1);
        DeviceUnit u6 = unit("ZX01-001", m3, DeviceStatus.LEASED, "在租", 1);
        DeviceUnit u7 = unit("ZX01-002", m3, DeviceStatus.IN_STOCK, "成色良好", 1);
        DeviceUnit u8 = unit("ZX01-003", m3, DeviceStatus.IN_STOCK, "前轮轴承已更换", 2);
        DeviceUnit u9 = unit("FC01-001", m4, DeviceStatus.IN_STOCK, "成色良好", 1);
        DeviceUnit u10 = unit("FC01-002", m4, DeviceStatus.IN_STOCK, "全新", 0);
        DeviceUnit u11 = unit("HL01-001", m5, DeviceStatus.LEASED, "在租", 1);
        DeviceUnit u12 = unit("HL01-002", m5, DeviceStatus.IN_STOCK, "成色良好", 1);
        DeviceUnit u13 = unit("XY01-001", m6, DeviceStatus.IN_STOCK, "消毒质检合格再上架", 1);
        DeviceUnit u14 = unit("XY01-002", m6, DeviceStatus.SCRAPPED, "支架开裂报废", 1);
        DeviceUnit u15 = unit("XY01-003", m6, DeviceStatus.DISINFECTING, "回收消毒质检中", 1);

        // ---------- 评估 ----------
        Assessment a1 = assessment(e1, assessor, today.minusMonths(4), 86, true, 3, 90, 150, 180,
                CaregiverAbility.FAIR, "老人长期卧床，起身后需轮椅移动；儿子白天上班，晚间照护。",
                DeviceCategory.WHEELCHAIR, m1.getId(), "门宽 86cm 满足电动轮椅通行，建议电动轮椅减轻照护负担。",
                AssessmentStatus.COMPLETED, today.minusMonths(4));
        Assessment a2 = assessment(e1, assessor, today.minusMonths(3).minusDays(5), 86, true, 3, 90, 150, 180,
                CaregiverAbility.FAIR, "复查：老人卧床时间增加，出现压疮风险。",
                DeviceCategory.NURSING_BED, m5.getId(), "建议电动护理床并搭配防褥疮气垫，床边空间 90cm 可摆放。",
                AssessmentStatus.COMPLETED, today.minusMonths(3).minusDays(5));
        Assessment a3 = assessment(e2, assessor, today.minusMonths(4).minusDays(10), 78, false, 2, 70, 120, 140,
                CaregiverAbility.GOOD, "老人可扶行，女儿同住。",
                DeviceCategory.WALKER, m3.getId(), "建议四轮助行器，门宽 78cm 可通过；无电梯注意上下楼安全。",
                AssessmentStatus.COMPLETED, today.minusMonths(4).minusDays(10));
        Assessment a4 = assessment(e3, assessor, today.minusMonths(7), 80, false, 1, 60, 110, 130,
                CaregiverAbility.POOR, "完全失能，儿子年岁也较大。",
                DeviceCategory.BATH_CHAIR, m6.getId(), "卫生间狭小，建议先配洗浴椅解决洗浴安全。",
                AssessmentStatus.COMPLETED, today.minusMonths(7));
        Assessment a5 = assessment(e4, assessor, today.minusDays(10), 82, false, 4, 80, 130, 150,
                CaregiverAbility.FAIR, "独居，孙子周末探望。",
                DeviceCategory.WHEELCHAIR, m2.getId(), "建议手动轮椅用于外出，无电梯 4 楼需家属协助搬运。",
                AssessmentStatus.COMPLETED, today.minusDays(10));
        assessment(e4, assessor, null, null, null, null, null, null, null,
                null, "复评任务：了解手动轮椅使用情况。", null, null, null,
                AssessmentStatus.PENDING, null);

        // ---------- 租赁 ----------
        RentalOrder r1 = rental(e1, a1, u1, m1, RentalStatus.ACTIVE, today.minusMonths(3).minusDays(11), null);
        RentalOrder r2 = rental(e1, a2, u11, m5, RentalStatus.ACTIVE, today.minusMonths(2).minusDays(11), null);
        RentalOrder r3 = rental(e2, a3, u6, m3, RentalStatus.ACTIVE, today.minusMonths(4), null);
        RentalOrder r4 = rental(e3, a4, u13, m6, RentalStatus.CLOSED, today.minusMonths(6).minusDays(11),
                today.minusMonths(3).minusDays(2));
        r4.setCloseReason(CloseReason.DECEASED);
        r4.setCloseNote("老人去世，家属联系社区结案");
        r4.setClosedAt(LocalDateTime.now().minusMonths(3).minusDays(2));
        rentalRepo.save(r4);
        RentalOrder r5 = rental(e4, a5, u5, m2, RentalStatus.PENDING_CONFIRM, null, null);
        RentalOrder r6 = rental(e2, null, u15, m6, RentalStatus.CLOSED, today.minusMonths(3),
                today.minusDays(23));
        r6.setCloseReason(CloseReason.NORMAL);
        r6.setCloseNote("短租洗浴椅到期归还");
        r6.setClosedAt(LocalDateTime.now().minusDays(23));
        rentalRepo.save(r6);

        // ---------- 生命周期事件 ----------
        String staff = "王芳（社区工作人员）";
        String ware = "赵仓储（仓储运维）";
        // u1 电动轮椅：在租 + 摔倒反馈 + 维修中
        ev(u1, r1, e1, ServiceEventType.CREATE, "租赁订单创建", "为老人「张桂兰」锁定辅具 WZ01-001（电动轮椅）", staff, monthsAgo(3, 12));
        ev(u1, r1, e1, ServiceEventType.DELIVERY, "辅具配送上门", "辅具 WZ01-001 已配送至 朝阳区幸福里小区 3 栋 2 单元 301", ware, monthsAgo(3, 11));
        ev(u1, r1, e1, ServiceEventType.INSTALL, "安装调试完成", "电动轮椅调试完成，正式起租，月租金 ¥120", ware, monthsAgo(3, 11));
        ev(u1, r1, e1, ServiceEventType.FEEDBACK, "使用反馈-老人摔倒", "风险等级 HIGH：老人夜间起身时轮椅滑动，险些摔倒", "张强", daysAgo(6));
        ev(u1, r1, e1, ServiceEventType.REPAIR, "生成维修单", "反馈触发维修：刹车系统检修", staff, daysAgo(5));
        // u11 护理床：在租
        ev(u11, r2, e1, ServiceEventType.CREATE, "租赁订单创建", "为老人「张桂兰」锁定辅具 HL01-001（电动护理床）", staff, monthsAgo(2, 12));
        ev(u11, r2, e1, ServiceEventType.DELIVERY, "辅具配送上门", "护理床配送上门", ware, monthsAgo(2, 11));
        ev(u11, r2, e1, ServiceEventType.INSTALL, "安装调试完成", "护理床安装完成，正式起租，月租金 ¥180", ware, monthsAgo(2, 11));
        ev(u11, r2, e1, ServiceEventType.FEEDBACK, "使用反馈-不会操作", "风险等级 MEDIUM：遥控器按键复杂，已上门指导", "张强", daysAgo(15));
        // u6 助行器：在租，异响待处理
        ev(u6, r3, e2, ServiceEventType.CREATE, "租赁订单创建", "为老人「刘建国」锁定辅具 ZX01-001（四轮助行器）", staff, monthsAgo(4, 1));
        ev(u6, r3, e2, ServiceEventType.DELIVERY, "辅具配送上门", "助行器配送上门", ware, monthsAgo(4, 0));
        ev(u6, r3, e2, ServiceEventType.INSTALL, "安装调试完成", "助行器调试完成，正式起租，月租金 ¥25", ware, monthsAgo(4, 0));
        ev(u6, r3, e2, ServiceEventType.FEEDBACK, "使用反馈-异响", "风险等级 LOW：右前轮转动异响，待处理", "刘敏", daysAgo(2));
        // u13 洗浴椅：完整生命周期（已结案→回收→消毒→质检→再上架→补贴核销）
        ev(u13, r4, e3, ServiceEventType.CREATE, "租赁订单创建", "为老人「陈秀珍」锁定辅具 XY01-001（洗浴椅）", staff, monthsAgo(6, 12));
        ev(u13, r4, e3, ServiceEventType.DELIVERY, "辅具配送上门", "洗浴椅配送上门", ware, monthsAgo(6, 11));
        ev(u13, r4, e3, ServiceEventType.INSTALL, "安装调试完成", "洗浴椅安装完成，正式起租，月租金 ¥15", ware, monthsAgo(6, 11));
        ev(u13, r4, e3, ServiceEventType.RECALL, "租赁结案-辅具回收", "结案原因：老人去世。辅具 XY01-001 回收待消毒", staff, monthsAgo(3, 2));
        ev(u13, r4, e3, ServiceEventType.DISINFECT, "开始消毒质检", "辅具 XY01-001 进入消毒质检流程", ware, monthsAgo(3, 1));
        ev(u13, r4, e3, ServiceEventType.QC, "消毒质检合格", "整机消毒，功能检测合格", ware, monthsAgo(3, 0));
        ev(u13, r4, e3, ServiceEventType.RESTOCK, "再次上架", "辅具 XY01-001 重新进入可租库存", ware, monthsAgo(3, 0));
        ev(u13, r4, e3, ServiceEventType.CLOSE, "补贴核销", "订单补贴 ¥90 已核销", "系统", monthsAgo(3, 0));
        // u15 洗浴椅：结案回收，消毒质检中
        ev(u15, r6, e2, ServiceEventType.CREATE, "租赁订单创建", "为老人「刘建国」锁定辅具 XY01-003（洗浴椅）", staff, monthsAgo(3, 1));
        ev(u15, r6, e2, ServiceEventType.DELIVERY, "辅具配送上门", "洗浴椅配送上门", ware, monthsAgo(3, 0));
        ev(u15, r6, e2, ServiceEventType.INSTALL, "安装调试完成", "洗浴椅安装完成，正式起租", ware, monthsAgo(3, 0));
        ev(u15, r6, e2, ServiceEventType.RECALL, "租赁结案-辅具回收", "结案原因：租期结束。辅具 XY01-003 回收待消毒", staff, daysAgo(23));
        ev(u15, r6, e2, ServiceEventType.DISINFECT, "开始消毒质检", "辅具 XY01-003 进入消毒质检流程", ware, daysAgo(22));
        // u5 手动轮椅：订单创建锁定
        ev(u5, r5, e4, ServiceEventType.CREATE, "租赁订单创建", "为老人「王德福」锁定辅具 WZ02-002（手动轮椅），待家属确认", staff, daysAgo(3));
        // u8 助行器：历史维修
        ev(u8, null, null, ServiceEventType.REPAIR, "维修完成", "更换磨损前轮轴承，费用 ¥80", ware, monthsAgo(2, 5));
        // u14 洗浴椅：报废
        ev(u14, null, null, ServiceEventType.QC, "消毒质检不合格", "支架开裂，存在安全风险", ware, monthsAgo(1, 3));
        ev(u14, null, null, ServiceEventType.SCRAP, "报废出库", "辅具 XY01-002 不再投入租赁", ware, monthsAgo(1, 3));

        // ---------- 使用反馈 ----------
        Feedback f1 = feedback(r1, e1, u1, FeedbackType.FALL, RiskLevel.HIGH,
                "老人夜间起身时轮椅滑动，险些摔倒，请尽快检查刹车。", "张强",
                FeedbackStatus.RESOLVED, Resolution.REPAIR, "已安排上门检修刹车系统", daysAgo(6), daysAgo(5));
        feedback(r3, e2, u6, FeedbackType.NOISE, RiskLevel.LOW,
                "助行器右前轮转动时有异响，影响使用。", "刘敏",
                FeedbackStatus.PENDING, null, null, daysAgo(2), null);
        feedback(r2, e1, u11, FeedbackType.CANT_OPERATE, RiskLevel.MEDIUM,
                "护理床遥控器按键太复杂，照护人不会操作。", "张强",
                FeedbackStatus.RESOLVED, Resolution.NONE, "已电话指导并上门演示操作", daysAgo(15), daysAgo(14));

        // ---------- 维修 ----------
        RepairOrder ro1 = new RepairOrder();
        ro1.setRepairNo("RO" + today.minusDays(5).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "01");
        ro1.setDeviceUnitId(u1.getId());
        ro1.setFeedbackId(f1.getId());
        ro1.setRentalOrderId(r1.getId());
        ro1.setStatus(RepairStatus.IN_PROGRESS);
        ro1.setDescription("反馈触发维修：老人夜间起身时轮椅滑动，检修刹车系统");
        ro1.setCreatedAt(daysAgo(5));
        repairRepo.save(ro1);

        RepairOrder ro2 = new RepairOrder();
        ro2.setRepairNo("RO" + today.minusMonths(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + "0002");
        ro2.setDeviceUnitId(u8.getId());
        ro2.setStatus(RepairStatus.DONE);
        ro2.setDescription("助行器前轮轴承磨损更换");
        ro2.setCost(new BigDecimal("80"));
        ro2.setResultNote("已更换轴承，转动正常");
        ro2.setCreatedAt(monthsAgo(2, 6));
        ro2.setFinishedAt(monthsAgo(2, 5));
        repairRepo.save(ro2);

        // ---------- 费用流水（押金/租金/补贴/维修费 分账） ----------
        // R1 电动轮椅
        pay(e1, r1, u1, PaymentType.DEPOSIT, PaymentDirection.INCOME, 1500, PaymentStatus.PAID, "家属确认租赁-押金已收", monthsAgo(3, 12));
        rentPays(e1, r1, u1, 120, r1.getStartDate(), 3, true);
        // R2 护理床
        pay(e1, r2, u11, PaymentType.DEPOSIT, PaymentDirection.INCOME, 2000, PaymentStatus.PAID, "家属确认租赁-押金已收", monthsAgo(2, 12));
        rentPays(e1, r2, u11, 180, r2.getStartDate(), 2, true);
        // R3 助行器
        pay(e2, r3, u6, PaymentType.DEPOSIT, PaymentDirection.INCOME, 300, PaymentStatus.PAID, "家属确认租赁-押金已收", monthsAgo(4, 1));
        rentPays(e2, r3, u6, 25, r3.getStartDate(), 4, true);
        // R4 洗浴椅（已结案）
        pay(e3, r4, u13, PaymentType.DEPOSIT, PaymentDirection.INCOME, 200, PaymentStatus.PAID, "家属确认租赁-押金已收", monthsAgo(6, 12));
        rentPays(e3, r4, u13, 15, r4.getStartDate(), 3, false);
        pay(e3, r4, u13, PaymentType.DEPOSIT_REFUND, PaymentDirection.EXPENSE, 200, PaymentStatus.REFUNDED, "租赁结案-退还押金", monthsAgo(3, 2));
        // R5 手动轮椅（待确认）
        pay(e4, r5, u5, PaymentType.DEPOSIT, PaymentDirection.INCOME, 800, PaymentStatus.PENDING, "租赁创建-收取押金", null);
        // R6 洗浴椅短租（已结案，押金待退）
        pay(e2, r6, u15, PaymentType.DEPOSIT, PaymentDirection.INCOME, 200, PaymentStatus.PAID, "家属确认租赁-押金已收", monthsAgo(3, 1));
        rentPays(e2, r6, u15, 15, r6.getStartDate(), 2, false);
        pay(e2, r6, u15, PaymentType.DEPOSIT_REFUND, PaymentDirection.EXPENSE, 200, PaymentStatus.PENDING, "租赁结案-退还押金", null);
        // 历史维修费
        pay(null, null, u8, PaymentType.REPAIR, PaymentDirection.EXPENSE, 80, PaymentStatus.PAID, "维修完成-维修费用", monthsAgo(2, 5));

        // ---------- 补贴 ----------
        subsidy(e1, r1, 600, SubsidyStatus.APPROVED, "长期护理保险辅具补贴（半年）", "资格核验通过", monthsAgo(3, 10));
        subsidy(e2, r3, 150, SubsidyStatus.APPROVED, "民政辅具租赁补贴", "资格核验通过", monthsAgo(4, 0));
        subsidy(e3, r4, 90, SubsidyStatus.WRITTEN_OFF, "民政辅具租赁补贴", "租赁结案，辅具已回收再上架", monthsAgo(6, 10));
        subsidy(e1, r2, 800, SubsidyStatus.PENDING, "长期护理保险护理床补贴申请", null, null);

        // 补贴到账费用
        pay(e1, r1, u1, PaymentType.SUBSIDY, PaymentDirection.INCOME, 600, PaymentStatus.PAID, "补贴审核通过-到账抵扣", monthsAgo(3, 9));
        pay(e2, r3, u6, PaymentType.SUBSIDY, PaymentDirection.INCOME, 150, PaymentStatus.PAID, "补贴审核通过-到账抵扣", monthsAgo(3, 28));
        pay(e3, r4, u13, PaymentType.SUBSIDY, PaymentDirection.INCOME, 90, PaymentStatus.PAID, "补贴审核通过-到账抵扣", monthsAgo(6, 8));
    }

    /**
     * 尺寸复评演示数据（幂等：新老数据库均执行）。
     * 1) 为在租订单补齐出库适配记录；2) 一条已完成（培训）复评 + 一条待家属上传资料的复评。
     */
    private void seedFitDemo() {
        String ware = "赵仓储（仓储运维）";
        // 1) 在租订单出库适配记录
        for (RentalOrder o : rentalRepo.findByStatusOrderByIdDesc(RentalStatus.ACTIVE)) {
            if (o.getElderlyCondition() == null) {
                o.setElderlyCondition("意识清楚，生命体征平稳，可配合辅具适配");
                o.setFittingAdvice("按评估建议完成调试，家属已现场学习基本操作，定期复查适配情况");
                o.setFamilyConfirmed(true);
                o.setFamilyConfirmTime(o.getInstallTime() == null
                        ? LocalDateTime.now().minusDays(1) : o.getInstallTime().plusHours(1));
                rentalRepo.save(o);
                unitRepo.findById(o.getDeviceUnitId()).ifPresent(u -> {
                    ServiceEvent e = new ServiceEvent();
                    e.setDeviceUnitId(u.getId());
                    e.setRentalOrderId(o.getId());
                    e.setElderlyId(o.getElderlyId());
                    e.setType(ServiceEventType.HANDOVER);
                    e.setTitle("出库适配记录");
                    e.setDetail("老人身体状况已登记，适配建议已告知，家属确认完成");
                    e.setOperatorName(ware);
                    e.setCreatedAt(o.getInstallTime() == null
                            ? LocalDateTime.now().minusDays(1) : o.getInstallTime().plusHours(1));
                    eventRepo.save(e);
                });
            }
        }

        if (fitReviewRepo.count() > 0) {
            return;
        }
        Elderly e1 = elderlyRepo.findByNameContainingOrderByIdDesc("张桂兰").stream().findFirst().orElse(null);
        User assessor = userRepo.findByUsername("assessor").orElse(null);
        if (e1 == null || assessor == null) {
            return;
        }
        RentalOrder bedOrder = null, chairOrder = null;
        for (RentalOrder o : rentalRepo.findByElderlyIdOrderByIdDesc(e1.getId())) {
            if (o.getStatus() != RentalStatus.ACTIVE) {
                continue;
            }
            DeviceModel m = modelRepo.findById(o.getModelId()).orElse(null);
            if (m == null) {
                continue;
            }
            if (m.getCategory() == DeviceCategory.NURSING_BED && bedOrder == null) {
                bedOrder = o;
            }
            if (m.getCategory() == DeviceCategory.WHEELCHAIR && chairOrder == null) {
                chairOrder = o;
            }
        }
        // 已完成的复评：护理床 → 操作问题 → 培训
        if (bedOrder != null) {
            final RentalOrder bo = bedOrder;
            FitReview fr = new FitReview();
            fr.setReviewNo("FR20260901001");
            fr.setRentalOrderId(bo.getId());
            fr.setElderlyId(e1.getId());
            fr.setDeviceUnitId(bo.getDeviceUnitId());
            fr.setStatus(FitReviewStatus.COMPLETED);
            fr.setHeightCm(158);
            fr.setWeightKg(62.5);
            fr.setRoomWidthCm(320);
            fr.setRoomLengthCm(360);
            fr.setCaregiverNote("老人反映床沿太高，起身时压腿，担心摔倒");
            fr.setSubmittedAt(daysAgo(20));
            fr.setConclusion(FitConclusion.OPERATION_ISSUE);
            fr.setAction(FitAction.TRAINING);
            fr.setConclusionNote("床高与起背角度调节不当导致压腿，非尺寸问题；已现场培训照护人使用三功能遥控器并张贴操作卡");
            fr.setAssessorId(assessor.getId());
            fr.setAssessorName(assessor.getName());
            fr.setReviewedAt(daysAgo(18));
            fr.setCreatedAt(daysAgo(22));
            fitReviewRepo.save(fr);
            unitRepo.findById(bo.getDeviceUnitId()).ifPresent(u -> {
                ev(u, bo, e1, ServiceEventType.FIT_REVIEW, "发起尺寸复评", "家属反馈床沿压腿，发起复评", "王芳（社区工作人员）", daysAgo(22));
                ev(u, bo, e1, ServiceEventType.FIT_REVIEW, "复评结论：操作问题", "处置：培训；已现场培训照护人", assessor.getName(), daysAgo(18));
                ev(u, bo, e1, ServiceEventType.TRAINING, "操作培训", "三功能遥控器使用培训完成，张贴操作卡", assessor.getName(), daysAgo(18));
            });
        }
        // 待家属上传资料的复评：轮椅
        if (chairOrder != null) {
            final RentalOrder co = chairOrder;
            FitReview fr = new FitReview();
            fr.setReviewNo("FR20260910001");
            fr.setRentalOrderId(co.getId());
            fr.setElderlyId(e1.getId());
            fr.setDeviceUnitId(co.getDeviceUnitId());
            fr.setStatus(FitReviewStatus.PENDING_INFO);
            fr.setCreatedAt(daysAgo(1));
            fitReviewRepo.save(fr);
            unitRepo.findById(co.getDeviceUnitId()).ifPresent(u ->
                    ev(u, co, e1, ServiceEventType.FIT_REVIEW, "发起尺寸复评",
                            "家属反映轮椅久坐不适，待上传使用照片、身高体重、房间尺寸与照护人说明",
                            "王芳（社区工作人员）", daysAgo(1)));
        }
    }

    // ---------- 构造辅助 ----------

    private User user(String username, String password, String name, Role role, String phone, Long elderlyId) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setName(name);
        u.setRole(role);
        u.setPhone(phone);
        u.setElderlyId(elderlyId);
        return userRepo.save(u);
    }

    private Elderly elderly(String name, String gender, int age, String idCard, String phone, String community,
                            String address, DisabilityLevel level, String livingEnv, String caregiverName,
                            String caregiverRelation, String caregiverPhone, String insuranceType,
                            boolean subsidyEligible, String subsidyType, String neededDevices,
                            ElderlyStatus status, String remark) {
        Elderly e = new Elderly();
        e.setName(name);
        e.setGender(gender);
        e.setAge(age);
        e.setIdCard(idCard);
        e.setPhone(phone);
        e.setCommunity(community);
        e.setAddress(address);
        e.setDisabilityLevel(level);
        e.setLivingEnv(livingEnv);
        e.setCaregiverName(caregiverName);
        e.setCaregiverRelation(caregiverRelation);
        e.setCaregiverPhone(caregiverPhone);
        e.setInsuranceType(insuranceType);
        e.setSubsidyEligible(subsidyEligible);
        e.setSubsidyType(subsidyType);
        e.setNeededDevices(neededDevices);
        e.setStatus(status);
        e.setRemark(remark);
        return elderlyRepo.save(e);
    }

    private DeviceModel model(String code, String name, DeviceCategory category, String spec,
                              double deposit, double rent, String description) {
        DeviceModel m = new DeviceModel();
        m.setCode(code);
        m.setName(name);
        m.setCategory(category);
        m.setSpec(spec);
        m.setDepositAmount(BigDecimal.valueOf(deposit));
        m.setMonthlyRent(BigDecimal.valueOf(rent));
        m.setDescription(description);
        return modelRepo.save(m);
    }

    private DeviceUnit unit(String serialNo, DeviceModel model, DeviceStatus status, String conditionNote, int rentalCount) {
        DeviceUnit u = new DeviceUnit();
        u.setSerialNo(serialNo);
        u.setModelId(model.getId());
        u.setStatus(status);
        u.setConditionNote(conditionNote);
        u.setPurchaseDate(LocalDate.now().minusYears(1));
        u.setRentalCount(rentalCount);
        return unitRepo.save(u);
    }

    private Assessment assessment(Elderly e, User assessor, LocalDate visitDate, Integer doorWidth,
                                  Boolean hasElevator, Integer floor, Integer bedside, Integer bathW, Integer bathL,
                                  CaregiverAbility ability, String notes, DeviceCategory recCategory, Long recModelId,
                                  String recNote, AssessmentStatus status, LocalDate completedDate) {
        Assessment a = new Assessment();
        a.setElderlyId(e.getId());
        a.setAssessorId(assessor.getId());
        a.setAssessorName(assessor.getName());
        a.setVisitDate(visitDate);
        a.setDoorWidthCm(doorWidth);
        a.setHasElevator(hasElevator);
        a.setFloor(floor);
        a.setBedsideSpaceCm(bedside);
        a.setBathroomWidthCm(bathW);
        a.setBathroomLengthCm(bathL);
        a.setCaregiverAbility(ability);
        a.setNotes(notes);
        a.setRecommendedCategory(recCategory);
        a.setRecommendedModelId(recModelId);
        a.setRecommendationNote(recNote);
        a.setStatus(status);
        if (completedDate != null) {
            a.setCreatedAt(completedDate.atTime(9, 0));
            a.setCompletedAt(completedDate.atTime(11, 30));
        }
        return assessmentRepo.save(a);
    }

    private RentalOrder rental(Elderly e, Assessment a, DeviceUnit u, DeviceModel m, RentalStatus status,
                               LocalDate startDate, LocalDate endDate) {
        RentalOrder o = new RentalOrder();
        o.setOrderNo("R2026" + String.format("%04d", e.getId() * 10 + rentalRepo.count()));
        o.setElderlyId(e.getId());
        o.setAssessmentId(a == null ? null : a.getId());
        o.setDeviceUnitId(u.getId());
        o.setModelId(m.getId());
        o.setStatus(status);
        o.setDepositAmount(m.getDepositAmount());
        o.setMonthlyRent(m.getMonthlyRent());
        o.setDeliveryAddress(e.getAddress());
        o.setStartDate(startDate);
        o.setEndDate(endDate);
        if (startDate != null) {
            o.setConfirmTime(startDate.minusDays(1).atTime(10, 0));
            o.setDeliveryTime(startDate.atTime(9, 0));
            o.setInstallTime(startDate.atTime(10, 30));
        }
        return rentalRepo.save(o);
    }

    private Feedback feedback(RentalOrder o, Elderly e, DeviceUnit u, FeedbackType type, RiskLevel risk,
                              String description, String submitter, FeedbackStatus status, Resolution resolution,
                              String handleNote, LocalDateTime createdAt, LocalDateTime resolvedAt) {
        Feedback f = new Feedback();
        f.setRentalOrderId(o.getId());
        f.setElderlyId(e.getId());
        f.setDeviceUnitId(u.getId());
        f.setType(type);
        f.setRiskLevel(risk);
        f.setDescription(description);
        f.setSubmitter(submitter);
        f.setStatus(status);
        f.setResolution(resolution);
        f.setHandleNote(handleNote);
        f.setCreatedAt(createdAt);
        f.setResolvedAt(resolvedAt);
        return feedbackRepo.save(f);
    }

    private void subsidy(Elderly e, RentalOrder o, double amount, SubsidyStatus status,
                         String applyReason, String reviewNote, LocalDateTime reviewedAt) {
        SubsidyApplication s = new SubsidyApplication();
        s.setElderlyId(e.getId());
        s.setRentalOrderId(o.getId());
        s.setAmount(BigDecimal.valueOf(amount));
        s.setStatus(status);
        s.setApplyReason(applyReason);
        s.setReviewNote(reviewNote);
        s.setReviewedAt(reviewedAt);
        if (status == SubsidyStatus.WRITTEN_OFF) {
            s.setWrittenOffAt(LocalDateTime.now().minusMonths(3));
        }
        subsidyRepo.save(s);
    }

    /** 生成从起租月往后的租金流水：历史月份已缴；在租订单再补一条当月待缴 */
    private void rentPays(Elderly e, RentalOrder o, DeviceUnit u, double rent, LocalDate startDate,
                          int paidMonths, boolean withPending) {
        YearMonth start = YearMonth.from(startDate);
        for (int i = 0; i < paidMonths; i++) {
            YearMonth ym = start.plusMonths(i);
            pay(e, o, u, PaymentType.RENT, PaymentDirection.INCOME, rent, PaymentStatus.PAID,
                    "月租金 " + ym, ym.atDay(5).atTime(10, 0));
        }
        if (withPending) {
            YearMonth current = YearMonth.now();
            pay(e, o, u, PaymentType.RENT, PaymentDirection.INCOME, rent, PaymentStatus.PENDING,
                    "月租金 " + current, null);
        }
    }

    private void pay(Elderly e, RentalOrder o, DeviceUnit u, PaymentType type, PaymentDirection dir,
                     double amount, PaymentStatus status, String action, LocalDateTime paidAt) {
        Payment p = new Payment();
        p.setPaymentNo("P2026" + String.format("%06d", paySeq++));
        p.setElderlyId(e == null ? null : e.getId());
        p.setRentalOrderId(o == null ? null : o.getId());
        p.setDeviceUnitId(u == null ? null : u.getId());
        p.setType(type);
        p.setDirection(dir);
        p.setAmount(BigDecimal.valueOf(amount));
        p.setStatus(status);
        p.setRelatedAction(action);
        if (paidAt != null) {
            p.setCreatedAt(paidAt.minusHours(2));
            p.setPaidAt(paidAt);
            p.setMethod(status == PaymentStatus.REFUNDED ? "原路退回" : "线上支付");
        }
        paymentRepo.save(p);
    }

    private void ev(DeviceUnit u, RentalOrder o, Elderly e, ServiceEventType type, String title,
                    String detail, String operator, LocalDateTime at) {
        ServiceEvent ev = new ServiceEvent();
        ev.setDeviceUnitId(u.getId());
        ev.setRentalOrderId(o == null ? null : o.getId());
        ev.setElderlyId(e == null ? null : e.getId());
        ev.setType(type);
        ev.setTitle(title);
        ev.setDetail(detail);
        ev.setOperatorName(operator);
        ev.setCreatedAt(at);
        eventRepo.save(ev);
    }

    private static LocalDateTime monthsAgo(int months, int extraDays) {
        return LocalDateTime.now().minusMonths(months).minusDays(extraDays);
    }

    private static LocalDateTime daysAgo(int days) {
        return LocalDateTime.now().minusDays(days);
    }
}
