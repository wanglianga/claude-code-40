import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'elderly', component: () => import('../views/ElderlyList.vue'), meta: { title: '老人档案', roles: ['ADMIN', 'STAFF', 'ASSESSOR', 'FAMILY'] } },
      { path: 'elderly/:id', component: () => import('../views/ElderlyDetail.vue'), meta: { title: '老人档案详情', roles: ['ADMIN', 'STAFF', 'ASSESSOR', 'FAMILY'] } },
      { path: 'assessments', component: () => import('../views/AssessmentList.vue'), meta: { title: '入户评估', roles: ['ADMIN', 'STAFF', 'ASSESSOR'] } },
      { path: 'devices', component: () => import('../views/DeviceStock.vue'), meta: { title: '辅具库存', roles: ['ADMIN', 'STAFF', 'WAREHOUSE'] } },
      { path: 'rentals', component: () => import('../views/RentalList.vue'), meta: { title: '租赁订单', roles: ['ADMIN', 'STAFF', 'WAREHOUSE', 'FAMILY'] } },
      { path: 'feedback', component: () => import('../views/FeedbackList.vue'), meta: { title: '使用反馈', roles: ['ADMIN', 'STAFF', 'FAMILY'] } },
      { path: 'repairs', component: () => import('../views/RepairList.vue'), meta: { title: '维修管理', roles: ['ADMIN', 'STAFF', 'WAREHOUSE'] } },
      { path: 'payments', component: () => import('../views/PaymentList.vue'), meta: { title: '费用中心', roles: ['ADMIN', 'STAFF', 'FAMILY'] } },
      { path: 'subsidies', component: () => import('../views/SubsidyList.vue'), meta: { title: '补贴管理', roles: ['ADMIN', 'STAFF', 'FAMILY'] } },
      { path: 'archives', component: () => import('../views/Archives.vue'), meta: { title: '双维度档案', roles: ['ADMIN', 'STAFF', 'ASSESSOR'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) return '/login'
  if (to.path === '/login' && token) return '/dashboard'
  if (to.meta.roles) {
    const user = JSON.parse(localStorage.getItem('user') || 'null')
    if (!user || !to.meta.roles.includes(user.role)) return '/dashboard'
  }
  return true
})

export default router
