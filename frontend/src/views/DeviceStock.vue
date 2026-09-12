<template>
  <div>
    <el-card shadow="never">
      <template #header>辅具型号与库存</template>
      <el-table :data="models" size="small" stripe>
        <el-table-column prop="model.code" label="编码" width="90" />
        <el-table-column prop="model.name" label="型号名称" width="130" />
        <el-table-column label="类别" width="100">
          <template #default="{ row }"><el-tag size="small">{{ categoryMap[row.model.category] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="model.spec" label="规格" min-width="200" show-overflow-tooltip />
        <el-table-column label="押金" width="90"><template #default="{ row }">¥{{ row.model.depositAmount }}</template></el-table-column>
        <el-table-column label="月租金" width="90"><template #default="{ row }">¥{{ row.model.monthlyRent }}</template></el-table-column>
        <el-table-column prop="total" label="总数" width="70" />
        <el-table-column label="可租" width="70">
          <template #default="{ row }"><b :style="{ color: row.available > 0 ? '#67c23a' : '#f56c6c' }">{{ row.available }}</b></template>
        </el-table-column>
        <el-table-column prop="leased" label="在租" width="70" />
      </el-table>
    </el-card>

    <el-card shadow="never" style="margin-top: 14px">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="loadUnits">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(v, k) in deviceStatusMap" :key="k" :value="k">{{ v }}</el-radio-button>
        </el-radio-group>
        <el-button v-if="canManage" type="primary" :icon="Plus" @click="openAdd">新增入库</el-button>
      </div>
      <el-table :data="units" v-loading="loading" stripe>
        <el-table-column prop="unit.serialNo" label="序列号" width="120" />
        <el-table-column prop="modelName" label="型号" width="120" />
        <el-table-column label="类别" width="100">
          <template #default="{ row }"><el-tag size="small">{{ categoryMap[row.category] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="deviceStatusTag[row.unit.status]" size="small">{{ deviceStatusMap[row.unit.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="unit.rentalCount" label="累计租期" width="80" />
        <el-table-column prop="unit.conditionNote" label="成色/备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="入库日期" width="110">
          <template #default="{ row }">{{ fmtDate(row.unit.purchaseDate) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openLifecycle(row)">生命周期</el-button>
            <el-button v-if="canManage && row.unit.status === 'RECALLED'" size="small" type="warning"
                       @click="disinfect(row)">开始消毒</el-button>
            <el-button v-if="canManage && row.unit.status === 'DISINFECTING'" size="small" type="success"
                       @click="openQc(row)">质检结果</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增入库 -->
    <el-dialog v-model="addDlg" title="辅具入库" width="420px">
      <el-form label-width="90px">
        <el-form-item label="型号">
          <el-select v-model="addForm.modelId" style="width: 100%">
            <el-option v-for="m in models" :key="m.model.id" :label="`${m.model.name}（${m.model.code}）`" :value="m.model.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="序列号"><el-input v-model="addForm.serialNo" placeholder="如 WZ01-004" /></el-form-item>
        <el-form-item label="成色备注"><el-input v-model="addForm.conditionNote" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDlg = false">取消</el-button>
        <el-button type="primary" @click="addUnit">入库</el-button>
      </template>
    </el-dialog>

    <!-- 质检 -->
    <el-dialog v-model="qcDlg" title="消毒质检结果" width="420px">
      <el-form label-width="90px">
        <el-form-item label="质检结论">
          <el-radio-group v-model="qcForm.pass">
            <el-radio :value="true">合格 · 再上架</el-radio>
            <el-radio :value="false">不合格 · 报废</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="质检说明"><el-input v-model="qcForm.note" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <el-alert type="info" :closable="false">
        质检合格将重新计入可租库存；若关联租赁有待核销补贴，将自动完成补贴核销。
      </el-alert>
      <template #footer>
        <el-button @click="qcDlg = false">取消</el-button>
        <el-button type="primary" @click="qc">提交</el-button>
      </template>
    </el-dialog>

    <!-- 生命周期 -->
    <el-drawer v-model="lifeDlg" :title="`辅具生命周期 · ${life?.unit?.serialNo || ''}`" size="560px">
      <template v-if="life">
        <el-descriptions :column="2" border size="small" style="margin-bottom: 16px">
          <el-descriptions-item label="型号">{{ life.modelName }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="deviceStatusTag[life.unit.status]" size="small">{{ deviceStatusMap[life.unit.status] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="累计租期">{{ life.unit.rentalCount }}</el-descriptions-item>
          <el-descriptions-item label="成色">{{ life.unit.conditionNote || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4>服务过的家庭</h4>
        <el-table :data="life.rentalHistory" size="small" border>
          <el-table-column prop="orderNo" label="订单号" width="150" />
          <el-table-column prop="elderlyName" label="老人" width="90" />
          <el-table-column prop="caregiverName" label="照护人" width="90" />
          <el-table-column prop="community" label="社区" width="100" />
          <el-table-column label="租期" min-width="170">
            <template #default="{ row }">{{ fmtDate(row.startDate) }} ~ {{ fmtDate(row.endDate) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="rentalStatusTag[row.status]" size="small">{{ rentalStatusMap[row.status] }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <h4 style="margin-top: 18px">生命周期事件</h4>
        <el-timeline style="padding-left: 4px">
          <el-timeline-item v-for="e in life.events" :key="e.id" :timestamp="fmtTime(e.createdAt)">
            <el-tag :type="eventTypeTag[e.type]" size="small">{{ eventTypeMap[e.type] }}</el-tag>
            <b style="margin: 0 6px">{{ e.title }}</b>
            <div style="color: #606266; font-size: 13px">{{ e.detail }}</div>
            <div style="color: #909399; font-size: 12px">经办：{{ e.operatorName || '系统' }}</div>
          </el-timeline-item>
        </el-timeline>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '../api'
import { useAuth } from '../store/auth'
import {
  categoryMap, deviceStatusMap, deviceStatusTag, rentalStatusMap, rentalStatusTag,
  eventTypeMap, eventTypeTag, fmtDate, fmtTime
} from '../api/dicts'

const auth = useAuth()
const canManage = computed(() => ['WAREHOUSE', 'ADMIN'].includes(auth.role))
const models = ref([])
const units = ref([])
const loading = ref(false)
const statusFilter = ref('')
const addDlg = ref(false)
const qcDlg = ref(false)
const lifeDlg = ref(false)
const addForm = ref({ modelId: null, serialNo: '', conditionNote: '' })
const qcForm = ref({ pass: true, note: '' })
const qcUnitId = ref(null)
const life = ref(null)

async function loadModels() {
  models.value = await api.get('/devices/models')
}

async function loadUnits() {
  loading.value = true
  try {
    units.value = await api.get('/devices/units', { params: { status: statusFilter.value || undefined } })
  } finally {
    loading.value = false
  }
}

function openAdd() {
  addForm.value = { modelId: models.value[0]?.model.id ?? null, serialNo: '', conditionNote: '' }
  addDlg.value = true
}

async function addUnit() {
  if (!addForm.value.serialNo) {
    ElMessage.warning('请填写序列号')
    return
  }
  await api.post('/devices/units', addForm.value)
  ElMessage.success('已入库')
  addDlg.value = false
  loadModels()
  loadUnits()
}

async function disinfect(row) {
  await api.post(`/devices/units/${row.unit.id}/disinfect`)
  ElMessage.success('已进入消毒质检流程')
  loadUnits()
}

function openQc(row) {
  qcUnitId.value = row.unit.id
  qcForm.value = { pass: true, note: '' }
  qcDlg.value = true
}

async function qc() {
  await api.post(`/devices/units/${qcUnitId.value}/qc`, qcForm.value)
  ElMessage.success(qcForm.value.pass ? '质检合格，已重新上架' : '已报废出库')
  qcDlg.value = false
  loadModels()
  loadUnits()
}

async function openLifecycle(row) {
  life.value = await api.get(`/devices/units/${row.unit.id}/lifecycle`)
  lifeDlg.value = true
}

onMounted(() => {
  loadModels()
  loadUnits()
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; flex-wrap: wrap; gap: 8px; }
h4 { margin: 8px 0; }
</style>
