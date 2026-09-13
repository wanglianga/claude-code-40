<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="statusFilter" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button v-for="(v, k) in fitReviewStatusMap" :key="k" :value="k">{{ v }}</el-radio-button>
        </el-radio-group>
        <el-button v-if="isStaff" type="primary" :icon="Plus" @click="openCreate">发起尺寸复评</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="review.reviewNo" label="复评单号" width="160" />
        <el-table-column prop="elderlyName" label="老人" width="90" />
        <el-table-column label="辅具" min-width="160">
          <template #default="{ row }">
            <el-tag size="small" style="margin-right: 4px">{{ categoryMap[row.category] }}</el-tag>
            {{ row.modelName }}（{{ row.serialNo }}）
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="fitReviewStatusTag[row.review.status]" size="small">{{ fitReviewStatusMap[row.review.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="复评结论" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.review.conclusion" :type="fitConclusionTag[row.review.conclusion]" size="small">
              {{ fitConclusionMap[row.review.conclusion] }}
            </el-tag><span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="处置" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.review.action" :type="fitActionTag[row.review.action]" size="small">
              {{ fitActionMap[row.review.action] }}
            </el-tag><span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="review.conclusionNote" label="结论原因（家属可见）" min-width="220" show-overflow-tooltip />
        <el-table-column label="发起时间" width="150">
          <template #default="{ row }">{{ fmtTime(row.review.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canSubmitInfo(row)" size="small" type="primary" @click="openInfo(row)">上传资料</el-button>
            <el-button v-if="canReview(row)" size="small" type="primary" @click="openReview(row)">复评</el-button>
            <el-button v-if="canExchange(row)" size="small" type="warning" @click="openExchange(row)">执行换型</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发起复评 -->
    <el-dialog v-model="createDlg" title="发起尺寸复评（限护理床/轮椅）" width="480px">
      <el-form label-width="90px">
        <el-form-item label="租赁订单">
          <el-select v-model="createForm.rentalOrderId" style="width: 100%">
            <el-option v-for="r in candidates" :key="r.order.id"
                       :label="`${r.elderlyName} · ${r.modelName}（${r.serialNo}）`" :value="r.order.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert type="info" :closable="false">
        发起后需家属上传使用照片、身高体重、房间尺寸与照护人说明，再由评估师复评。
      </el-alert>
      <template #footer>
        <el-button @click="createDlg = false">取消</el-button>
        <el-button type="primary" @click="create">发起</el-button>
      </template>
    </el-dialog>

    <!-- 家属上传资料 -->
    <el-dialog v-model="infoDlg" title="上传复评资料" width="560px">
      <el-form label-width="110px">
        <el-form-item label="使用照片" required>
          <div class="photo-row">
            <el-image v-for="p in infoForm.photos" :key="p" :src="fileUrl(p)" :preview-src-list="photoUrls"
                      fit="cover" class="photo-thumb" preview-teleported />
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadPhoto">
              <el-button :icon="Plus" :loading="uploading">上传</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="身高(cm)" required>
              <el-input-number v-model="infoForm.heightCm" :min="100" :max="220" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="体重(kg)" required>
              <el-input-number v-model="infoForm.weightKg" :min="20" :max="200" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房间宽(cm)" required>
              <el-input-number v-model="infoForm.roomWidthCm" :min="0" :max="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房间长(cm)" required>
              <el-input-number v-model="infoForm.roomLengthCm" :min="0" :max="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="照护人说明" required>
          <el-input v-model="infoForm.caregiverNote" type="textarea" :rows="3"
                    placeholder="不适部位、出现时间、使用场景、已尝试的调整等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="infoDlg = false">取消</el-button>
        <el-button type="primary" @click="submitInfo">提交复评资料</el-button>
      </template>
    </el-dialog>

    <!-- 评估师复评 -->
    <el-dialog v-model="reviewDlg" title="评估师复评" width="620px">
      <template v-if="current">
        <el-descriptions :column="2" border size="small" style="margin-bottom: 12px">
          <el-descriptions-item label="身高/体重">
            {{ current.review.heightCm }}cm / {{ current.review.weightKg }}kg
          </el-descriptions-item>
          <el-descriptions-item label="房间尺寸">
            {{ current.review.roomWidthCm ?? '-' }} × {{ current.review.roomLengthCm ?? '-' }} cm
          </el-descriptions-item>
          <el-descriptions-item label="照护人说明" :span="2">{{ current.review.caregiverNote || '-' }}</el-descriptions-item>
        </el-descriptions>
        <div class="photo-row" style="margin-bottom: 14px">
          <el-image v-for="p in splitPhotos(current.review.photoUrls)" :key="p" :src="fileUrl(p)"
                    :preview-src-list="splitPhotos(current.review.photoUrls).map(fileUrl)"
                    fit="cover" class="photo-thumb" preview-teleported />
          <span v-if="!splitPhotos(current.review.photoUrls).length" style="color: #909399">未上传照片</span>
        </div>
        <el-form label-width="110px">
          <el-form-item label="复评结论">
            <el-radio-group v-model="reviewForm.conclusion">
              <el-radio v-for="(v, k) in fitConclusionMap" :key="k" :value="k">{{ v }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="处置决定">
            <el-radio-group v-model="reviewForm.action">
              <el-radio value="TRAINING">培训（操作问题）</el-radio>
              <el-radio value="EXCHANGE">换型（尺寸不合）</el-radio>
              <el-radio value="SUSPEND">暂停租赁（病情变化）</el-radio>
              <el-radio value="NONE">无需处理</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="结论原因">
            <el-input v-model="reviewForm.conclusionNote" type="textarea" :rows="3"
                      placeholder="将展示给家属的换型/培训具体原因" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="reviewDlg = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交复评结论</el-button>
      </template>
    </el-dialog>

    <!-- 执行换型 -->
    <el-dialog v-model="exchangeDlg" title="执行换型（重新确认门宽与照护人操作）" width="480px">
      <el-form label-width="130px">
        <el-form-item label="新型号">
          <el-select v-model="exchangeForm.newModelId" style="width: 100%">
            <el-option v-for="m in exchangeModels" :key="m.model.id" :value="m.model.id"
                       :label="`${m.model.name}（可租${m.available}件）`" :disabled="m.available === 0" />
          </el-select>
        </el-form-item>
        <el-form-item label="门宽复测(cm)" required>
          <el-input-number v-model="exchangeForm.doorWidthReconfirmedCm" :min="40" :max="200" style="width: 100%" />
        </el-form-item>
        <el-form-item label="照护人操作确认">
          <el-switch v-model="exchangeForm.caregiverRetrained" active-text="已重新确认/培训" />
        </el-form-item>
        <el-form-item label="换型说明">
          <el-input v-model="exchangeForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exchangeDlg = false">取消</el-button>
        <el-button type="warning" @click="submitExchange">确认换型</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailDlg" :title="`复评详情 · ${current?.review?.reviewNo || ''}`" size="520px">
      <template v-if="current">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="老人">{{ current.elderlyName }}</el-descriptions-item>
          <el-descriptions-item label="辅具">{{ current.modelName }}（{{ current.serialNo }}）</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="fitReviewStatusTag[current.review.status]" size="small">{{ fitReviewStatusMap[current.review.status] }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="身高/体重">{{ current.review.heightCm ?? '-' }}cm / {{ current.review.weightKg ?? '-' }}kg</el-descriptions-item>
          <el-descriptions-item label="房间尺寸">{{ current.review.roomWidthCm ?? '-' }} × {{ current.review.roomLengthCm ?? '-' }} cm</el-descriptions-item>
          <el-descriptions-item label="照护人说明">{{ current.review.caregiverNote || '-' }}</el-descriptions-item>
          <el-descriptions-item label="复评结论">
            <el-tag v-if="current.review.conclusion" :type="fitConclusionTag[current.review.conclusion]" size="small">
              {{ fitConclusionMap[current.review.conclusion] }}
            </el-tag><span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="处置决定">
            <el-tag v-if="current.review.action" :type="fitActionTag[current.review.action]" size="small">
              {{ fitActionMap[current.review.action] }}
            </el-tag><span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="结论原因">{{ current.review.conclusionNote || '-' }}</el-descriptions-item>
          <el-descriptions-item label="评估师">{{ current.review.assessorName || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="current.review.newModelId" label="换型结果">
            换为「{{ current.newModelName }}」；门宽复测 {{ current.review.doorWidthReconfirmedCm }}cm；
            照护人操作{{ current.review.caregiverRetrained ? '已重新确认' : '未确认' }}
            <div v-if="current.review.exchangeNote" style="color: #909399">{{ current.review.exchangeNote }}</div>
          </el-descriptions-item>
        </el-descriptions>
        <h4>使用照片</h4>
        <div class="photo-row">
          <el-image v-for="p in splitPhotos(current.review.photoUrls)" :key="p" :src="fileUrl(p)"
                    :preview-src-list="splitPhotos(current.review.photoUrls).map(fileUrl)"
                    fit="cover" class="photo-thumb" preview-teleported />
          <span v-if="!splitPhotos(current.review.photoUrls).length" style="color: #909399">未上传照片</span>
        </div>
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
  categoryMap, fitReviewStatusMap, fitReviewStatusTag, fitConclusionMap, fitConclusionTag,
  fitActionMap, fitActionTag, fmtTime
} from '../api/dicts'

const auth = useAuth()
const isStaff = computed(() => ['STAFF', 'ADMIN'].includes(auth.role))
const rows = ref([])
const loading = ref(false)
const statusFilter = ref('')
const createDlg = ref(false)
const infoDlg = ref(false)
const reviewDlg = ref(false)
const exchangeDlg = ref(false)
const detailDlg = ref(false)
const candidates = ref([])
const exchangeModels = ref([])
const current = ref(null)
const uploading = ref(false)
const createForm = ref({ rentalOrderId: null })
const infoForm = ref({ photos: [], heightCm: null, weightKg: null, roomWidthCm: null, roomLengthCm: null, caregiverNote: '' })
const reviewForm = ref({ conclusion: 'OPERATION_ISSUE', action: 'TRAINING', conclusionNote: '' })
const exchangeForm = ref({ newModelId: null, doorWidthReconfirmedCm: null, caregiverRetrained: false, note: '' })

const photoUrls = computed(() => infoForm.value.photos.map(fileUrl))

function fileUrl(name) { return '/api/files/' + name }
function splitPhotos(s) { return (s || '').split(',').filter(Boolean) }

function canSubmitInfo(row) {
  return row.review.status === 'PENDING_INFO' && (auth.isFamily || isStaff.value)
}
function canReview(row) {
  return row.review.status === 'PENDING_REVIEW' && ['ASSESSOR', 'ADMIN'].includes(auth.role)
}
function canExchange(row) {
  return row.review.status === 'EXCHANGING' && isStaff.value
}

async function load() {
  loading.value = true
  try {
    rows.value = await api.get('/fit-reviews', { params: { status: statusFilter.value || undefined } })
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  const all = await api.get('/rentals')
  candidates.value = all.filter(r =>
    ['ACTIVE', 'SUSPENDED'].includes(r.order.status) && ['NURSING_BED', 'WHEELCHAIR'].includes(r.category))
  if (!candidates.value.length) {
    ElMessage.warning('当前没有在租的护理床/轮椅订单')
    return
  }
  createForm.value = { rentalOrderId: candidates.value[0].order.id }
  createDlg.value = true
}

async function create() {
  await api.post('/fit-reviews', createForm.value)
  ElMessage.success('复评单已创建，待家属上传资料')
  createDlg.value = false
  load()
}

function openInfo(row) {
  current.value = row
  infoForm.value = { photos: [], heightCm: null, weightKg: null, roomWidthCm: null, roomLengthCm: null, caregiverNote: '' }
  infoDlg.value = true
}

async function uploadPhoto(opt) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', opt.file)
    const res = await api.post('/files', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    infoForm.value.photos.push(res.name)
    ElMessage.success('照片已上传')
  } finally {
    uploading.value = false
  }
}

async function submitInfo() {
  const f = infoForm.value
  const missing = []
  if (!f.photos.length) missing.push('使用照片')
  if (!f.heightCm) missing.push('身高')
  if (!f.weightKg) missing.push('体重')
  if (!f.roomWidthCm) missing.push('房间宽度')
  if (!f.roomLengthCm) missing.push('房间长度')
  if (!f.caregiverNote || !f.caregiverNote.trim()) missing.push('照护人说明')
  if (missing.length) {
    ElMessage.warning('复评资料不完整，请补充：' + missing.join('、'))
    return
  }
  await api.post(`/fit-reviews/${current.value.review.id}/info`, {
    photoUrls: f.photos.join(','),
    heightCm: f.heightCm,
    weightKg: f.weightKg,
    roomWidthCm: f.roomWidthCm,
    roomLengthCm: f.roomLengthCm,
    caregiverNote: f.caregiverNote
  })
  ElMessage.success('资料已提交，等待评估师复评')
  infoDlg.value = false
  load()
}

function openReview(row) {
  current.value = row
  reviewForm.value = { conclusion: 'OPERATION_ISSUE', action: 'TRAINING', conclusionNote: '' }
  reviewDlg.value = true
}

async function submitReview() {
  if (!reviewForm.value.conclusionNote) {
    ElMessage.warning('请填写结论原因（家属可见）')
    return
  }
  await api.post(`/fit-reviews/${current.value.review.id}/review`, reviewForm.value)
  ElMessage.success('复评结论已提交')
  reviewDlg.value = false
  load()
}

async function openExchange(row) {
  current.value = row
  const models = await api.get('/devices/models')
  exchangeModels.value = models.filter(m => m.model.category === row.category)
  exchangeForm.value = { newModelId: null, doorWidthReconfirmedCm: null, caregiverRetrained: false, note: '' }
  exchangeDlg.value = true
}

async function submitExchange() {
  if (!exchangeForm.value.newModelId) {
    ElMessage.warning('请选择新型号')
    return
  }
  await api.post(`/fit-reviews/${current.value.review.id}/exchange`, exchangeForm.value)
  ElMessage.success('换型完成')
  exchangeDlg.value = false
  load()
}

function openDetail(row) {
  current.value = row
  detailDlg.value = true
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; flex-wrap: wrap; gap: 8px; }
.photo-row { display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
.photo-thumb { width: 84px; height: 84px; border-radius: 6px; border: 1px solid #ebeef5; }
h4 { margin: 14px 0 8px; }
</style>
