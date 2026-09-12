<template>
  <div>
    <el-row :gutter="16">
      <!-- 老人维度 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>老人维度档案</span>
              <el-select v-model="elderlyId" filterable placeholder="选择老人" style="width: 220px" @change="loadElderly">
                <el-option v-for="e in elderlyList" :key="e.id" :label="`${e.name}（${e.community}）`" :value="e.id" />
              </el-select>
            </div>
          </template>
          <template v-if="elderlyData">
            <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
              <el-descriptions-item label="姓名">{{ elderlyData.elderly.name }}</el-descriptions-item>
              <el-descriptions-item label="失能等级">{{ disabilityMap[elderlyData.elderly.disabilityLevel] }}</el-descriptions-item>
              <el-descriptions-item label="评估次数">{{ elderlyData.assessments.length }}</el-descriptions-item>
              <el-descriptions-item label="租赁次数">{{ elderlyData.rentals.length }}</el-descriptions-item>
              <el-descriptions-item label="维修次数">{{ elderlyData.repairs.length }}</el-descriptions-item>
              <el-descriptions-item label="费用笔数">{{ elderlyData.payments.length }}</el-descriptions-item>
            </el-descriptions>
            <h4>经历过的辅具</h4>
            <el-table :data="elderlyData.rentals" size="small" border style="margin-bottom: 12px">
              <el-table-column prop="modelName" label="型号" width="110" />
              <el-table-column prop="serialNo" label="序列号" width="110" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="rentalStatusTag[row.order.status]" size="small">{{ rentalStatusMap[row.order.status] }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="租期" min-width="170">
                <template #default="{ row }">{{ fmtDate(row.order.startDate) }} ~ {{ fmtDate(row.order.endDate) }}</template>
              </el-table-column>
            </el-table>
            <h4>评估 · 租赁 · 维修 · 回收 全事件</h4>
            <el-timeline style="padding-left: 4px; max-height: 420px; overflow-y: auto">
              <el-timeline-item v-for="e in elderlyData.events" :key="e.id" :timestamp="fmtTime(e.createdAt)">
                <el-tag :type="eventTypeTag[e.type]" size="small">{{ eventTypeMap[e.type] }}</el-tag>
                <b style="margin: 0 6px">{{ e.title }}</b>
                <span style="color: #606266; font-size: 13px">{{ e.detail }}</span>
              </el-timeline-item>
            </el-timeline>
          </template>
          <el-empty v-else description="请选择老人查看完整档案" />
        </el-card>
      </el-col>

      <!-- 辅具维度 -->
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>辅具维度档案</span>
              <el-select v-model="unitId" filterable placeholder="选择辅具" style="width: 240px" @change="loadUnit">
                <el-option v-for="u in unitList" :key="u.unit.id"
                           :label="`${u.unit.serialNo} · ${u.modelName}`" :value="u.unit.id" />
              </el-select>
            </div>
          </template>
          <template v-if="unitData">
            <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
              <el-descriptions-item label="序列号">{{ unitData.unit.serialNo }}</el-descriptions-item>
              <el-descriptions-item label="型号">{{ unitData.modelName }}</el-descriptions-item>
              <el-descriptions-item label="当前状态">
                <el-tag :type="deviceStatusTag[unitData.unit.status]" size="small">{{ deviceStatusMap[unitData.unit.status] }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="累计租期">{{ unitData.unit.rentalCount }}</el-descriptions-item>
            </el-descriptions>
            <h4>服务过的家庭（{{ unitData.rentalHistory.length }}）</h4>
            <el-table :data="unitData.rentalHistory" size="small" border style="margin-bottom: 12px">
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
            <h4>生命周期（租赁 · 维修 · 回收 · 消毒 · 再上架）</h4>
            <el-timeline style="padding-left: 4px; max-height: 420px; overflow-y: auto">
              <el-timeline-item v-for="e in unitData.events" :key="e.id" :timestamp="fmtTime(e.createdAt)">
                <el-tag :type="eventTypeTag[e.type]" size="small">{{ eventTypeMap[e.type] }}</el-tag>
                <b style="margin: 0 6px">{{ e.title }}</b>
                <span style="color: #606266; font-size: 13px">{{ e.detail }}</span>
              </el-timeline-item>
            </el-timeline>
          </template>
          <el-empty v-else description="请选择辅具查看生命周期" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'
import {
  disabilityMap, rentalStatusMap, rentalStatusTag, deviceStatusMap, deviceStatusTag,
  eventTypeMap, eventTypeTag, fmtDate, fmtTime
} from '../api/dicts'

const elderlyList = ref([])
const unitList = ref([])
const elderlyId = ref(null)
const unitId = ref(null)
const elderlyData = ref(null)
const unitData = ref(null)

async function loadElderly() {
  elderlyData.value = elderlyId.value ? await api.get(`/elderly/${elderlyId.value}/timeline`) : null
}

async function loadUnit() {
  unitData.value = unitId.value ? await api.get(`/devices/units/${unitId.value}/lifecycle`) : null
}

onMounted(async () => {
  elderlyList.value = await api.get('/elderly')
  unitList.value = await api.get('/devices/units')
  if (elderlyList.value.length) {
    elderlyId.value = elderlyList.value[0].id
    loadElderly()
  }
  if (unitList.value.length) {
    unitId.value = unitList.value[0].unit.id
    loadUnit()
  }
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
h4 { margin: 10px 0 8px; }
</style>
