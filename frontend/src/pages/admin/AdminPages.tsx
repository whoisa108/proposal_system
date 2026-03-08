import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import api from '../../api/axios'
import ConfirmDialog from '../../components/ConfirmDialog'
import dayjs from 'dayjs'
import type { Proposal, User, AuditLog, DeadlineSetting } from '../../types'
import axios from 'axios'

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

// ---- 所有提案管理 ----
export function AdminProposalsPage() {
  const queryClient = useQueryClient()
  const [deleteTarget, setDeleteTarget] = useState<Proposal | null>(null)

  const { data: proposals = [], isLoading } = useQuery<Proposal[]>({
    queryKey: ['adminProposals'],
    queryFn: () => api.get<Proposal[]>('/admin/proposals').then((r) => r.data),
  })

  async function handleDownloadAll() {
    const res = await api.get('/admin/proposals/download-all', { responseType: 'blob' })
    downloadBlob(res.data, 'proposals.zip')
  }

  async function handleDownload(p: Proposal) {
    const res = await api.get(`/admin/proposals/${p.id}/download`, { responseType: 'blob' })
    downloadBlob(res.data, p.fileName ?? p.title)
  }

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/proposals/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminProposals'] })
      setDeleteTarget(null)
    },
  })

  if (isLoading) return <div className="p-6 text-sm text-gray-500">載入中...</div>

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold">所有提案</h1>
        <button
          onClick={handleDownloadAll}
          className="text-sm border border-gray-300 rounded px-4 py-2 hover:bg-gray-50"
        >
          ↓ 下載全部 (ZIP)
        </button>
      </div>
      <table className="w-full text-sm border border-gray-200">
        <thead>
          <tr className="bg-gray-50">
            <th className="border border-gray-200 px-3 py-2 text-left">提案人</th>
            <th className="border border-gray-200 px-3 py-2 text-left">工號</th>
            <th className="border border-gray-200 px-3 py-2 text-left">部門</th>
            <th className="border border-gray-200 px-3 py-2 text-left">點子名稱</th>
            <th className="border border-gray-200 px-3 py-2 text-left">類別</th>
            <th className="border border-gray-200 px-3 py-2 text-left">建立時間</th>
            <th className="border border-gray-200 px-3 py-2 text-left">操作</th>
          </tr>
        </thead>
        <tbody>
          {proposals.map((p) => (
            <tr key={p.id}>
              <td className="border border-gray-200 px-3 py-2">{p.proposerName}</td>
              <td className="border border-gray-200 px-3 py-2">{p.employeeId}</td>
              <td className="border border-gray-200 px-3 py-2">{p.department}</td>
              <td className="border border-gray-200 px-3 py-2">{p.title}</td>
              <td className="border border-gray-200 px-3 py-2">{p.category}</td>
              <td className="border border-gray-200 px-3 py-2">
                {dayjs(p.createdAt).format('YYYY-MM-DD HH:mm')}
              </td>
              <td className="border border-gray-200 px-3 py-2">
                <div className="flex gap-2">
                  <Link to={`/admin/proposals/${p.id}/edit`} className="text-blue-600 hover:underline">編輯</Link>
                  <button onClick={() => handleDownload(p)} className="text-green-600 hover:underline">下載</button>
                  <button onClick={() => setDeleteTarget(p)} className="text-red-600 hover:underline">刪除</button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {deleteTarget && (
        <ConfirmDialog
          message={`確定要刪除「${deleteTarget.title}」嗎？`}
          onConfirm={() => deleteMutation.mutate(deleteTarget.id)}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}

// ---- 使用者管理 ----
export function AdminUsersPage() {
  const queryClient = useQueryClient()
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null)

  const { data: users = [], isLoading } = useQuery<User[]>({
    queryKey: ['adminUsers'],
    queryFn: () => api.get<User[]>('/admin/users').then((r) => r.data),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/admin/users/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['adminUsers'] })
      setDeleteTarget(null)
    },
  })

  if (isLoading) return <div className="p-6 text-sm text-gray-500">載入中...</div>

  return (
    <div className="p-6">
      <h1 className="text-lg font-semibold mb-4">使用者管理</h1>
      <table className="w-full text-sm border border-gray-200">
        <thead>
          <tr className="bg-gray-50">
            <th className="border border-gray-200 px-3 py-2 text-left">姓名</th>
            <th className="border border-gray-200 px-3 py-2 text-left">工號</th>
            <th className="border border-gray-200 px-3 py-2 text-left">部門</th>
            <th className="border border-gray-200 px-3 py-2 text-left">角色</th>
            <th className="border border-gray-200 px-3 py-2 text-left">操作</th>
          </tr>
        </thead>
        <tbody>
          {users.map((u) => (
            <tr key={u.id}>
              <td className="border border-gray-200 px-3 py-2">{u.name}</td>
              <td className="border border-gray-200 px-3 py-2">{u.employeeId}</td>
              <td className="border border-gray-200 px-3 py-2">{u.department}</td>
              <td className="border border-gray-200 px-3 py-2">{u.role}</td>
              <td className="border border-gray-200 px-3 py-2">
                {u.role !== 'ADMIN' && (
                  <button onClick={() => setDeleteTarget(u)} className="text-red-600 hover:underline text-sm">
                    刪除
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {deleteTarget && (
        <ConfirmDialog
          message={`確定要刪除使用者「${deleteTarget.name}」嗎？`}
          onConfirm={() => deleteMutation.mutate(deleteTarget.id)}
          onCancel={() => setDeleteTarget(null)}
        />
      )}
    </div>
  )
}

// ---- 截止時間設定 ----
interface DeadlineForm {
  deadline: string
}

export function AdminDeadlinePage() {
  const [message, setMessage] = useState<string>('')

  const { data } = useQuery<DeadlineSetting>({
    queryKey: ['deadline'],
    queryFn: () => api.get<DeadlineSetting>('/settings/deadline').then((r) => r.data),
  })

  const { register, handleSubmit } = useForm<DeadlineForm>()

  async function onSubmit(formData: DeadlineForm) {
    try {
      await api.put('/admin/deadline', { deadline: new Date(formData.deadline).toISOString() })
      setMessage('截止時間已更新')
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setMessage((err.response?.data as { message?: string })?.message ?? '更新失敗')
      }
    }
  }

  const currentDeadline = data?.deadline ? dayjs(data.deadline).format('YYYY-MM-DDTHH:mm') : ''

  return (
    <div className="p-6 max-w-sm">
      <h1 className="text-lg font-semibold mb-4">截止時間設定</h1>
      {data?.deadline && (
        <p className="text-sm text-gray-600 mb-4">
          目前截止時間：{dayjs(data.deadline).format('YYYY-MM-DD HH:mm')}
          {data.isPassed && <span className="text-red-600 ml-2">（已截止）</span>}
        </p>
      )}
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-sm mb-1">新截止時間</label>
          <input
            {...register('deadline', { required: true })}
            type="datetime-local"
            defaultValue={currentDeadline}
            className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
          />
        </div>
        <button type="submit" className="bg-gray-900 text-white rounded px-6 py-2 text-sm hover:bg-gray-700">
          儲存
        </button>
        {message && <p className="text-sm text-gray-700">{message}</p>}
      </form>
    </div>
  )
}

// ---- Audit Log ----
export function AdminAuditLogPage() {
  const { data: logs = [], isLoading } = useQuery<AuditLog[]>({
    queryKey: ['auditLogs'],
    queryFn: () => api.get<AuditLog[]>('/admin/audit-logs').then((r) => r.data),
  })

  if (isLoading) return <div className="p-6 text-sm text-gray-500">載入中...</div>

  return (
    <div className="p-6">
      <h1 className="text-lg font-semibold mb-4">Audit Log</h1>
      <table className="w-full text-sm border border-gray-200">
        <thead>
          <tr className="bg-gray-50">
            <th className="border border-gray-200 px-3 py-2 text-left">時間</th>
            <th className="border border-gray-200 px-3 py-2 text-left">操作人</th>
            <th className="border border-gray-200 px-3 py-2 text-left">工號</th>
            <th className="border border-gray-200 px-3 py-2 text-left">動作</th>
            <th className="border border-gray-200 px-3 py-2 text-left">目標 ID</th>
            <th className="border border-gray-200 px-3 py-2 text-left">IP</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log) => (
            <tr key={log.id}>
              <td className="border border-gray-200 px-3 py-2">
                {dayjs(log.timestamp).format('YYYY-MM-DD HH:mm:ss')}
              </td>
              <td className="border border-gray-200 px-3 py-2">{log.operatorName}</td>
              <td className="border border-gray-200 px-3 py-2">{log.operatorId}</td>
              <td className="border border-gray-200 px-3 py-2">{log.action}</td>
              <td className="border border-gray-200 px-3 py-2 text-xs text-gray-500">{log.targetId}</td>
              <td className="border border-gray-200 px-3 py-2">{log.ip}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
