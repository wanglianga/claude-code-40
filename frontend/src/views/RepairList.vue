<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="PENDING">待处理</el-radio-button>
          <el-radio-button value="IN_PROGRESS">维修中</el-radio-button>
          <el-radio-button value="DONE">已完成</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="repair.repairNo" label="维修单号" width="160" />
        <el-table-column prop="serialNo" label="辅具序列号" width="120" />
        <el-table-column prop="modelName" label="型号" width="120" />
        <el-table-column prop="repair.description" label="维修内容" min-width="240" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="repairStatusTag[row.repair.status]" size="small">{{ repairStatusMap[row.repair.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="维修费用" width="100">
          <template #default="{ row }">{{ row.repair.cost != null ? '¥' + row.repair.cost : '-' }}</template>
        </el-table-column>
        <el-table-column prop="repair.resultNote" label="维修结果" min-width="150" show-overflow-tooltip />
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.repair.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canOps && row.repair.status === 'PENDING'" size="small" type="primary"
                       @click="start(row)">开始维修</el-button>
            <el-button v-if="canOps && row.repair.status === 'IN_PROGRESS'" size="small" type="success"
                       @click="openFinish(row)">完成维修</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="finishDlg" title="完成维修" width="420px">
      <el-form label-width="90px">
        <el-form-item label="维修费用">
          <el-input-number v-model="finishForm.cost" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="维修结果">
          <el-input v-model="finishForm.resultNote" type="textarea" :rows="2" placeholder="更换部件/调试结果" />
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false">
        维修费用将单独记账（平台支出）；若租约仍在，辅具自动恢复「租赁中」。
      </el-alert>
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
import { repairStatusMap, repairStatusTag, fmtTime } from '../api/dicts'

const auth = useAuth()
const canOps = computed(() => ['WAREHOUSE', 'ADMIN', 'STAFF'].includes(auth.role))
const rows = ref([])
const loading = ref(false)
const statusFilter = ref('')
const finishDlg = ref(false)
const finishId = ref(null)
const finishForm = ref({ cost: 0, resultNote: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/repairs', { params: { status: statusFilter.value || undefined } })
  } finally {
    loading.value = false
  }
}

async function start(row) {
  await api.post(`/repairs/${row.repair.id}/start`)
  ElMessage.success('已开始维修')
  load()
}

function openFinish(row) {
  finishId.value = row.repair.id
  finishForm.value = { cost: 0, resultNote: '' }
  finishDlg.value = true
}

async function finish() {
  await api.post(`/repairs/${finishId.value}/finish`, finishForm.value)
  ElMessage.success('维修完成，费用已记账')
  finishDlg.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; }
</style>
