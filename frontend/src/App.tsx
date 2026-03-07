import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider } from './context/AuthContext'
import { RequireAuth, RequireAdmin } from './components/RouteGuard'
import Navbar from './components/Navbar'
import type { ReactNode } from 'react'

import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import MyProposalsPage from './pages/MyProposalsPage'
import { NewProposalPage, EditProposalPage } from './pages/ProposalPages'
import {
  AdminProposalsPage,
  AdminUsersPage,
  AdminDeadlinePage,
  AdminAuditLogPage,
} from './pages/admin/AdminPages'

const queryClient = new QueryClient()

function Layout({ children }: { children: ReactNode }) {
  return (
    <div>
      <Navbar />
      <main>{children}</main>
    </div>
  )
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* Public */}
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />

            {/* USER */}
            <Route path="/proposals" element={
              <RequireAuth><Layout><MyProposalsPage /></Layout></RequireAuth>
            } />
            <Route path="/proposals/new" element={
              <RequireAuth><Layout><NewProposalPage /></Layout></RequireAuth>
            } />
            <Route path="/proposals/:id/edit" element={
              <RequireAuth><Layout><EditProposalPage /></Layout></RequireAuth>
            } />

            {/* ADMIN */}
            <Route path="/admin/proposals" element={
              <RequireAdmin><Layout><AdminProposalsPage /></Layout></RequireAdmin>
            } />
            <Route path="/admin/proposals/:id/edit" element={
              <RequireAdmin><Layout><EditProposalPage adminMode={true} /></Layout></RequireAdmin>
            } />
            <Route path="/admin/users" element={
              <RequireAdmin><Layout><AdminUsersPage /></Layout></RequireAdmin>
            } />
            <Route path="/admin/deadline" element={
              <RequireAdmin><Layout><AdminDeadlinePage /></Layout></RequireAdmin>
            } />
            <Route path="/admin/audit-logs" element={
              <RequireAdmin><Layout><AdminAuditLogPage /></Layout></RequireAdmin>
            } />

            {/* Default */}
            <Route path="/" element={<Navigate to="/login" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
