import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { RoleCode } from '@/types'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  userId: string | null
  organizationId: string | null
  roleCode: RoleCode | null
  fullName: string | null
  isAuthenticated: boolean

  setTokens: (accessToken: string, refreshToken: string) => void
  setUser: (userId: string, organizationId: string, roleCode: RoleCode, fullName: string) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      userId: null,
      organizationId: null,
      roleCode: null,
      fullName: null,
      isAuthenticated: false,

      setTokens: (accessToken, refreshToken) =>
        set({ accessToken, refreshToken }),

      setUser: (userId, organizationId, roleCode, fullName) =>
        set({ userId, organizationId, roleCode, fullName, isAuthenticated: true }),

      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          userId: null,
          organizationId: null,
          roleCode: null,
          fullName: null,
          isAuthenticated: false,
        }),
    }),
    {
      name: 'adisker-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        userId: state.userId,
        organizationId: state.organizationId,
        roleCode: state.roleCode,
        fullName: state.fullName,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)
