<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22" color="#fff"><FirstAidKit /></el-icon>
        <span>辅具租赁平台</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d" text-color="#bfcbd9"
               active-text-color="#409eff" style="border-right: none">
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <span>{{ m.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title || '工作台' }}</span>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ auth.user?.name }}
            <el-tag size="small" style="margin-left: 6px">{{ roleMap[auth.role] }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../store/auth'
import { roleMap } from '../api/dicts'

const router = useRouter()
const auth = useAuth()

const allMenus = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer', roles: ['ADMIN', 'STAFF', 'ASSESSOR', 'FAMILY', 'WAREHOUSE'] },
  { path: '/elderly', title: '老人档案', icon: 'UserFilled', roles: ['ADMIN', 'STAFF', 'ASSESSOR', 'FAMILY'] },
  { path: '/assessments', title: '入户评估', icon: 'Checked', roles: ['ADMIN', 'STAFF', 'ASSESSOR'] },
  { path: '/devices', title: '辅具库存', icon: 'Box', roles: ['ADMIN', 'STAFF', 'WAREHOUSE'] },
  { path: '/rentals', title: '租赁订单', icon: 'Tickets', roles: ['ADMIN', 'STAFF', 'WAREHOUSE', 'FAMILY'] },
  { path: '/feedback', title: '使用反馈', icon: 'ChatDotSquare', roles: ['ADMIN', 'STAFF', 'FAMILY'] },
  { path: '/fit-reviews', title: '尺寸复评', icon: 'ScaleToOriginal', roles: ['ADMIN', 'STAFF', 'ASSESSOR', 'FAMILY'] },
  { path: '/repairs', title: '维修管理', icon: 'Tools', roles: ['ADMIN', 'STAFF', 'WAREHOUSE', 'FAMILY'] },
  { path: '/payments', title: '费用中心', icon: 'Wallet', roles: ['ADMIN', 'STAFF', 'FAMILY'] },
  { path: '/subsidies', title: '补贴管理', icon: 'Money', roles: ['ADMIN', 'STAFF', 'FAMILY'] },
  { path: '/archives', title: '双维度档案', icon: 'Files', roles: ['ADMIN', 'STAFF', 'ASSESSOR'] }
]

const menus = computed(() => allMenus.filter(m => m.roles.includes(auth.role)))

function onCommand(cmd) {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { height: 100%; }
.aside { background: #1f2d3d; }
.logo {
  height: 60px; display: flex; align-items: center; justify-content: center;
  gap: 8px; color: #fff; font-size: 17px; font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.header {
  background: #fff; display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
.page-title { font-size: 16px; font-weight: 600; }
.user-info { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.main { padding: 16px; overflow-y: auto; }
</style>
