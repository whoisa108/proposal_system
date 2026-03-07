import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useQuery } from '@tanstack/react-query'
import api from '../api/axios'
import dayjs from 'dayjs'
import duration from 'dayjs/plugin/duration'
import { useEffect, useState } from 'react'
import type { DeadlineSetting } from '../types'

dayjs.extend(duration)

function Countdown({ deadline }: { deadline: string }) {
  const [remaining, setRemaining] = useState<string>('')

  useEffect(() => {
    function update() {
      const diff = dayjs(deadline).diff(dayjs())
      if (diff <= 0) {
        setRemaining('已截止')
        return
      }
      const d = dayjs.duration(diff)
      setRemaining(
        `${d.days()} 天 ${d.hours()} 時 ${d.minutes()} 分 ${d.seconds()} 秒`
      )
    }
    update()
    const timer = setInterval(update, 1000)
    return () => clearInterval(timer)
  }, [deadline])

  return <span className="text-sm text-gray-600">提案截止倒數：{remaining}</span>
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const { data } = useQuery<DeadlineSetting>({
    queryKey: ['deadline'],
    queryFn: () => api.get<DeadlineSetting>('/settings/deadline').then((r) => r.data),
    refetchInterval: 60000,
  })

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <nav className="border-b border-gray-200 px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-6">
        <span className="font-semibold text-gray-900">ESG 點子競賽</span>
        {user?.role === 'USER' && (
          <>
            <Link to="/proposals" className="text-sm text-gray-700 hover:underline">我的提案</Link>
            <Link to="/proposals/new" className="text-sm text-gray-700 hover:underline">新增提案</Link>
          </>
        )}
        {user?.role === 'ADMIN' && (
          <>
            <Link to="/admin/proposals" className="text-sm text-gray-700 hover:underline">所有提案</Link>
            <Link to="/admin/users" className="text-sm text-gray-700 hover:underline">使用者管理</Link>
            <Link to="/admin/deadline" className="text-sm text-gray-700 hover:underline">截止時間</Link>
            <Link to="/admin/audit-logs" className="text-sm text-gray-700 hover:underline">Audit Log</Link>
          </>
        )}
      </div>
      <div className="flex items-center gap-4">
        {data?.deadline && !data.isPassed && <Countdown deadline={data.deadline} />}
        {data?.isPassed && (
          <span className="text-sm text-red-600 font-medium">提案已截止</span>
        )}
        {user && (
          <div className="flex items-center gap-3">
            <span className="text-sm text-gray-600">{user.name}（{user.employeeId}）</span>
            <button
              onClick={handleLogout}
              className="text-sm border border-gray-300 rounded px-3 py-1 hover:bg-gray-50"
            >
              登出
            </button>
          </div>
        )}
      </div>
    </nav>
  )
}
