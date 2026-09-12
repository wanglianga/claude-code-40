<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待评估</el-radio-button>
          <el-radio-button value="COMPLETED">已完成</el-radio-button>
        </el-radio-group>
        <el-button v-if="isStaff" type="primary" :icon="Plus" @click="openCreate">新建评估单</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="编号" width="70" />
        <el-table-column prop="elderlyName" label="老人" width="100" />
        <el-table-column prop="assessment.assessorName" label="评估师" width="140">
          <template #default="{ row }">{{ row.assessment.assessorName || '待指派' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="assessmentStatusTag[row.assessment.status]" size="small">{{ assessmentStatusMap[row.assessment.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入户日期" width="110">
          <template #default="{ row }">{{ fmtDate(row.assessment.visitDate) }}</template>
        </el-table-column>
        <el-table-column label="居家测量" min-width="230">
          <template #default="{ row }">
            <span v-if="row.assessment.status === 'COMPLETED'">
              门宽{{ row.assessment.doorWidthCm }}cm · {{ row.assessment.hasElevator ? '有电梯' : `无电梯${row.assessment.floor}楼` }}
              · 床边{{ row.assessment.bedsideSpaceCm }}cm · 卫生间{{ row.assessment.bathroomWidthCm }}×{{ row.assessment.bathroomLengthCm }}cm
            </span><span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="照护人能力" width="100">
          <template #default="{ row }">{{ abilityMap[row.assessment.caregiverAbility] || '-' }}</template>
        </el-table-column>
        <el-table-column label="辅具建议" min-width="220">
          <template #default="{ row }">
            <span v-if="row.assessment.recommendedCategory">
              <el-tag size="small" type="success">{{ categoryMap[row.assessment.recommendedCategory] }}</el-tag>
              {{ row.modelName || '' }} {{ row.assessment.recommendationNote }}
            </span><span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canComplete(row)" size="small" type="primary" @click="openComplete(row)">入户评估</el-button>
            <el-button size="small" @click="view(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建评估 -->
    <el-dialog v-model="createDlg" title="新建入户评估单" width="440px">
      <el-form label-width="90px">
        <el-form-item label="老人">
          <el-select v-model="createForm.elderlyId" filterable placeholder="选择老人" style="width: 100%">
            <el-option v-for="e in elderlyList" :key="e.id" :label="`${e.name}（${e.community}）`" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估师">
          <el-select v-model="createForm.assessorId" placeholder="指派评估师" style="width: 100%">
            <el-option v-for="a in assessors" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDlg = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <!-- 完成评估 -->
    <el-dialog v-model="completeDlg" title="入户评估测量与辅具建议" width="620px">
      <el-form :model="completeForm" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="入户日期">
              <el-date-picker v-model="completeForm.visitDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有无电梯">
              <el-switch v-model="completeForm.hasElevator" active-text="有" inactive-text="无" />
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="楼层"><el-input-number v-model="completeForm.floor" :min="1" :max="60" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="门宽(cm)"><el-input-number v-model="completeForm.doorWidthCm" :min="40" :max="200" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="床边空间(cm)"><el-input-number v-model="completeForm.bedsideSpaceCm" :min="0" :max="300" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="照护人能力">
              <el-select v-model="completeForm.caregiverAbility" style="width: 100%">
                <el-option v-for="(v, k) in abilityMap" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="卫生间宽(cm)"><el-input-number v-model="completeForm.bathroomWidthCm" :min="0" :max="500" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="卫生间长(cm)"><el-input-number v-model="completeForm.bathroomLengthCm" :min="0" :max="500" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="居家情况"><el-input v-model="completeForm.notes" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
        <el-divider>辅具建议</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="建议类别">
              <el-select v-model="completeForm.recommendedCategory" style="width: 100%" @change="onCategoryChange">
                <el-option v-for="(v, k) in categoryMap" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建议型号">
              <el-select v-model="completeForm.recommendedModelId" style="width: 100%">
                <el-option v-for="m in categoryModels" :key="m.model.id" :label="m.model.name" :value="m.model.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="建议说明"><el-input v-model="completeForm.recommendationNote" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="completeDlg = false">取消</el-button>
        <el-button type="primary" @click="complete">提交评估结论</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailDlg" title="评估详情" size="440px">
      <el-descriptions v-if="current" :column="1" border size="small">
        <el-descriptions-item label="老人">{{ current.elderlyName }}</el-descriptions-item>
        <el-descriptions-item label="评估师">{{ current.assessment.assessorName || '待指派' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ assessmentStatusMap[current.assessment.status] }}</el-descriptions-item>
        <el-descriptions-item label="入户日期">{{ fmtDate(current.assessment.visitDate) }}</el-descriptions-item>
        <el-descriptions-item label="门宽">{{ current.assessment.doorWidthCm ?? '-' }} cm</el-descriptions-item>
        <el-descriptions-item label="电梯">{{ current.assessment.hasElevator == null ? '-' : (current.assessment.hasElevator ? '有' : '无，' + current.assessment.floor + ' 楼') }}</el-descriptions-item>
        <el-descriptions-item label="床边空间">{{ current.assessment.bedsideSpaceCm ?? '-' }} cm</el-descriptions-item>
        <el-descriptions-item label="卫生间">{{ current.assessment.bathroomWidthCm ?? '-' }} × {{ current.assessment.bathroomLengthCm ?? '-' }} cm</el-descriptions-item>
        <el-descriptions-item label="照护人能力">{{ abilityMap[current.assessment.caregiverAbility] || '-' }}</el-descriptions-item>
        <el-descriptions-item label="居家情况">{{ current.assessment.notes || '-' }}</el-descriptions-item>
        <el-descriptions-item label="建议">{{ current.assessment.recommendationNote || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'
import { useAuth } from '../store/auth'
import { assessmentStatusMap, assessmentStatusTag, abilityMap, categoryMap, fmtDate } from '../api/dicts'

const auth = useAuth()
const isStaff = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))
const rows = ref([])
const loading = ref(false)
const statusFilter = ref('')
const createDlg = ref(false)
const completeDlg = ref(false)
const detailDlg = ref(false)
const elderlyList = ref([])
const assessors = ref([])
const models = ref([])
const current = ref(null)
const createForm = ref({ elderlyId: null, assessorId: null })
const completeForm = ref({})
const completeId = ref(null)

const categoryModels = computed(() =>
  models.value.filter(m => m.model.category === completeForm.value.recommendedCategory))

function canComplete(row) {
  return row.assessment.status === 'PENDING' && ['ASSESSOR', 'ADMIN'].includes(auth.role)
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/assessments', { params: { status: statusFilter.value || undefined } })
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  elderlyList.value = await api.get('/elderly')
  assessors.value = await api.get('/users', { params: { role: 'ASSESSOR' } })
  createForm.value = { elderlyId: null, assessorId: assessors.value[0]?.id ?? null }
  createDlg.value = true
}

async function create() {
  if (!createForm.value.elderlyId) {
    ElMessage.warning('请选择老人')
    return
  }
  await api.post('/assessments', createForm.value)
  ElMessage.success('评估单已创建')
  createDlg.value = false
  load()
}

async function openComplete(row) {
  completeId.value = row.assessment.id
  models.value = await api.get('/devices/models')
  completeForm.value = {
    visitDate: new Date().toISOString().slice(0, 10),
    hasElevator: false, floor: 1, doorWidthCm: 80, bedsideSpaceCm: 80,
    bathroomWidthCm: 120, bathroomLengthCm: 150, caregiverAbility: 'FAIR',
    notes: '', recommendedCategory: null, recommendedModelId: null, recommendationNote: ''
  }
  completeDlg.value = true
}

function onCategoryChange() {
  completeForm.value.recommendedModelId = null
}

async function complete() {
  await api.put(`/assessments/${completeId.value}/complete`, completeForm.value)
  ElMessage.success('评估已完成，辅具建议已生成')
  completeDlg.value = false
  load()
}

function view(row) {
  current.value = row
  detailDlg.value = true
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; }
</style>
