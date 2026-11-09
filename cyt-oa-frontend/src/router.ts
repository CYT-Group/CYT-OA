import { createRouter, createWebHistory } from 'vue-router'
import { session } from './lib/session'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('./pages/LoginPage.vue') },
    {
      path: '/',
      component: () => import('./pages/ShellPage.vue'),
      children: [
        { path: '', redirect: () => (session.isAdmin ? '/admin' : '/me/overview') },
        { path: 'admin', name: 'console', component: () => import('./pages/HomePage.vue') },
        { path: 'portal/analytics', name: 'portal-analytics', component: () => import('./pages/portal/PortalAnalyticsPage.vue') },
        { path: 'org', name: 'org', component: () => import('./pages/OrgPage.vue') },
        { path: 'wecom-templates', name: 'wecom-templates', component: () => import('./pages/WecomTemplatePage.vue') },
        { path: 'me/:view', name: 'me', component: () => import('./pages/me/MePage.vue') },
        { path: 'me', redirect: '/me/overview' },
        { path: 'page/hrm-employee', name: 'employees', component: () => import('./pages/EmployeePage.vue') },
        { path: 'page/sys-users', name: 'accounts', component: () => import('./pages/AccountsPage.vue') },
        { path: 'page/:key', name: 'page', component: () => import('./pages/DataPage.vue'), props: true }
      ]
    }
  ]
})

router.beforeEach((to) => {
  if (to.path !== '/login' && !session.token) {
    return { path: '/login', query: { from: to.fullPath } }
  }
  if (to.path === '/login' && session.token) {
    return { path: '/' }
  }
  if (to.name === 'console' && !session.isAdmin) {
    return { path: '/me/overview' }
  }
})

export default router
