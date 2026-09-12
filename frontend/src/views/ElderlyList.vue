<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="按姓名搜索" clearable style="width: 220px"
                  :prefix-icon="Search" @change="load" />
        <el-button v-if="canEdit" type="primary" :icon="Plus" @click="openForm()">建立老人档案</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="姓名" width="90">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/elderly/${row.id}`)">{{ row.name }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="community" label="社区" width="100" />
        <el-table-column label="失能等级" width="100">
          <template #default="{ row }">
            <el-tag :type="disabilityTag[row.disabilityLevel]" size="small">{{ disabilityMap[row.disabilityLevel] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所需辅具" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="d in (row.neededDevices || '').split(',').filter(Boolean)" :key="d"
                    size="small" style="margin-right: 4px">{{ categoryMap[d] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="照护人" width="130">
          <template #default="{ row }">{{ row.caregiverName }}（{{ row.caregiverRelation }}）</template>
        </el-table-column>
        <el-table-column prop="insuranceType" label="医保" width="110" />
        <el-table-column label="补贴资格" width="90">
          <template #default="{ row }">
            <el-tag :type="row.subsidyEligible ? 'success' : 'info'" size="small">
              {{ row.subsidyEligible ? row.subsidyType || '有' : '无' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="elderlyStatusTag[row.status]" size="small">{{ elderlyStatusMap[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/elderly/${row.id}`)">档案</el-button>
            <el-button v-if="canEdit" size="small" type="primary" @click="openForm(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dlg" :title="form.id ? '编辑档案' : '建立老人档案'" width="640px">
      <el-form :model="form" label-width="96px">
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="性别">
              <el-select v-model="form.gender"><el-option label="男" value="男" /><el-option label="女" value="女" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="年龄"><el-input-number v-model="form.age" :min="50" :max="120" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属社区"><el-input v-model="form.community" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="失能等级">
              <el-select v-model="form.disabilityLevel">
                <el-option v-for="(v, k) in disabilityMap" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="居住地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="居住环境"><el-input v-model="form.livingEnv" placeholder="楼层/电梯/房屋结构/卫生间等" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="照护人"><el-input v-model="form.caregiverName" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="关系"><el-input v-model="form.caregiverRelation" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="照护人电话"><el-input v-model="form.caregiverPhone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="医保类型"><el-input v-model="form.insuranceType" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="补贴资格">
              <el-switch v-model="form.subsidyEligible" active-text="有" inactive-text="无" />
              <el-input v-if="form.subsidyEligible" v-model="form.subsidyType" placeholder="补贴类型（长护险/民政补贴）" style="margin-top: 6px" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所需辅具">
              <el-checkbox-group v-model="neededArr">
                <el-checkbox v-for="(v, k) in categoryMap" :key="k" :value="k">{{ v }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="老人状态">
              <el-select v-model="form.status">
                <el-option v-for="(v, k) in elderlyStatusMap" :key="k" :label="v" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import api from '../api'
import { useAuth } from '../store/auth'
import { disabilityMap, disabilityTag, elderlyStatusMap, elderlyStatusTag, categoryMap } from '../api/dicts'

const auth = useAuth()
const canEdit = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))
const rows = ref([])
const loading = ref(false)
const keyword = ref('')
const dlg = ref(false)
const saving = ref(false)
const form = ref({})
const neededArr = ref([])

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/elderly', { params: { keyword: keyword.value || undefined } })
  } finally {
    loading.value = false
  }
}

function openForm(row) {
  form.value = row ? { ...row } : { gender: '男', age: 70, disabilityLevel: 'MILD', status: 'ACTIVE', subsidyEligible: false }
  neededArr.value = row ? (row.neededDevices || '').split(',').filter(Boolean) : []
  dlg.value = true
}

async function save() {
  if (!form.value.name) {
    ElMessage.warning('请填写老人姓名')
    return
  }
  saving.value = true
  try {
    const body = { ...form.value, neededDevices: neededArr.value.join(',') }
    if (form.value.id) {
      await api.put(`/elderly/${form.value.id}`, body)
    } else {
      await api.post('/elderly', body)
    }
    ElMessage.success('已保存')
    dlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; }
</style>
