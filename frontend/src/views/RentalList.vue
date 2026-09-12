<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(v, k) in rentalStatusMap" :key="k" :value="k">{{ v }}</el-radio-button>
        </el-radio-group>
        <el-button v-if="isStaff" type="primary" :icon="Plus" @click="openCreate">新建租赁单</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="order.orderNo" label="订单号" width="170" />
        <el-table-column prop="elderlyName" label="老人" width="90" />
        <el-table-column label="辅具" min-width="180">
          <template #default="{ row }">
            <el-tag size="small" style="margin-right: 4px">{{ categoryMap[row.category] }}</el-tag>
            {{ row.modelName }}（{{ row.serialNo }}）
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="rentalStatusTag[row.order.status]" size="small">{{ rentalStatusMap[row.order.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="押金/月租" width="120">
          <template #default="{ row }">¥{{ row.order.depositAmount }} / ¥{{ row.order.monthlyRent }}</template>
        </el-table-column>
        <el-table-column label="起租日期" width="105">
          <template #default="{ row }">{{ fmtDate(row.order.startDate) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.order.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openDetail(row)">办理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建租赁 -->
    <el-dialog v-model="createDlg" title="新建租赁单" width="480px">
      <el-form label-width="90px">
        <el-form-item label="老人">
          <el-select v-model="createForm.elderlyId" filterable style="width: 100%" @change="onElderlyChange">
            <el-option v-for="e in elderlyList" :key="e.id" :label="`${e.name}（${e.community}）`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联评估">
          <el-select v-model="createForm.assessmentId" clearable style="width: 100%" @change="onAssessmentChange">
            <el-option v-for="a in elderlyAssessments" :key="a.assessment.id"
                     :label="`${fmtDate(a.assessment.visitDate)} 建议：${categoryMap[a.assessment.recommendedCategory] || '-'}`"
                     :value="a.assessment.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="辅具型号">
          <el-select v-model="createForm.modelId" style="width: 100%">
            <el-option v-for="m in models" :key="m.model.id" :value="m.model.id"
                       :label="`${m.model.name}（押金¥${m.model.depositAmount} 月租¥${m.model.monthlyRent} 可租${m.available}件）`"
                       :disabled="m.available === 0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDlg = false">取消</el-button>
        <el-button type="primary" @click="create">创建并锁定库存</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情/办理 -->
    <el-drawer v-model="detailDlg" :title="`租赁订单 · ${detail?.order?.orderNo || ''}`" size="620px">
      <template v-if="detail">
        <el-steps :active="stepActive" align-center finish-status="success" style="margin-bottom: 18px">
          <el-step title="创建锁库" />
          <el-step title="家属确认" />
          <el-step title="配送" />
          <el-step title="安装起租" />
          <el-step :title="detail.order.status === 'CANCELLED' ? '已取消' : '结案回收'" />
        </el-steps>

        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="老人">{{ detail.elderlyName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="rentalStatusTag[detail.order.status]" size="small">{{ rentalStatusMap[detail.order.status] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="辅具">{{ detail.modelName }}（{{ detail.serialNo }}）</el-descriptions-item>
          <el-descriptions-item label="押金/月租">¥{{ detail.order.depositAmount }} / ¥{{ detail.order.monthlyRent }}</el-descriptions-item>
          <el-descriptions-item label="配送地址" :span="2">{{ detail.order.deliveryAddress }}</el-descriptions-item>
          <el-descriptions-item label="起租日期">{{ fmtDate(detail.order.startDate) }}</el-descriptions-item>
          <el-descriptions-item label="结案日期">{{ fmtDate(detail.order.endDate) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.order.closeReason" label="结案原因" :span="2">
            {{ closeReasonMap[detail.order.closeReason] }} {{ detail.order.closeNote ? '：' + detail.order.closeNote : '' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="detail.assessment" label="评估建议" :span="2">
            {{ detail.assessment.recommendationNote || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="actions">
          <el-button v-if="detail.order.status === 'PENDING_CONFIRM' && canConfirm" type="primary"
                     @click="act('confirm')">确认租赁（收押金）</el-button>
          <el-button v-if="detail.order.status === 'CONFIRMED' && canOps" type="primary"
                     @click="act('deliver')">配送上门</el-button>
          <el-button v-if="detail.order.status === 'DELIVERED' && canOps" type="success"
                     @click="act('install')">安装完成起租</el-button>
          <el-button v-if="['ACTIVE', 'DELIVERED', 'CONFIRMED'].includes(detail.order.status) && isStaff"
                     type="warning" @click="closeDlg = true">结案回收</el-button>
          <el-button v-if="['PENDING_CONFIRM', 'CONFIRMED'].includes(detail.order.status) && isStaff"
                     type="danger" plain @click="act('cancel')">取消订单</el-button>
        </div>

        <h4>费用明细（分账）</h4>
        <el-table :data="detail.payments" size="small" border>
          <el-table-column label="类型" width="90">
            <template #default="{ row }"><el-tag :type="paymentTypeTag[row.type]" size="small">{{ paymentTypeMap[row.type] }}</el-tag></template>
          </el-table-column>
          <el-table-column label="金额" width="90"><template #default="{ row }">¥{{ row.amount }}</template></el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }"><el-tag :type="paymentStatusTag[row.status]" size="small">{{ paymentStatusMap[row.status] }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="relatedAction" label="服务动作" min-width="160" />
          <el-table-column label="时间" width="140">
            <template #default="{ row }">{{ fmtTime(row.paidAt || row.createdAt) }}</template>
          </el-table-column>
        </el-table>

        <h4>服务事件</h4>
        <el-timeline style="padding-left: 4px">
          <el-timeline-item v-for="e in detail.events" :key="e.id" :timestamp="fmtTime(e.createdAt)">
            <el-tag :type="eventTypeTag[e.type]" size="small">{{ eventTypeMap[e.type] }}</el-tag>
            <b style="margin: 0 6px">{{ e.title }}</b>
            <span style="color: #606266; font-size: 13px">{{ e.detail }}</span>
          </el-timeline-item>
        </el-timeline>

        <template v-if="detail.feedback?.length">
          <h4>关联反馈</h4>
          <el-table :data="detail.feedback" size="small" border>
            <el-table-column label="类型" width="90">
              <template #default="{ row }"><el-tag size="small">{{ feedbackTypeMap[row.type] }}</el-tag></template>
            </el-table-column>
            <el-table-column label="风险" width="80">
              <template #default="{ row }"><el-tag :type="riskTag[row.riskLevel]" size="small">{{ riskMap[row.riskLevel] }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="180" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }"><el-tag :type="feedbackStatusTag[row.status]" size="small">{{ feedbackStatusMap[row.status] }}</el-tag></template>
            </el-table-column>
          </el-table>
        </template>
      </template>
    </el-drawer>

    <!-- 结案 -->
    <el-dialog v-model="closeDlg" title="租赁结案（辅具进入回收流程）" width="440px">
      <el-form label-width="90px">
        <el-form-item label="结案原因">
          <el-select v-model="closeForm.reason" style="width: 100%">
            <el-option v-for="(v, k) in closeReasonMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="closeForm.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <el-alert type="warning" :closable="false">
        结案后辅具将标记为「已回收待消毒」，并生成押金退还单；消毒质检合格后再上架，补贴随之核销。
      </el-alert>
      <template #footer>
        <el-button @click="closeDlg = false">取消</el-button>
        <el-button type="warning" @click="close">确认结案</el-button>
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
import {
  categoryMap, rentalStatusMap, rentalStatusTag, closeReasonMap,
  paymentTypeMap, paymentTypeTag, paymentStatusMap, paymentStatusTag,
  feedbackTypeMap, riskMap, riskTag, feedbackStatusMap, feedbackStatusTag,
  eventTypeMap, eventTypeTag, fmtDate, fmtTime
} from '../api/dicts'

const auth = useAuth()
const isStaff = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))
const canOps = computed(() => ['WAREHOUSE', 'ADMIN', 'STAFF'].includes(auth.role))

const rows = ref([])
const loading = ref(false)
const statusFilter = ref('')
const createDlg = ref(false)
const detailDlg = ref(false)
const closeDlg = ref(false)
const elderlyList = ref([])
const elderlyAssessments = ref([])
const models = ref([])
const detail = ref(null)
const createForm = ref({ elderlyId: null, modelId: null, assessmentId: null })
const closeForm = ref({ reason: 'NORMAL', note: '' })

const canConfirm = computed(() =>
  auth.isFamily || isStaff.value)

const stepActive = computed(() => {
  if (!detail.value) return 0
  const s = detail.value.order.status
  return { PENDING_CONFIRM: 1, CONFIRMED: 2, DELIVERED: 3, ACTIVE: 4, CLOSED: 5, CANCELLED: 5 }[s] ?? 0
})

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/rentals', { params: { status: statusFilter.value || undefined } })
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  elderlyList.value = await api.get('/elderly')
  models.value = await api.get('/devices/models')
  createForm.value = { elderlyId: null, modelId: null, assessmentId: null }
  elderlyAssessments.value = []
  createDlg.value = true
}

async function onElderlyChange(eid) {
  createForm.value.assessmentId = null
  if (!eid) {
    elderlyAssessments.value = []
    return
  }
  const all = await api.get('/assessments')
  elderlyAssessments.value = all.filter(a => a.assessment.elderlyId === eid && a.assessment.status === 'COMPLETED')
}

function onAssessmentChange(aid) {
  const a = elderlyAssessments.value.find(x => x.assessment.id === aid)
  if (a?.assessment?.recommendedModelId) {
    createForm.value.modelId = a.assessment.recommendedModelId
  }
}

async function create() {
  if (!createForm.value.elderlyId || !createForm.value.modelId) {
    ElMessage.warning('请选择老人与辅具型号')
    return
  }
  await api.post('/rentals', createForm.value)
  ElMessage.success('租赁单已创建并锁定库存，等待家属确认')
  createDlg.value = false
  load()
}

async function openDetail(row) {
  detail.value = await api.get(`/rentals/${row.order.id}`)
  detailDlg.value = true
}

async function act(action) {
  const id = detail.value.order.id
  const labels = { confirm: '确认租赁并收取押金？', deliver: '确认配送上门？', install: '确认安装完成并起租？', cancel: '确认取消该订单？' }
  await ElMessageBox.confirm(labels[action], '操作确认', { type: 'warning' })
  await api.post(`/rentals/${id}/${action}`)
  ElMessage.success('操作成功')
  detail.value = await api.get(`/rentals/${id}`)
  load()
}

async function close() {
  const id = detail.value.order.id
  await api.post(`/rentals/${id}/close`, closeForm.value)
  ElMessage.success('已结案，辅具进入回收流程')
  closeDlg.value = false
  detail.value = await api.get(`/rentals/${id}`)
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; flex-wrap: wrap; gap: 8px; }
.actions { margin: 16px 0; display: flex; gap: 8px; flex-wrap: wrap; }
h4 { margin: 14px 0 8px; }
</style>
