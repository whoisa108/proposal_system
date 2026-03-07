import { useForm, useFieldArray } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useState } from 'react'
import type { ProposalFormValues, Proposal } from '../types'

const CATEGORIES = ['酷炫點子獎', '卓越影響獎'] as const
const DIRECTIONS = ['綠色製造', '建立責任供應鏈', '打造健康共榮職場', '培育人才', '關懷弱勢'] as const

const schema = z.object({
  category: z.string().min(1, '請選擇點子類別'),
  direction: z.string().min(1, '請選擇點子方向'),
  title: z.string().min(1, '請輸入點子名稱').max(50, '不得超過 50 字'),
  summary: z.string().min(1, '請輸入點子摘要').max(300, '不得超過 300 字'),
  teammates: z
    .array(
      z.object({
        name: z.string().min(1, '請輸入姓名'),
        employeeId: z.string().min(1, '請輸入工號'),
      })
    )
    .max(4, '隊友最多 4 人'),
})

interface Props {
  defaultValues?: Proposal
  onSubmit: (data: ProposalFormValues, file: File | null) => void
  isSubmitting: boolean
}

export default function ProposalForm({ defaultValues, onSubmit, isSubmitting }: Props) {
  const [file, setFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string>('')

  const {
    register,
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<ProposalFormValues>({
    resolver: zodResolver(schema),
    defaultValues: defaultValues
      ? {
          category: defaultValues.category,
          direction: defaultValues.direction,
          title: defaultValues.title,
          summary: defaultValues.summary,
          teammates: defaultValues.teammates,
        }
      : { category: '', direction: '', title: '', summary: '', teammates: [] },
  })

  const { fields, append, remove } = useFieldArray({ control, name: 'teammates' })
  const titleLen = watch('title')?.length ?? 0
  const summaryLen = watch('summary')?.length ?? 0

  function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0]
    if (!f) return
    const name = f.name.toLowerCase()
    if (!name.endsWith('.pdf') && !name.endsWith('.ppt') && !name.endsWith('.pptx')) {
      setFileError('檔案格式只接受 PDF、PPT、PPTX')
      setFile(null)
      return
    }
    if (f.size > 5 * 1024 * 1024) {
      setFileError('檔案大小不得超過 5MB')
      setFile(null)
      return
    }
    setFileError('')
    setFile(f)
  }

  function handleFormSubmit(data: ProposalFormValues) {
    if (!defaultValues && !file) {
      setFileError('請上傳提案檔案')
      return
    }
    onSubmit(data, file)
  }

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-5 max-w-xl">

      {/* 點子類別 */}
      <div>
        <label className="block text-sm mb-1">點子類別</label>
        <select {...register('category')} className="w-full border border-gray-300 rounded px-3 py-2 text-sm">
          <option value="">請選擇</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
        </select>
        {errors.category && <p className="text-red-600 text-xs mt-1">{errors.category.message}</p>}
      </div>

      {/* 點子方向 */}
      <div>
        <label className="block text-sm mb-1">點子方向</label>
        <select {...register('direction')} className="w-full border border-gray-300 rounded px-3 py-2 text-sm">
          <option value="">請選擇</option>
          {DIRECTIONS.map((d) => <option key={d} value={d}>{d}</option>)}
        </select>
        {errors.direction && <p className="text-red-600 text-xs mt-1">{errors.direction.message}</p>}
      </div>

      {/* 點子名稱 */}
      <div>
        <label className="block text-sm mb-1">點子名稱（{titleLen}/50）</label>
        <input
          {...register('title')}
          className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
          placeholder="請輸入點子名稱"
        />
        {errors.title && <p className="text-red-600 text-xs mt-1">{errors.title.message}</p>}
      </div>

      {/* 點子摘要 */}
      <div>
        <label className="block text-sm mb-1">點子摘要（{summaryLen}/300）</label>
        <textarea
          {...register('summary')}
          rows={4}
          className="w-full border border-gray-300 rounded px-3 py-2 text-sm resize-none"
          placeholder="請輸入點子摘要"
        />
        {errors.summary && <p className="text-red-600 text-xs mt-1">{errors.summary.message}</p>}
      </div>

      {/* 提案檔案 */}
      <div>
        <label className="block text-sm mb-1">
          提案檔案（PDF / PPT / PPTX，≤ 5MB）
          {defaultValues && <span className="text-gray-400 ml-1">不換檔可留空</span>}
        </label>
        <input
          type="file"
          accept=".pdf,.ppt,.pptx"
          onChange={handleFileChange}
          className="w-full text-sm border border-gray-300 rounded px-3 py-2"
        />
        {fileError && <p className="text-red-600 text-xs mt-1">{fileError}</p>}
        {defaultValues?.fileName && !file && (
          <p className="text-gray-500 text-xs mt-1">目前檔案：{defaultValues.fileName}</p>
        )}
      </div>

      {/* 隊友 */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <label className="text-sm">隊友（0–4 人）</label>
          {fields.length < 4 && (
            <button
              type="button"
              onClick={() => append({ name: '', employeeId: '' })}
              className="text-sm border border-gray-300 rounded px-2 py-1 hover:bg-gray-50"
            >
              + 新增隊友
            </button>
          )}
        </div>
        {fields.map((field, index) => (
          <div key={field.id} className="flex gap-2 mb-2 items-start">
            <div className="flex-1">
              <input
                {...register(`teammates.${index}.name`)}
                placeholder="姓名"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
              {errors.teammates?.[index]?.name && (
                <p className="text-red-600 text-xs mt-1">{errors.teammates[index]?.name?.message}</p>
              )}
            </div>
            <div className="flex-1">
              <input
                {...register(`teammates.${index}.employeeId`)}
                placeholder="工號"
                className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              />
              {errors.teammates?.[index]?.employeeId && (
                <p className="text-red-600 text-xs mt-1">{errors.teammates[index]?.employeeId?.message}</p>
              )}
            </div>
            <button
              type="button"
              onClick={() => remove(index)}
              className="text-red-600 text-sm px-2 py-2 hover:underline"
            >
              移除
            </button>
          </div>
        ))}
      </div>

      <button
        type="submit"
        disabled={isSubmitting}
        className="bg-gray-900 text-white rounded px-6 py-2 text-sm hover:bg-gray-700 disabled:opacity-50"
      >
        {isSubmitting ? '送出中...' : '送出'}
      </button>
    </form>
  )
}
