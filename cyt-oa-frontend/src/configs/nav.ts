import { menuGroups } from './pages'
import type { MenuLeaf } from './types'

/** 二级项默认走通用 CRUD 页，专用组件页面自己在菜单项上写 to */
const leafTarget = (it: MenuLeaf) => it.to ?? `/page/${it.key}`

export interface NavSub {
  title: string
  to: string
}

export interface NavItem {
  key: string
  title: string
  icon: string
  to: string
  /** 计数徽章的键，取 /api/me/summary 返回的同名字段 */
  badge?: string
  children?: NavSub[]
}

export interface NavZone {
  key: string
  title: string
  items: NavItem[]
  /** 仅管理控制台身份可见 */
  adminOnly?: boolean
}

export const navZones: NavZone[] = [
  {
    key: 'me',
    title: '工作台',
    // 这一区每一项在页面里都是一排页签，菜单再列一遍就是同一件事说两次，所以只留一级入口。
    // 管理区不同：它的子项是各自独立的路由，那是真的下级页面。
    items: [
      { key: 'overview', title: '概览', icon: 'home', to: '/me/overview' },
      { key: 'rhythm', title: '大促 · 新品', icon: 'target', to: '/me/rhythm' },
      { key: 'reminder', title: '我的提醒', icon: 'bell', to: '/me/reminder', badge: 'reminder' },
      { key: 'report', title: '我的报告', icon: 'trending-up', to: '/me/report', badge: 'report' },
      { key: 'apply', title: '我要办事', icon: 'clipboard', to: '/me/apply' },
      { key: 'profile', title: '我的资料 · 足迹', icon: 'user', to: '/me/profile' }
    ]
  },
  {
    key: 'team',
    title: '团队协作',
    items: [{ key: 'team', title: '我的团队', icon: 'users', to: '/me/team', badge: 'team' }]
  },
  {
    key: 'admin',
    title: '企业管理',
    adminOnly: true,
    items: [
      { key: 'console', title: '控制台概览', icon: 'trending-up', to: '/admin' },
      ...menuGroups.map((g) => ({
        key: g.group,
        title: g.group,
        icon: g.items[0]?.icon ?? 'folder',
        to: leafTarget(g.items[0]!),
        // 组里只剩一个页面时不再套一层：那一行和父级标题讲的是同一件事
        children:
          g.items.length > 1 ? g.items.map((it) => ({ title: it.title, to: leafTarget(it) })) : undefined
      }))
    ]
  }
]
