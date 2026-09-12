<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="title">
        <el-icon :size="28" color="#409eff"><FirstAidKit /></el-icon>
        <div>
          <h2>社区失能老人辅具租赁平台</h2>
          <p>租赁 · 维修 · 回收 · 补贴 一体化管理</p>
        </div>
      </div>
      <el-form @submit.prevent="doLogin">
        <el-form-item>
          <el-input v-model="username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="password" type="password" placeholder="密码" size="large"
                    :prefix-icon="Lock" show-password @keyup.enter="doLogin" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doLogin">
          登 录
        </el-button>
      </el-form>
      <el-divider>演示账号（点击填充）</el-divider>
      <div class="demo-accounts">
        <el-tag v-for="a in demos" :key="a.u" class="demo-tag" @click="fill(a)">
          {{ a.label }} {{ a.u }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuth } from '../store/auth'

const router = useRouter()
const auth = useAuth()
const username = ref('')
const password = ref('')
const loading = ref(false)

const demos = [
  { label: '管理员', u: 'admin', p: 'admin123' },
  { label: '社区工作人员', u: 'staff', p: 'staff123' },
  { label: '评估师', u: 'assessor', p: 'assess123' },
  { label: '家属', u: 'family', p: 'family123' },
  { label: '仓储运维', u: 'warehouse', p: 'ware123' }
]

function fill(a) {
  username.value = a.u
  password.value = a.p
}

async function doLogin() {
  if (!username.value || !password.value) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3c88 0%, #409eff 100%);
}
.login-card { width: 420px; border-radius: 12px; }
.title { display: flex; gap: 12px; align-items: center; margin-bottom: 20px; }
.title h2 { margin: 0; font-size: 19px; }
.title p { margin: 4px 0 0; color: #909399; font-size: 12px; }
.demo-accounts { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.demo-tag { cursor: pointer; }
</style>
