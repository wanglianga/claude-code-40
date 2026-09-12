<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待处理</el-radio-button>
          <el-radio-button value="RESOLVED">已解决</el-radio-button>
        </el-radio-group>
        <el-button v-if="auth.isFamily || isStaff" type="primary" :icon="Plus" @click="openSubmit">提交使用反馈</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="提交时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.feedback.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="elderlyName" label="老人" width="90" />
        <el-table-column prop="serialNo" label="辅具" width="110" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }"><el-tag size="small">{{ feedbackTypeMap[row.feedback.type] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="风险" width="90">
          <template #default="{ row }"><el-tag :type="riskTag[row.feedback.riskLevel]" size="small">{{ riskMap[row.feedback.riskLevel] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="feedback.description" label="问题描述" min-width="220" show-overflow-tooltip />
        <el-table-column prop="feedback.submitter" label="提交人" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="feedbackStatusTag[row.feedback.status]" size="small">{{ feedbackStatusMap[row.feedback.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处置" width="90">
          <template #default="{ row }">{{ row.feedback.resolution ? resolutionMap[row.feedback.resolution] : '-' }}</template>
        </el-table-column>
        <el-table-column prop="feedback.handleNote" label="处理说明" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="isStaff && row.feedback.status === 'PENDING'" size="small" type="primary"
                       @click="openHandle(row)">处置</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 提交反馈 -->
    <el-dialog v-model="submitDlg" title="提交使用反馈" width="480px">
      <el-form label-width="90px">
        <el-form-item label="租赁订单">
          <el-select v-model="submitForm.rentalOrderId" style="width: 100%">
            <el-option v-for="r in activeRentals" :key="r.order.id"
                       :label="`${r.modelName}（${r.serialNo}）${r.order.orderNo}`" :value="r.order.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题类型">
          <el-select v-model="submitForm.type" style="width: 100%">
            <el-option v-for="(v, k) in feedbackTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="submitForm.description" type="textarea" :rows="3"
                    placeholder="请描述磨损/异响/尺寸/操作等具体情况" />
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false">
        「老人摔倒」将自动标记为高风险并优先处理；平台会按风险生成维修、换型或再次评估。
      </el-alert>
      <template #footer>
        <el-button @click="submitDlg = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </template>
    </el-dialog>

    <!-- 处置 -->
    <el-dialog v-model="handleDlg" title="反馈处置（按风险生成后续动作）" width="480px">
      <template v-if="current">
        <el-alert :type="current.feedback.riskLevel === 'HIGH' ? 'error' : 'warning'" :closable="false" style="margin-bottom: 12px">
          {{ riskMap[current.feedback.riskLevel] }} · {{ feedbackTypeMap[current.feedback.type] }}：{{ current.feedback.description }}
        </el-alert>
        <el-form label-width="90px">
          <el-form-item label="处置方式">
            <el-radio-group v-model="handleForm.resolution">
              <el-radio value="REPAIR">维修（生成维修单）</el-radio>
              <el-radio value="EXCHANGE">换型（更换同型号库存）</el-radio>
              <el-radio value="FIT_REVIEW">尺寸复评（护理床/轮椅）</el-radio>
              <el-radio value="REASSESS">再次评估（生成评估单）</el-radio>
              <el-radio value="NONE">无需上门</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处理说明">
            <el-input v-model="handleForm.note" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="handleDlg = false">取消</el-button>
        <el-button type="primary" @click="handle">确认处置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'
import { useAuth } from '../store/auth'
import {
  feedbackTypeMap, riskMap, riskTag, feedbackStatusMap, feedbackStatusTag,
  resolutionMap, fmtTime
} from '../api/dicts'

const auth = useAuth()
const isStaff = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))
const rows = ref([])
const loading = ref(false)
const statusFilter = ref('')
const submitDlg = ref(false)
const handleDlg = ref(false)
const activeRentals = ref([])
const current = ref(null)
const submitForm = ref({ rentalOrderId: null, type: 'WEAR', description: '' })
const handleForm = ref({ resolution: 'REPAIR', note: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/feedback', { params: { status: statusFilter.value || undefined } })
  } finally {
    loading.value = false
  }
}

async function openSubmit() {
  const all = await api.get('/rentals')
  activeRentals.value = all.filter(r => ['ACTIVE', 'DELIVERED'].includes(r.order.status))
  if (!activeRentals.value.length) {
    ElMessage.warning('当前没有在租的辅具')
    return
  }
  submitForm.value = { rentalOrderId: activeRentals.value[0].order.id, type: 'WEAR', description: '' }
  submitDlg.value = true
}

async function submit() {
  if (!submitForm.value.description) {
    ElMessage.warning('请填写问题描述')
    return
  }
  await api.post('/feedback', submitForm.value)
  ElMessage.success('反馈已提交')
  submitDlg.value = false
  load()
}

function openHandle(row) {
  current.value = row
  handleForm.value = { resolution: row.feedback.riskLevel === 'LOW' ? 'REPAIR' : 'REPAIR', note: '' }
  handleDlg.value = true
}

async function handle() {
  await api.post(`/feedback/${current.value.feedback.id}/handle`, handleForm.value)
  ElMessage.success('已处置')
  handleDlg.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; }
</style>
