import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import api from '../api/axios'
import { useState } from 'react'
import type { UserInfo } from '../types'
import axios from 'axios'

const schema = z.object({
  employeeId: z.string().min(1, '請輸入工號'),
  password: z.string().min(1, '請輸入密碼'),
})

type FormValues = z.infer<typeof schema>

interface LoginResponse {
  token: string
  employeeId: string
  name: string
  department: string
  role: 'USER' | 'ADMIN'
}

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string>('')

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValues>({
    resolver: zodResolver(schema),
  })

  async function onSubmit(data: FormValues) {
    setError('')
    try {
      const res = await api.post<LoginResponse>('/auth/login', data)
      const { token, ...userInfo } = res.data
      login(token, userInfo as UserInfo)
      navigate(userInfo.role === 'ADMIN' ? '/admin/proposals' : '/proposals')
    } catch (err) {
      if (axios.isAxiosError(err)) {
        setError((err.response?.data as { message?: string })?.message ?? '登入失敗')
      }
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="w-full max-w-sm border border-gray-300 rounded p-8">
        <h1 className="text-xl font-semibold mb-6">登入</h1>

        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <label className="block text-sm mb-1">工號</label>
            <input
              {...register('employeeId')}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              placeholder="請輸入工號"
            />
            {errors.employeeId && <p className="text-red-600 text-xs mt-1">{errors.employeeId.message}</p>}
          </div>

          <div>
            <label className="block text-sm mb-1">密碼</label>
            <input
              {...register('password')}
              type="password"
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm"
              placeholder="請輸入密碼"
            />
            {errors.password && <p className="text-red-600 text-xs mt-1">{errors.password.message}</p>}
          </div>

          {error && <p className="text-red-600 text-sm">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full bg-gray-900 text-white rounded py-2 text-sm hover:bg-gray-700 disabled:opacity-50"
          >
            {isSubmitting ? '登入中...' : '登入'}
          </button>
        </form>

        <p className="text-sm mt-4 text-center">
          還沒有帳號？{' '}
          <Link to="/register" className="underline">立即註冊</Link>
        </p>
      </div>
    </div>
  )
}
