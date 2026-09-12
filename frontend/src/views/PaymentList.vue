<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <el-select v-model="typeFilter" placeholder="费用类型" clearable style="width: 140px; margin-right: 8px" @change="load">
            <el-option v-for="(v, k) in paymentTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
          <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 130px" @change="load">
            <el-option v-for="(v, k) in paymentStatusMap" :key="k" :label="v" :value="k" />
          </el-select>
        </div>
        <el-tag type="info">押金 / 租金 / 补贴 / 维修费 分账核算</el-tag>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="payment.paymentNo" label="流水号" width="150" />
        <el-table-column prop="elderlyName" label="老人" width="90">
          <template #default="{ row }">{{ row.elderlyName || '—' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="paymentTypeTag[row.payment.type]" size="small">{{ paymentTypeMap[row.payment.type] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收支" width="70">
          <template #default="{ row }">{{ directionMap[row.payment.direction] }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }">
            <b :style="{ color: row.payment.direction === 'INCOME' ? '#67c23a' : '#f56c6c' }">¥{{ row.payment.amount }}</b>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="paymentStatusTag[row.payment.status]" size="small">{{ paymentStatusMap[row.payment.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联辅具" width="170">
          <template #default="{ row }">
            <span v-if="row.serialNo">
              {{ row.modelName }}（{{ row.serialNo }}）
              <el-tag :type="deviceStatusTag[row.unitStatus]" size="small" style="margin-left: 4px">{{ deviceStatusMap[row.unitStatus] }}</el-tag>
            </span><span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="payment.relatedAction" label="对应服务动作" min-width="170" show-overflow-tooltip />
        <el-table-column label="支付方式" width="100">
          <template #default="{ row }">{{ row.payment.method || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.payment.paidAt || row.payment.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canPay(row)" size="small" type="primary" @click="pay(row)">支付</el-button>
            <el-button v-if="canRefund(row)" size="small" type="warning" @click="refund(row)">办理退款</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import { useAuth } from '../store/auth'
import {
  paymentTypeMap, paymentTypeTag, paymentStatusMap, paymentStatusTag, directionMap,
  deviceStatusMap, deviceStatusTag, fmtTime
} from '../api/dicts'

const auth = useAuth()
const isStaff = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))
const rows = ref([])
const loading = ref(false)
const typeFilter = ref('')
const statusFilter = ref('')

function canPay(row) {
  return row.payment.status === 'PENDING' && row.payment.direction === 'INCOME'
    && (auth.isFamily || isStaff.value)
}

function canRefund(row) {
  return row.payment.status === 'PENDING' && row.payment.type === 'DEPOSIT_REFUND' && isStaff.value
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/payments', {
      params: { type: typeFilter.value || undefined, status: statusFilter.value || undefined }
    })
  } finally {
    loading.value = false
  }
}

async function pay(row) {
  await ElMessageBox.confirm(`确认支付 ¥${row.payment.amount}（${paymentTypeMap[row.payment.type]}）？`, '在线支付', { type: 'info' })
  await api.post(`/payments/${row.payment.id}/pay`, { method: '线上支付' })
  ElMessage.success('支付成功')
  load()
}

async function refund(row) {
  await ElMessageBox.confirm(`确认向家属退还押金 ¥${row.payment.amount}？`, '押金退还', { type: 'warning' })
  await api.post(`/payments/${row.payment.id}/refund`)
  ElMessage.success('退款已办理')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; align-items: center; }
</style>
