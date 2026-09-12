<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="4" v-for="c in cards" :key="c.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-num" :style="{ color: c.color }">{{ c.value }}</div>
          <div class="stat-label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>辅具库存状态分布</template>
          <div v-for="(v, k) in stats.unitStatus" :key="k" class="status-row">
            <el-tag :type="deviceStatusTag[k]" size="small" style="width: 110px">{{ deviceStatusMap[k] }}</el-tag>
            <el-progress :percentage="unitPct(v)" :stroke-width="14" style="flex: 1"
                         :color="progressColor" :show-text="false" />
            <span class="status-num">{{ v }} 件</span>
          </div>
        </el-card>
        <el-card shadow="never" style="margin-top: 16px">
          <template #header>本月分账收支</template>
          <div class="finance-grid">
            <div class="fin-item"><span>租金收入</span><b class="income">¥{{ stats.monthFinance?.rentIncome ?? 0 }}</b></div>
            <div class="fin-item"><span>押金收入</span><b class="income">¥{{ stats.monthFinance?.depositIncome ?? 0 }}</b></div>
            <div class="fin-item"><span>补贴到账</span><b class="income">¥{{ stats.monthFinance?.subsidyIncome ?? 0 }}</b></div>
            <div class="fin-item"><span>维修支出</span><b class="expense">¥{{ stats.monthFinance?.repairExpense ?? 0 }}</b></div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card shadow="never">
          <template #header>最新服务动态（辅具生命周期事件）</template>
          <el-timeline style="padding-left: 4px">
            <el-timeline-item v-for="e in stats.recentEvents" :key="e.id"
                              :timestamp="fmtTime(e.createdAt)" :type="timelineType(e.type)">
              <el-tag :type="eventTypeTag[e.type]" size="small">{{ eventTypeMap[e.type] }}</el-tag>
              <b style="margin: 0 6px">{{ e.title }}</b>
              <span class="event-detail">{{ e.detail }}</span>
              <div class="event-op">经办：{{ e.operatorName || '系统' }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'
import { deviceStatusMap, deviceStatusTag, eventTypeMap, eventTypeTag, fmtTime } from '../api/dicts'

const loading = ref(false)
const stats = ref({ unitStatus: {}, monthFinance: {}, recentEvents: [] })

const cards = computed(() => [
  { label: '在册老人', value: stats.value.elderlyCount ?? '-', color: '#409eff' },
  { label: '在租订单', value: stats.value.activeRentals ?? '-', color: '#67c23a' },
  { label: '可租库存', value: stats.value.unitStatus?.IN_STOCK ?? '-', color: '#409eff' },
  { label: '待处理反馈', value: stats.value.pendingFeedback ?? '-', color: '#e6a23c' },
  { label: '待审核补贴', value: stats.value.pendingSubsidies ?? '-', color: '#e6a23c' },
  { label: '进行中维修', value: stats.value.pendingRepairs ?? '-', color: '#f56c6c' }
])

const totalUnits = computed(() =>
  Object.values(stats.value.unitStatus || {}).reduce((a, b) => a + Number(b), 0))

function unitPct(v) {
  return totalUnits.value ? Math.round((Number(v) / totalUnits.value) * 100) : 0
}

function progressColor() { return '#409eff' }

function timelineType(t) {
  return ['RESTOCK', 'INSTALL'].includes(t) ? 'success' : ['REPAIR', 'SCRAP'].includes(t) ? 'danger' : 'primary'
}

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await api.get('/dashboard/stats')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-card { text-align: center; }
.stat-num { font-size: 28px; font-weight: 700; }
.stat-label { color: #909399; margin-top: 6px; font-size: 13px; }
.status-row { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.status-num { width: 48px; text-align: right; color: #606266; font-size: 13px; }
.finance-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.fin-item { display: flex; justify-content: space-between; padding: 10px 14px; background: #f5f7fa; border-radius: 6px; }
.income { color: #67c23a; }
.expense { color: #f56c6c; }
.event-detail { color: #606266; font-size: 13px; }
.event-op { color: #909399; font-size: 12px; margin-top: 2px; }
</style>
