<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(v, k) in subsidyStatusMap" :key="k" :value="k">{{ v }}</el-radio-button>
        </el-radio-group>
        <el-button v-if="isStaff" type="primary" :icon="Plus" @click="openApply">申请补贴</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="subsidy.id" label="编号" width="70" />
        <el-table-column prop="elderlyName" label="老人" width="100" />
        <el-table-column prop="orderNo" label="租赁订单" width="170" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }"><b style="color: #67c23a">¥{{ row.subsidy.amount }}</b></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="subsidyStatusTag[row.subsidy.status]" size="small">{{ subsidyStatusMap[row.subsidy.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="subsidy.applyReason" label="申请事由" min-width="200" show-overflow-tooltip />
        <el-table-column prop="subsidy.reviewNote" label="审核意见" min-width="150" show-overflow-tooltip />
        <el-table-column label="申请时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.subsidy.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="auth.role === 'ADMIN' && row.subsidy.status === 'PENDING'">
              <el-button size="small" type="success" @click="review(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="review(row, false)">驳回</el-button>
            </template>
            <el-button v-if="isStaff && row.subsidy.status === 'APPROVED'" size="small" type="warning"
                       @click="writeOff(row)">核销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="applyDlg" title="申请补贴" width="460px">
      <el-form label-width="90px">
        <el-form-item label="租赁订单">
          <el-select v-model="applyForm.rentalOrderId" style="width: 100%">
            <el-option v-for="r in rentals" :key="r.order.id"
                       :label="`${r.elderlyName} · ${r.modelName} · ${r.order.orderNo}`" :value="r.order.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="补贴金额">
          <el-input-number v-model="applyForm.amount" :min="1" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="申请事由">
          <el-input v-model="applyForm.applyReason" type="textarea" :rows="2"
                    placeholder="如：长期护理保险辅具补贴（半年）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDlg = false">取消</el-button>
        <el-button type="primary" @click="apply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'
import { useAuth } from '../store/auth'
import { subsidyStatusMap, subsidyStatusTag, fmtTime } from '../api/dicts'

const auth = useAuth()
const isStaff = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))
const rows = ref([])
const loading = ref(false)
const statusFilter = ref('')
const applyDlg = ref(false)
const rentals = ref([])
const applyForm = ref({ rentalOrderId: null, amount: 100, applyReason: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/subsidies', { params: { status: statusFilter.value || undefined } })
  } finally {
    loading.value = false
  }
}

async function openApply() {
  const all = await api.get('/rentals')
  rentals.value = all.filter(r => !['CANCELLED'].includes(r.order.status))
  applyForm.value = { rentalOrderId: rentals.value[0]?.order.id ?? null, amount: 100, applyReason: '' }
  applyDlg.value = true
}

async function apply() {
  if (!applyForm.value.rentalOrderId) {
    ElMessage.warning('请选择租赁订单')
    return
  }
  await api.post('/subsidies', applyForm.value)
  ElMessage.success('补贴申请已提交')
  applyDlg.value = false
  load()
}

async function review(row, approve) {
  const { value } = await ElMessageBox.prompt(
    `确认${approve ? '通过' : '驳回'} ¥${row.subsidy.amount} 的补贴申请？`, '审核意见',
    { confirmButtonText: '确定', cancelButtonText: '取消', inputPlaceholder: '审核意见（可选）' })
  await api.post(`/subsidies/${row.subsidy.id}/review`, { approve, reviewNote: value || '' })
  ElMessage.success('已审核')
  load()
}

async function writeOff(row) {
  await ElMessageBox.confirm('核销要求租赁已结案。确认核销该笔补贴？', '补贴核销', { type: 'warning' })
  await api.post(`/subsidies/${row.subsidy.id}/writeoff`)
  ElMessage.success('已核销')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; flex-wrap: wrap; gap: 8px; }
</style>
