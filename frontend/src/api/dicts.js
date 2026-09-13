/** 枚举中文字典与标签颜色 */
export const roleMap = { ADMIN: '管理员', STAFF: '社区工作人员', ASSESSOR: '评估师', FAMILY: '家属', WAREHOUSE: '仓储运维' }

export const disabilityMap = { MILD: '轻度失能', MODERATE: '中度失能', SEVERE: '重度失能', TOTAL: '完全失能' }
export const disabilityTag = { MILD: 'success', MODERATE: 'warning', SEVERE: 'danger', TOTAL: 'info' }

export const elderlyStatusMap = { ACTIVE: '在住', HOSPITALIZED: '住院', DECEASED: '去世', MOVED: '搬离' }
export const elderlyStatusTag = { ACTIVE: 'success', HOSPITALIZED: 'warning', DECEASED: 'info', MOVED: 'info' }

export const categoryMap = { WHEELCHAIR: '轮椅', WALKER: '助行器', ANTI_BEDSORE_PAD: '防褥疮垫', NURSING_BED: '护理床', BATH_CHAIR: '洗浴椅' }

export const deviceStatusMap = {
  IN_STOCK: '在库', RESERVED: '已锁定', LEASED: '租赁中', MAINTENANCE: '维修中',
  RECALLED: '已回收待消毒', DISINFECTING: '消毒质检中', SCRAPPED: '已报废'
}
export const deviceStatusTag = {
  IN_STOCK: 'success', RESERVED: 'warning', LEASED: 'primary', MAINTENANCE: 'danger',
  RECALLED: 'warning', DISINFECTING: 'warning', SCRAPPED: 'info'
}

export const rentalStatusMap = {
  PENDING_CONFIRM: '待家属确认', CONFIRMED: '已确认待配送', DELIVERED: '已配送待安装',
  ACTIVE: '租赁中', SUSPENDED: '已暂停', CLOSED: '已结案', CANCELLED: '已取消'
}
export const rentalStatusTag = {
  PENDING_CONFIRM: 'warning', CONFIRMED: 'primary', DELIVERED: 'primary',
  ACTIVE: 'success', SUSPENDED: 'warning', CLOSED: 'info', CANCELLED: 'info'
}

export const closeReasonMap = {
  NORMAL: '租期结束', HOSPITALIZED: '老人住院', DECEASED: '老人去世',
  MOVED: '老人搬离', SUBSIDY_CHANGE: '补贴资格变化', DAMAGED: '辅具损坏'
}

export const abilityMap = { GOOD: '良好', FAIR: '一般', POOR: '较弱' }

export const assessmentStatusMap = { PENDING: '待评估', COMPLETED: '已完成' }
export const assessmentStatusTag = { PENDING: 'warning', COMPLETED: 'success' }

export const feedbackTypeMap = {
  WEAR: '磨损', NOISE: '异响', SIZE_MISFIT: '尺寸不适',
  FALL: '老人摔倒', CANT_OPERATE: '不会操作', BRAKE_FAILURE: '刹车失灵', AIR_LEAK: '气垫漏气', OTHER: '其他'
}
export const riskMap = { LOW: '低风险', MEDIUM: '中风险', HIGH: '高风险' }
export const riskTag = { LOW: 'success', MEDIUM: 'warning', HIGH: 'danger' }
export const feedbackStatusMap = { PENDING: '待处理', RESOLVED: '已解决' }
export const feedbackStatusTag = { PENDING: 'danger', RESOLVED: 'success' }
export const resolutionMap = { NONE: '无需上门', REPAIR: '维修', EXCHANGE: '换型', REASSESS: '再次评估', FIT_REVIEW: '尺寸复评' }

export const repairStatusMap = { PENDING: '待排程', SCHEDULED: '已排程', IN_PROGRESS: '维修中', DONE: '已完成' }
export const repairStatusTag = { PENDING: 'warning', SCHEDULED: 'primary', IN_PROGRESS: 'primary', DONE: 'success' }

export const repairPriorityMap = { URGENT: '紧急', HIGH: '优先', NORMAL: '常规' }
export const repairPriorityTag = { URGENT: 'danger', HIGH: 'warning', NORMAL: 'info' }

export const visitResultMap = { NORMAL: '试用正常', ABNORMAL: '仍有问题' }

export const inspectionResultMap = { NORMAL_WEAR: '正常磨损', PARTS_MISSING: '配件缺失', MISUSE: '护理员操作问题' }
export const inspectionResultTag = { NORMAL_WEAR: 'success', PARTS_MISSING: 'warning', MISUSE: 'danger' }

export const paymentTypeMap = { DEPOSIT: '押金', RENT: '租金', SUBSIDY: '补贴', REPAIR: '维修费', DEPOSIT_REFUND: '押金退还', DEDUCTION: '检测扣款' }
export const paymentTypeTag = { DEPOSIT: 'warning', RENT: 'primary', SUBSIDY: 'success', REPAIR: 'danger', DEPOSIT_REFUND: 'info', DEDUCTION: 'danger' }
export const paymentStatusMap = { PENDING: '待支付', PAID: '已支付', REFUNDED: '已退还' }
export const paymentStatusTag = { PENDING: 'warning', PAID: 'success', REFUNDED: 'info' }
export const directionMap = { INCOME: '收入', EXPENSE: '支出' }

export const subsidyStatusMap = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', WRITTEN_OFF: '已核销' }
export const subsidyStatusTag = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', WRITTEN_OFF: 'info' }

export const eventTypeMap = {
  CREATE: '订单创建', DELIVERY: '配送', INSTALL: '安装', HANDOVER: '出库适配', FEEDBACK: '反馈',
  REPAIR: '维修', EXCHANGE: '换型', REASSESS: '再次评估', FIT_REVIEW: '尺寸复评',
  TRAINING: '培训', SUSPEND: '暂停', RESUME: '恢复',
  RECALL: '回收', DISINFECT: '消毒', QC: '质检', RESTOCK: '再上架', SCRAP: '报废', CLOSE: '结案/核销'
}
export const eventTypeTag = {
  CREATE: 'primary', DELIVERY: 'primary', INSTALL: 'success', HANDOVER: 'success', FEEDBACK: 'warning',
  REPAIR: 'danger', EXCHANGE: 'warning', REASSESS: 'warning', FIT_REVIEW: 'warning',
  TRAINING: 'success', SUSPEND: 'warning', RESUME: 'success',
  RECALL: 'warning', DISINFECT: 'primary', QC: 'primary', RESTOCK: 'success', SCRAP: 'info', CLOSE: 'info'
}

export const fitReviewStatusMap = {
  PENDING_INFO: '待家属上传资料', PENDING_REVIEW: '待评估师复评', EXCHANGING: '待换型执行', COMPLETED: '已完成'
}
export const fitReviewStatusTag = {
  PENDING_INFO: 'warning', PENDING_REVIEW: 'primary', EXCHANGING: 'warning', COMPLETED: 'success'
}
export const fitConclusionMap = { OPERATION_ISSUE: '操作问题', SIZE_MISMATCH: '尺寸不合', CONDITION_CHANGE: '病情变化' }
export const fitConclusionTag = { OPERATION_ISSUE: 'warning', SIZE_MISMATCH: 'danger', CONDITION_CHANGE: 'info' }
export const fitActionMap = { TRAINING: '培训', EXCHANGE: '换型', SUSPEND: '暂停租赁', NONE: '无需处理' }
export const fitActionTag = { TRAINING: 'success', EXCHANGE: 'warning', SUSPEND: 'danger', NONE: 'info' }

export function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '-'
}
export function fmtDate(t) {
  return t ? String(t).slice(0, 10) : '-'
}
