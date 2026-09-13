<template>
  <div>
    <el-card shadow="never" v-if="!auth.isFamily">
      <div class="spare-strip">
        <span class="spare-title">备件库存：</span>
        <el-tag v-for="p in spareParts" :key="p.id" :type="p.stock > 0 ? 'success' : 'danger'" style="margin-right: 8px">
          {{ p.name }} × {{ p.stock }}
        </el-tag>
      </div>
    </el-card>

    <el-card shadow="never" style="margin-top: 14px">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(v, k) in repairStatusMap" :key="k" :value="k">{{ v }}</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <el-alert v-if="row.repair.safetyNotice" type="error" :closable="false" style="margin-bottom: 8px">
                <b>照护人临时安全措施：</b>{{ row.repair.safetyNotice }}
              </el-alert>
              <el-alert v-if="row.repair.alternativeNotice" type="warning" :closable="false" style="margin-bottom: 8px">
                <b>临时替代办法：</b>{{ row.repair.alternativeNotice }}
              </el-alert>
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="排程依据">{{ row.repair.priorityReason || '-' }}</el-descriptions-item>
                <el-descriptions-item label="备件">{{ row.repair.sparePartName || '无需' }}（{{ row.repair.spareReady == null ? '-' : (row.repair.spareReady ? '有库存' : '缺货') }}）</el-descriptions-item>
                <el-descriptions-item label="试用结果">{{ row.repair.visitResult ? visitResultMap[row.repair.visitResult] : '未回传' }}</el-descriptions-item>
                <el-descriptions-item label="试用说明">{{ row.repair.visitNote || '-' }}</el-descriptions-item>
                <template v-if="row.handover">
                  <el-descriptions-item label="出库身体状况">{{ row.handover.elderlyCondition || '-' }}</el-descriptions-item>
                  <el-descriptions-item label="适配建议">{{ row.handover.fittingAdvice || '-' }}（家属{{ row.handover.familyConfirmed ? '已确认' : '未确认' }}）</el-descriptions-item>
                </template>
                <el-descriptions-item v-if="row.repair.misuseNote" label="误用说明" :span="2">{{ row.repair.misuseNote }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="repair.repairNo" label="维修单号" width="150" />
        <el-table-column label="故障" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.repair.faultType" size="small">{{ feedbackTypeMap[row.repair.faultType] }}</el-tag><span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.repair.priority" :type="repairPriorityTag[row.repair.priority]" size="small">
              {{ repairPriorityMap[row.repair.priority] }}
            </el-tag><span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="elderlyName" label="老人" width="90" />
        <el-table-column prop="serialNo" label="辅具" width="105" />
        <el-table-column label="维修员(距离)" width="110">
          <template #default="{ row }">
            <span v-if="row.repair.assigneeName">{{ row.repair.assigneeName }}（{{ row.repair.distanceKm }}km）</span><span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="计划上门" width="145">
          <template #default="{ row }">{{ fmtTime(row.repair.scheduledAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="repairStatusTag[row.repair.status]" size="small">{{ repairStatusMap[row.repair.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="费用" width="80">
          <template #default="{ row }">{{ row.repair.cost != null ? '¥' + row.repair.cost : '-' }}</template>
        </el-table-column>
        <el-table-column label="误用" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.repair.misuse === true" type="danger" size="small">家属误用</el-tag>
            <el-tag v-else-if="row.repair.misuse === false" type="success" size="small">正常磨损</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="isStaff && row.repair.status !== 'DONE'" size="small" @click="reschedule(row)">重新排程</el-button>
            <el-button v-if="canOps && ['PENDING', 'SCHEDULED'].includes(row.repair.status)" size="small" type="primary"
                       @click="start(row)">开始上门</el-button>
            <el-button v-if="canOps && row.repair.status === 'IN_PROGRESS'" size="small" type="success"
                       @click="openFinish(row)">完成维修</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="finishDlg" title="完成维修（回传试用结果）" width="500px">
      <el-alert v-if="currentHandover?.elderlyCondition" type="info" :closable="false" style="margin-bottom: 12px">
        <b>出库适配记录：</b>{{ currentHandover.elderlyCondition }}
        <div v-if="currentHandover.fittingAdvice">适配建议：{{ currentHandover.fittingAdvice }}</div>
        <div>家属确认：{{ currentHandover.familyConfirmed ? '已确认' : '未确认' }}</div>
      </el-alert>
      <el-form label-width="90px">
        <el-form-item label="试用结果" required>
          <el-radio-group v-model="finishForm.visitResult">
            <el-radio value="NORMAL">试用正常</el-radio>
            <el-radio value="ABNORMAL">仍有问题（退回待上门）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="试用说明">
          <el-input v-model="finishForm.visitNote" type="textarea" :rows="2"
                    placeholder="老人试用情况：刹车/升降/气泵等是否正常" />
        </el-form-item>
        <template v-if="finishForm.visitResult === 'NORMAL'">
          <el-form-item label="维修费用">
            <el-input-number v-model="finishForm.cost" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="维修结果">
            <el-input v-model="finishForm.resultNote" type="textarea" :rows="2" placeholder="更换部件/调试结果" />
          </el-form-item>
          <el-form-item label="误用判定">
            <el-radio-group v-model="finishForm.misuse">
              <el-radio :value="false">正常磨损</el-radio>
              <el-radio :value="true">家属误用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="finishForm.misuse" label="误用说明">
            <el-input v-model="finishForm.misuseNote" type="textarea" :rows="2"
                      placeholder="对照出库适配记录说明误用情况" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="finishDlg = false">取消</el-button>
        <el-button type="primary" @click="finish">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import { useAuth } from '../store/auth'
import {
  repairStatusMap, repairStatusTag, repairPriorityMap, repairPriorityTag,
  feedbackTypeMap, visitResultMap, fmtTime
} from '../api/dicts'

const auth = useAuth()
const canOps = computed(() => ['WAREHOUSE', 'ADMIN', 'STAFF'].includes(auth.role))
const isStaff = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))
const rows = ref([])
const spareParts = ref([])
const loading = ref(false)
const statusFilter = ref('')
const finishDlg = ref(false)
const finishId = ref(null)
const finishForm = ref({ cost: 0, resultNote: '', misuse: false, misuseNote: '', visitResult: 'NORMAL', visitNote: '' })
const currentHandover = ref(null)

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/repairs', { params: { status: statusFilter.value || undefined } })
  } finally {
    loading.value = false
  }
}

async function loadSpareParts() {
  spareParts.value = await api.get('/repairs/spare-parts')
}

async function reschedule(row) {
  await api.post(`/repairs/${row.repair.id}/schedule`)
  ElMessage.success('已按最新情况重新排程')
  load()
}

async function start(row) {
  await api.post(`/repairs/${row.repair.id}/start`)
  ElMessage.success('维修员已上门')
  load()
}

function openFinish(row) {
  finishId.value = row.repair.id
  finishForm.value = { cost: 0, resultNote: '', misuse: false, misuseNote: '', visitResult: 'NORMAL', visitNote: '' }
  currentHandover.value = row.handover || null
  finishDlg.value = true
}

async function finish() {
  await api.post(`/repairs/${finishId.value}/finish`, finishForm.value)
  ElMessage.success(finishForm.value.visitResult === 'NORMAL' ? '维修完成，费用已记账' : '已退回待上门，请重新排程')
  finishDlg.value = false
  load()
  loadSpareParts()
}

onMounted(() => {
  load()
  loadSpareParts()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; }
.spare-strip { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.spare-title { font-weight: 600; }
.expand-box { padding: 4px 12px; }
</style>
