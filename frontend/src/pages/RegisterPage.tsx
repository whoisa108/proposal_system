import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate, Link } from 'react-router-dom'
import api from '../api/axios'
import { useState } from 'react'
import axios from 'axios'

const DEPARTMENTS = ['AAID', 'BSID', 'ICSD', 'TSID', 'PLED', 'PEID'] as const

const schema = z
  .object({
    employeeId: z.string().min(1, '請輸入工號'),
    name: z.string().min(1, '請輸入姓名'),
    department: z.string().min(1, '請選擇部門'),
    password: z
      .string()
      .min(8, '密碼至少 8 個字元')
      .regex(/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,}$/, '密碼需包含英文字母、數字及特殊符號'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: '兩次密碼不一致',
    path: ['confirmPassword'],
  })

type FormValues = z.infer<typeof schema>

export default function RegisterPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string>('')

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  })

  async function onSubmit(data: FormValues) {
    setError('')
    try {
      await api.post('/auth/register', {
        employeeId: data.employeeId,
        name: data.name,
        department: data.department,
        password: data.password,
      })
      navigate('/login')
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError((err.response?.data as { message?: string })?.message ?? '註冊失敗')
      }
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="w-full max-w-sm border border-gray-300 rounded p-8">
        <h1 className="text-xl font-semibold mb-6">註冊帳號</h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm mb-1">工號</label>
            <input {...register('employeeId')} className="w-full border border-gray-300 rounded px-3 py-2 text-sm" />
            {errors.employeeId && <p className="text-red-600 text-xs mt-1">{errors.employeeId.message}</p>}
          </div>

          <div>
            <label className="block text-sm mb-1">姓名</label>
            <input {...register('name')} className="w-full border border-gray-300 rounded px-3 py-2 text-sm" />
            {errors.name && <p className="text-red-600 text-xs mt-1">{errors.name.message}</p>}
          </div>

          <div>
            <label className="block text-sm mb-1">部門</label>
            <select {...register('department')} className="w-full border border-gray-300 rounded px-3 py-2 text-sm">
              <option value="">請選擇部門</option>
              {DEPARTMENTS.map((d) => <option key={d} value={d}>{d}</option>)}
            </select>
            {errors.department && <p className="text-red-600 text-xs mt-1">{errors.department.message}</p>}
          </div>

          <div>
            <label className="block text-sm mb-1">密碼</label>
            <input
              {...register('password')}
              type="password"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
            <p className="text-gray-400 text-xs mt-1">至少 8 字元，需含英文、數字及特殊符號</p>
            {errors.password && <p className="text-red-600 text-xs mt-1">{errors.password.message}</p>}
          </div>

          <div>
            <label className="block text-sm mb-1">確認密碼</label>
            <input
              {...register('confirmPassword')}
              type="password"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
            />
            {errors.confirmPassword && <p className="text-red-600 text-xs mt-1">{errors.confirmPassword.message}</p>}
          </div>

          {error && <p className="text-red-600 text-sm">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-gray-900 text-white rounded py-2 text-sm hover:bg-gray-700 disabled:opacity-50"
          >
            {isSubmitting ? '註冊中...' : '註冊'}
          </button>
        </form>

        <p className="text-sm mt-4 text-center">
          已有帳號？{' '}
          <Link to="/login" className="underline">返回登入</Link>
        </p>
      </div>
    </div>
  )
}
