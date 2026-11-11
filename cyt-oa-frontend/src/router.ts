import { createRouter, createWebHistory } from 'vue-router'
import { session } from './lib/session'
import { pageConfigs } from './configs/pages'
import { portalMenus } from './configs/portal'
import { hasPortalPermission } from './lib/portal-access'
import { loadPortalRollout, portalEntryEnabled } from './lib/portal-rollout'
import { portalEntryForRoute } from './lib/portal-entry-policy'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 迁入正文和引用块可能仍携带旧门户的阅读地址。
    { path: '/article/:documentId(\\d+)', redirect: to => `/portal/hr-documents/id/${to.params.documentId}/preview` },
    { path: '/hr-doc/:legacySlug', redirect: to => ({ path: '/portal/hr-documents', query: { legacySlug: String(to.params.legacySlug), mode: 'preview' } }) },
    { path: '/login', component: () => import('./pages/LoginPage.vue') },
    {
      path: '/',
      component: () => import('./pages/ShellPage.vue'),
      children: [
        { path: '', redirect: () => (session.canAccessConsole ? session.consoleHome : '/me/overview') },
        { path: 'admin', name: 'console', component: () => import('./pages/HomePage.vue') },
        { path: 'portal/analytics', name: 'portal-analytics', component: () => import('./pages/portal/PortalAnalyticsPage.vue') },
        { path: 'portal', component: () => import('./pages/portal/PortalHubPage.vue') },
        { path: 'portal/roles', component: () => import('./pages/portal/PortalRolesPage.vue') },
        { path: 'portal/rollout', component: () => import('./pages/portal/PortalRolloutPage.vue'), meta: { adminOnly: true } },
        { path: 'portal/unavailable', component: () => import('./pages/portal/PortalEntryUnavailablePage.vue') },
        { path: 'portal/ai', component: () => import('./pages/portal/PortalAiPage.vue') },
        { path: 'portal/api-costs', component: () => import('./pages/portal/PortalAiPage.vue'), props: { costsOnly: true } },
        { path: 'portal/news', component: () => import('./pages/portal/PortalContentPage.vue'), props: { kind: 'news' } },
        { path: 'portal/wiki', component: () => import('./pages/portal/PortalContentPage.vue'), props: { kind: 'wiki' } },
        { path: 'portal/training', component: () => import('./pages/portal/PortalTrainingPage.vue') },
        { path: 'portal/categories', component: () => import('./pages/portal/PortalCategoriesPage.vue') },
        { path: 'portal/hr-documents', component: () => import('./pages/portal/editor/PortalHrDocumentWorkspace.vue'), props: { view: 'list' }, meta: { permission: 'can_edit_hr_documents' } },
        { path: 'portal/hr-documents/new', component: () => import('./pages/portal/editor/PortalHrDocumentWorkspace.vue'), props: { view: 'editor' }, meta: { permission: 'can_edit_hr_documents' } },
        { path: 'portal/hr-documents/id/:documentId(\\d+)/edit', component: () => import('./pages/portal/editor/PortalHrDocumentWorkspace.vue'), props: { view: 'editor' }, meta: { permission: 'can_edit_hr_documents' } },
        { path: 'portal/hr-documents/id/:documentId(\\d+)/preview', component: () => import('./pages/portal/editor/PortalHrDocumentWorkspace.vue'), props: { view: 'preview' }, meta: { permission: 'can_edit_hr_documents' } },
        { path: 'portal/signoff', component: () => import('./pages/portal/PortalSignoffPage.vue') },
        { path: 'portal/brand-docs', component: () => import('./pages/portal/PortalBrandDocsPage.vue') },
        { path: 'portal/contracts', component: () => import('./pages/portal/PortalContractsPage.vue') },
        { path: 'portal/celebrations', component: () => import('./pages/portal/PortalCelebrationsPage.vue') },
        { path: 'portal/employee-info', component: () => import('./pages/portal/PortalCelebrationsPage.vue'), props: { employeesOnly: true } },
        { path: 'portal/resumes', component: () => import('./pages/portal/PortalResumesPage.vue') },
        { path: 'portal/materials', component: () => import('./pages/portal/PortalMaterialsPage.vue') },
        { path: 'portal/import', component: () => import('./pages/portal/PortalImportPage.vue') },
        { path: 'forbidden', component: () => import('./pages/ForbiddenPage.vue') },
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

router.beforeEach(async (to) => {
  if (to.path !== '/login' && !session.token) {
    return { path: '/login', query: { from: to.fullPath } }
  }
  if (to.path === '/login' && session.token) {
    return { path: '/' }
  }
  if (to.name === 'console' && !session.isAdmin) {
    return { path: session.canManagePortal ? '/portal' : '/me/overview' }
  }
  if (typeof to.meta.permission === 'string' && !session.has(to.meta.permission)) return '/forbidden'
  if (to.meta.adminOnly && !session.isAdmin) return '/forbidden'
  const rolloutEntry = portalEntryForRoute(to.path, to.query)
  if (rolloutEntry && !hasPortalPermission(rolloutEntry)) return '/forbidden'
  if (session.canAccessConsole && to.path !== '/login' && to.path !== '/portal/rollout' && to.path !== '/portal/unavailable') {
    try { await loadPortalRollout(true) }
    catch {
      if (!session.token) return { path: '/login', query: { from: to.fullPath } }
      if (rolloutEntry) return { path: '/portal/unavailable', query: { reason: 'load', from: to.fullPath } }
    }
  }
  if (rolloutEntry && !portalEntryEnabled(rolloutEntry.key)) return { path: '/portal/unavailable', query: { from: to.fullPath } }
  if (to.path === '/portal/hr-documents' && /^\d+$/.test(String(to.query.id || ''))) {
    const { id, mode, ...query } = to.query
    return { path: `/portal/hr-documents/id/${id}/${mode === 'preview' ? 'preview' : 'edit'}`, query }
  }
  if (to.path === '/portal/hr-documents' && typeof to.query.legacySlug === 'string') {
    const { legacySlug, mode, ...query } = to.query
    try {
      const { resolveDocumentId } = await import('./pages/portal/editor/bridge/api')
      const id = await resolveDocumentId(legacySlug)
      return { path: `/portal/hr-documents/id/${id}/${mode === 'preview' ? 'preview' : 'edit'}`, query }
    } catch {
      // 已删除或无法解析的旧链接仍回到文档列表，保留搜索词供定位。
      return { path: '/portal/hr-documents', query: { ...query, q: legacySlug } }
    }
  }
  if (to.path.startsWith('/portal')) {
    const matches = portalMenus.flatMap((g) => g.items).filter((item) => (item.to ?? `/page/${item.key}`).split('?')[0] === to.path)
    if (!session.canAccessConsole || (matches.length && !matches.some(hasPortalPermission))) return '/forbidden'
  }
  if (to.name === 'page') {
    const key = String(to.params.key ?? '')
    const config = pageConfigs[key]
    if (config?.permission ? !session.has(config.permission) : !session.isAdmin) return '/forbidden'
  }
})

export default router
