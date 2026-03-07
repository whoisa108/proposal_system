// ---- 使用者 ----
export interface User {
  id: string
  employeeId: string
  name: string
  department: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

// ---- 登入後存在 localStorage 的使用者資訊 ----
export interface UserInfo {
  employeeId: string
  name: string
  department: string
  role: 'USER' | 'ADMIN'
}

// ---- 隊友 ----
export interface Teammate {
  name: string
  employeeId: string
}

// ---- 提案 ----
export interface Proposal {
  id: string
  proposerId: string
  employeeId: string
  proposerName: string
  department: string
  category: string
  direction: string
  title: string
  summary: string
  fileName: string
  filePath: string
  teammates: Teammate[]
  createdAt: string
  updatedAt: string
}

// ---- 截止時間設定 ----
export interface DeadlineSetting {
  deadline: string | null
  isPassed: boolean
}

// ---- Audit Log ----
export interface AuditLog {
  id: string
  operatorId: string
  operatorName: string
  action: string
  targetId: string
  detail: string
  ip: string
  timestamp: string
}

// ---- 提案表單欄位 ----
export interface ProposalFormValues {
  category: string
  direction: string
  title: string
  summary: string
  teammates: Teammate[]
}
