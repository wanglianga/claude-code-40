<template>
  <div v-loading="loading">
    <el-card shadow="never" v-if="d.elderly">
      <template #header>
        <div class="card-header">
          <span>老人档案 · {{ d.elderly.name }}</span>
          <div v-if="isStaff">
            <el-button size="small" type="primary" @click="assessDlg = true">发起入户评估</el-button>
            <el-button size="small" type="success" @click="openRent">发起租赁</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="姓名">{{ d.elderly.name }}</el-descriptions-item>
        <el-descriptions-item label="性别/年龄">{{ d.elderly.gender }} / {{ d.elderly.age }}岁</el-descriptions-item>
        <el-descriptions-item label="失能等级">
          <el-tag :type="disabilityTag[d.elderly.disabilityLevel]" size="small">{{ disabilityMap[d.elderly.disabilityLevel] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="elderlyStatusTag[d.elderly.status]" size="small">{{ elderlyStatusMap[d.elderly.status] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="社区">{{ d.elderly.community }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="3">{{ d.elderly.address }}</el-descriptions-item>
        <el-descriptions-item label="居住环境" :span="2">{{ d.elderly.livingEnv || '-' }}</el-descriptions-item>
        <el-descriptions-item label="照护人">{{ d.elderly.caregiverName }}（{{ d.elderly.caregiverRelation }}）{{ d.elderly.caregiverPhone }}</el-descriptions-item>
        <el-descriptions-item label="医保">{{ d.elderly.insuranceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="补贴资格">
          <el-tag :type="d.elderly.subsidyEligible ? 'success' : 'info'" size="small">
            {{ d.elderly.subsidyEligible ? d.elderly.subsidyType || '有' : '无' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="所需辅具" :span="2">
          <el-tag v-for="t in (d.elderly.neededDevices || '').split(',').filter(Boolean)" :key="t"
                  size="small" style="margin-right: 4px">{{ categoryMap[t] }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="4">{{ d.elderly.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" style="margin-top: 14px">
      <el-tabs v-model="tab">
        <el-tab-pane :label="`评估记录 (${d.assessments?.length || 0})`" name="assess">
          <el-table :data="d.assessments || []" size="small" stripe>
            <el-table-column label="评估日期" width="110"><template #default="{ row }">{{ fmtDate(row.visitDate) }}</template></el-table-column>
            <el-table-column prop="assessorName" label="评估师" width="130" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><el-tag :type="assessmentStatusTag[row.status]" size="small">{{ assessmentStatusMap[row.status] }}</el-tag></template>
            </el-table-column>
            <el-table-column label="门宽/电梯/床边/卫生间" width="220">
              <template #default="{ row }">
                <span v-if="row.status === 'COMPLETED'">
                  {{ row.doorWidthCm }}cm / {{ row.hasElevator ? '有电梯' : '无电梯(' + row.floor + '楼)' }} / 床边{{ row.bedsideSpaceCm }}cm / {{ row.bathroomWidthCm }}×{{ row.bathroomLengthCm }}cm
                </span><span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="照护人能力" width="100">
              <template #default="{ row }">{{ abilityMap[row.caregiverAbility] || '-' }}</template>
            </el-table-column>
            <el-table-column label="辅具建议" min-width="220">
              <template #default="{ row }">
                <span v-if="row.recommendedCategory">【{{ categoryMap[row.recommendedCategory] }}】{{ row.recommendationNote }}</span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`租赁记录 (${d.rentals?.length || 0})`" name="rentals">
          <el-table :data="d.rentals || []" size="small" stripe>
            <el-table-column prop="order.orderNo" label="订单号" width="170" />
            <el-table-column prop="modelName" label="辅具型号" width="110" />
            <el-table-column prop="serialNo" label="序列号" width="110" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><el-tag :type="rentalStatusTag[row.order.status]" size="small">{{ rentalStatusMap[row.order.status] }}</el-tag></template>
            </el-table-column>
            <el-table-column label="起租/结束" width="190">
              <template #default="{ row }">{{ fmtDate(row.order.startDate) }} ~ {{ fmtDate(row.order.endDate) }}</template>
            </el-table-column>
            <el-table-column label="押金/月租金" width="130">
              <template #default="{ row }">¥{{ row.order.depositAmount }} / ¥{{ row.order.monthlyRent }}</template>
            </el-table-column>
            <el-table-column label="结案原因" min-width="120">
              <template #default="{ row }">{{ row.order.closeReason ? closeReasonMap[row.order.closeReason] : '-' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`使用反馈 (${d.feedback?.length || 0})`" name="feedback">
          <el-table :data="d.feedback || []" size="small" stripe>
            <el-table-column label="时间" width="150"><template #default="{ row }">{{ fmtTime(row.createdAt) }}</template></el-table-column>
            <el-table-column label="类型" width="100">
              <template #default="{ row }"><el-tag size="small">{{ feedbackTypeMap[row.type] }}</el-tag></template>
            </el-table-column>
            <el-table-column label="风险" width="90">
              <template #default="{ row }"><el-tag :type="riskTag[row.riskLevel]" size="small">{{ riskMap[row.riskLevel] }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="220" />
            <el-table-column label="处置" width="100">
              <template #default="{ row }">{{ row.resolution ? resolutionMap[row.resolution] : '待处理' }}</template>
            </el-table-column>
            <el-table-column prop="handleNote" label="处理说明" min-width="160" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`维修记录 (${d.repairs?.length || 0})`" name="repairs">
          <el-table :data="d.repairs || []" size="small" stripe>
            <el-table-column prop="repairNo" label="维修单号" width="160" />
            <el-table-column prop="description" label="维修内容" min-width="220" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><el-tag :type="repairStatusTag[row.status]" size="small">{{ repairStatusMap[row.status] }}</el-tag></template>
            </el-table-column>
            <el-table-column label="费用" width="90"><template #default="{ row }">{{ row.cost != null ? '¥' + row.cost : '-' }}</template></el-table-column>
            <el-table-column prop="resultNote" label="结果" min-width="150" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`费用明细 (${d.payments?.length || 0})`" name="payments">
          <el-table :data="d.payments || []" size="small" stripe>
            <el-table-column prop="paymentNo" label="流水号" width="150" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }"><el-tag :type="paymentTypeTag[row.type]" size="small">{{ paymentTypeMap[row.type] }}</el-tag></template>
            </el-table-column>
            <el-table-column label="收支" width="70">
              <template #default="{ row }">{{ directionMap[row.direction] }}</template>
            </el-table-column>
            <el-table-column label="金额" width="100">
              <template #default="{ row }"><b :style="{ color: row.direction === 'INCOME' ? '#67c23a' : '#f56c6c' }">¥{{ row.amount }}</b></template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><el-tag :type="paymentStatusTag[row.status]" size="small">{{ paymentStatusMap[row.status] }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="relatedAction" label="对应服务动作" min-width="180" />
            <el-table-column label="时间" width="150"><template #default="{ row }">{{ fmtTime(row.paidAt || row.createdAt) }}</template></el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`补贴 (${d.subsidies?.length || 0})`" name="subsidies">
          <el-table :data="d.subsidies || []" size="small" stripe>
            <el-table-column prop="id" label="编号" width="70" />
            <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ row.amount }}</template></el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }"><el-tag :type="subsidyStatusTag[row.status]" size="small">{{ subsidyStatusMap[row.status] }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="applyReason" label="申请事由" min-width="200" />
            <el-table-column prop="reviewNote" label="审核意见" min-width="160" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`服务事件 (${d.events?.length || 0})`" name="events">
          <el-timeline style="padding: 10px 0 0 4px">
            <el-timeline-item v-for="e in d.events" :key="e.id" :timestamp="fmtTime(e.createdAt)">
              <el-tag :type="eventTypeTag[e.type]" size="small">{{ eventTypeMap[e.type] }}</el-tag>
              <b style="margin: 0 6px">{{ e.title }}</b>
              <span style="color: #606266; font-size: 13px">{{ e.detail }}</span>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 发起评估 -->
    <el-dialog v-model="assessDlg" title="发起入户评估" width="420px">
      <el-form label-width="90px">
        <el-form-item label="评估师">
          <el-select v-model="assessForm.assessorId" placeholder="选择评估师" style="width: 100%">
            <el-option v-for="a in assessors" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assessDlg = false">取消</el-button>
        <el-button type="primary" @click="createAssessment">创建评估单</el-button>
      </template>
    </el-dialog>

    <!-- 发起租赁 -->
    <el-dialog v-model="rentDlg" title="发起租赁（自动锁定在库辅具）" width="480px">
      <el-alert v-if="lastAssessment?.recommendedCategory" type="success" :closable="false" style="margin-bottom: 12px">
        最近评估建议：{{ categoryMap[lastAssessment.recommendedCategory] }} — {{ lastAssessment.recommendationNote }}
      </el-alert>
      <el-form label-width="90px">
        <el-form-item label="辅具型号">
          <el-select v-model="rentForm.modelId" placeholder="选择型号" style="width: 100%">
            <el-option v-for="m in models" :key="m.model.id" :value="m.model.id"
                       :label="`${m.model.name}（押金¥${m.model.depositAmount} 月租¥${m.model.monthlyRent} 可租${m.available}件）`"
                       :disabled="m.available === 0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联评估">
          <el-select v-model="rentForm.assessmentId" clearable placeholder="可选" style="width: 100%">
            <el-option v-for="a in (d.assessments || []).filter(x => x.status === 'COMPLETED')" :key="a.id"
                       :label="`${fmtDate(a.visitDate)} ${categoryMap[a.recommendedCategory] || ''} 评估`" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rentDlg = false">取消</el-button>
        <el-button type="primary" @click="createRental">创建租赁单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuth } from '../store/auth'
import {
  disabilityMap, disabilityTag, elderlyStatusMap, elderlyStatusTag, categoryMap, abilityMap,
  assessmentStatusMap, assessmentStatusTag, rentalStatusMap, rentalStatusTag, closeReasonMap,
  feedbackTypeMap, riskMap, riskTag, resolutionMap, repairStatusMap, repairStatusTag,
  paymentTypeMap, paymentTypeTag, paymentStatusMap, paymentStatusTag, directionMap,
  subsidyStatusMap, subsidyStatusTag, eventTypeMap, eventTypeTag, fmtDate, fmtTime
} from '../api/dicts'

const route = useRoute()
const auth = useAuth()
const id = route.params.id
const isStaff = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))

const loading = ref(false)
const d = ref({})
const tab = ref('assess')
const assessDlg = ref(false)
const rentDlg = ref(false)
const assessors = ref([])
const models = ref([])
const assessForm = ref({ assessorId: null })
const rentForm = ref({ modelId: null, assessmentId: null })

const lastAssessment = computed(() =>
  (d.value.assessments || []).find(a => a.status === 'COMPLETED'))

async function load() {
  loading.value = true
  try {
    d.value = await api.get(`/elderly/${id}/timeline`)
  } finally {
    loading.value = false
  }
}

async function createAssessment() {
  await api.post('/assessments', { elderlyId: Number(id), assessorId: assessForm.value.assessorId })
  ElMessage.success('评估单已创建')
  assessDlg.value = false
  load()
}

async function openRent() {
  models.value = await api.get('/devices/models')
  rentDlg.value = true
}

async function createRental() {
  if (!rentForm.value.modelId) {
    ElMessage.warning('请选择辅具型号')
    return
  }
  await api.post('/rentals', { elderlyId: Number(id), modelId: rentForm.value.modelId, assessmentId: rentForm.value.assessmentId })
  ElMessage.success('租赁单已创建，等待家属确认')
  rentDlg.value = false
  load()
}

onMounted(async () => {
  load()
  if (isStaff.value) {
    assessors.value = await api.get('/users', { params: { role: 'ASSESSOR' } })
  }
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
