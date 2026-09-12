package com.community.assist.model;

/**
 * 平台全部枚举。中文标签由前端字典维护，后端仅存英文常量。
 */
public final class Enums {

    private Enums() {
    }

    /** 用户角色：管理员/社区工作人员/评估师/家属/仓储运维 */
    public enum Role {ADMIN, STAFF, ASSESSOR, FAMILY, WAREHOUSE}

    /** 失能等级 */
    public enum DisabilityLevel {MILD, MODERATE, SEVERE, TOTAL}

    /** 老人状态：在住/住院/去世/搬离 */
    public enum ElderlyStatus {ACTIVE, HOSPITALIZED, DECEASED, MOVED}

    /** 辅具类别 */
    public enum DeviceCategory {WHEELCHAIR, WALKER, ANTI_BEDSORE_PAD, NURSING_BED, BATH_CHAIR}

    /** 单件辅具状态 */
    public enum DeviceStatus {IN_STOCK, RESERVED, LEASED, MAINTENANCE, RECALLED, DISINFECTING, SCRAPPED}

    /** 租赁订单状态 */
    public enum RentalStatus {PENDING_CONFIRM, CONFIRMED, DELIVERED, ACTIVE, CLOSED, CANCELLED}

    /** 结案原因 */
    public enum CloseReason {NORMAL, HOSPITALIZED, DECEASED, MOVED, SUBSIDY_CHANGE, DAMAGED}

    /** 照护人操作能力 */
    public enum CaregiverAbility {GOOD, FAIR, POOR}

    /** 评估状态 */
    public enum AssessmentStatus {PENDING, COMPLETED}

    /** 使用反馈类型 */
    public enum FeedbackType {WEAR, NOISE, SIZE_MISFIT, FALL, CANT_OPERATE, OTHER}

    /** 风险等级 */
    public enum RiskLevel {LOW, MEDIUM, HIGH}

    /** 反馈处理状态 */
    public enum FeedbackStatus {PENDING, RESOLVED}

    /** 反馈处置方式 */
    public enum Resolution {NONE, REPAIR, EXCHANGE, REASSESS}

    /** 维修单状态 */
    public enum RepairStatus {PENDING, IN_PROGRESS, DONE}

    /** 辅具生命周期事件类型 */
    public enum ServiceEventType {CREATE, DELIVERY, INSTALL, FEEDBACK, REPAIR, EXCHANGE, REASSESS, RECALL, DISINFECT, QC, RESTOCK, SCRAP, CLOSE}

    /** 费用类型：押金/租金/补贴/维修费/押金退还 */
    public enum PaymentType {DEPOSIT, RENT, SUBSIDY, REPAIR, DEPOSIT_REFUND}

    /** 收支方向 */
    public enum PaymentDirection {INCOME, EXPENSE}

    /** 费用状态 */
    public enum PaymentStatus {PENDING, PAID, REFUNDED}

    /** 补贴申请状态 */
    public enum SubsidyStatus {PENDING, APPROVED, REJECTED, WRITTEN_OFF}
}
