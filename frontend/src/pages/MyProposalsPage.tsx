import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import api from '../api/axios'
import { useState } from 'react'
import ConfirmDialog from '../components/ConfirmDialog'
import dayjs from 'dayjs'
import type { Proposal } from '../types'

export default function MyProposalsPage() {
  const queryClient = useQueryClient()
  const [deleteTarget, setDeleteTarget] = useState<Proposal | null>(null)

  const { data: proposals = [], isLoading } = useQuery<Proposal[]>({
    queryKey: ['myProposals'],
    queryFn: () => api.get<Proposal[]>('/proposals/my').then((r) => r.data),
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/proposals/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myProposals'] })
      setDeleteTarget(null)
    },
  })

  if (isLoading) return <div className="p-6 text-sm text-gray-500">載入中...</div>

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-4">
        <h1 className="text-lg font-semibold">我的提案</h1>
        <Link to="/proposals/new" className="text-sm border border-gray-300 rounded px-4 py-2 hover:bg-gray-50">
          + 新增提案
        </Link>
      </div>

      {proposals.length === 0 ? (
        <p className="text-sm text-gray-500">尚無提案</p>
      ) : (
        <table className="w-full text-sm border border-gray-200">
          <thead>
            <tr className="bg-gray-50">
              <th className="border border-gray-200 px-3 py-2 text-left">點子名稱</th>
              <th className="border border-gray-200 px-3 py-2 text-left">類別</th>
              <th className="border border-gray-200 px-3 py-2 text-left">方向</th>
              <th className="border border-gray-200 px-3 py-2 text-left">建立時間</th>
              <th className="border border-gray-200 px-3 py-2 text-left">操作</th>
            </tr>
          </thead>
          <tbody>
            {proposals.map((p) => (
              <tr key={p.id}>
                <td className="border border-gray-200 px-3 py-2">{p.title}</td>
                <td className="border border-gray-200 px-3 py-2">{p.category}</td>
                <td className="border border-gray-200 px-3 py-2">{p.direction}</td>
                <td className="border border-gray-200 px-3 py-2">
                  {dayjs(p.createdAt).format('YYYY-MM-DD HH:mm')}
                </td>
                <td className="border border-gray-200 px-3 py-2">
                  <div className="flex gap-2">
                    <Link to={`/proposals/${p.id}/edit`} className="text-blue-600 hover:underline">編輯</Link>
                    <button onClick={() => setDeleteTarget(p)} className="text-red-600 hover:underline">刪除</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

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
