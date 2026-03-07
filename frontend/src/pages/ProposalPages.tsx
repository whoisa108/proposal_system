import { useNavigate, useParams } from 'react-router-dom'
import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import ProposalForm from '../components/ProposalForm'
import api from '../api/axios'
import type { Proposal, ProposalFormValues } from '../types'
import axios from 'axios'

// ---- 新增提案 ----
export function NewProposalPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string>('')
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)

  async function handleSubmit(data: ProposalFormValues, file: File | null) {
    setError('')
    setIsSubmitting(true)
    try {
      const formData = new FormData()
      formData.append('data', JSON.stringify(data))
      if (file) formData.append('file', file)
      await api.post('/proposals', formData)
      navigate('/proposals')
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError((err.response?.data as { message?: string })?.message ?? '送出失敗')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="p-6">
      <h1 className="text-lg font-semibold mb-6">新增提案</h1>
      {error && <p className="text-red-600 text-sm mb-4">{error}</p>}
      <ProposalForm onSubmit={handleSubmit} isSubmitting={isSubmitting} />
    </div>
  )
}

// ---- 編輯提案 ----
interface EditProposalPageProps {
  adminMode?: boolean
}

export function EditProposalPage({ adminMode = false }: EditProposalPageProps) {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [error, setError] = useState<string>('')
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false)

  const { data: proposal, isLoading } = useQuery<Proposal | undefined>({
    queryKey: ['proposal', id],
    queryFn: async () => {
      const url = adminMode ? '/admin/proposals' : '/proposals/my'
      const res = await api.get<Proposal[]>(url)
      return res.data.find((p) => p.id === id)
    },
    enabled: !!id,
  })

  async function handleSubmit(data: ProposalFormValues, file: File | null) {
    setError('')
    setIsSubmitting(true)
    try {
      const formData = new FormData()
      formData.append('data', JSON.stringify(data))
      if (file) formData.append('file', file)
      const url = adminMode ? `/admin/proposals/${id}` : `/proposals/${id}`
      await api.put(url, formData)
      navigate(adminMode ? '/admin/proposals' : '/proposals')
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError((err.response?.data as { message?: string })?.message ?? '更新失敗')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) return <div className="p-6 text-sm text-gray-500">載入中...</div>
  if (!proposal) return <div className="p-6 text-sm text-gray-500">找不到提案</div>

  return (
    <div className="p-6">
      <h1 className="text-lg font-semibold mb-6">編輯提案</h1>
      {error && <p className="text-red-600 text-sm mb-4">{error}</p>}
      <ProposalForm defaultValues={proposal} onSubmit={handleSubmit} isSubmitting={isSubmitting} />
    </div>
  )
}
