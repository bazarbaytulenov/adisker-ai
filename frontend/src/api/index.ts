import api from './client'
import type {
  ApiResponse, PageResponse, TokenResponse,
  Organization, Branch, User, Group, Child,
  AttendanceSheet, Cyclogram, DailyPost,
  ProspectivePlan, PlanSection, ScheduleData,
  ObservationData, IndividualCard,
} from '@/types'

// ─── Auth ────────────────────────────────────────────────────────────────────
export const authApi = {
  login: (email: string, password: string) =>
    api.post<ApiResponse<TokenResponse>>('/auth/login', { email, password }),

  refresh: (refreshToken: string) =>
    api.post<ApiResponse<TokenResponse>>('/auth/refresh', { refreshToken }),

  logout: () => api.post<ApiResponse<void>>('/auth/logout'),

  me: () => api.get<ApiResponse<User>>('/users/me'),
}

// ─── Organizations ────────────────────────────────────────────────────────────
export const orgApi = {
  list: (page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<Organization>>>('/organizations', { params: { page, size } }),

  get: (id: string) =>
    api.get<ApiResponse<Organization>>(`/organizations/${id}`),

  create: (data: Partial<Organization>) =>
    api.post<ApiResponse<Organization>>('/organizations', data),

  update: (id: string, data: Partial<Organization>) =>
    api.put<ApiResponse<Organization>>(`/organizations/${id}`, data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/organizations/${id}`),
}

// ─── Branches ────────────────────────────────────────────────────────────────
export const branchApi = {
  list: (organizationId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<Branch>>>('/branches', { params: { organizationId, page, size } }),

  listActive: (organizationId: string) =>
    api.get<ApiResponse<Branch[]>>('/branches/active', { params: { organizationId } }),

  get: (id: string, organizationId: string) =>
    api.get<ApiResponse<Branch>>(`/branches/${id}`, { params: { organizationId } }),

  create: (organizationId: string, data: Partial<Branch>) =>
    api.post<ApiResponse<Branch>>('/branches', data, { params: { organizationId } }),

  update: (id: string, organizationId: string, data: Partial<Branch>) =>
    api.put<ApiResponse<Branch>>(`/branches/${id}`, data, { params: { organizationId } }),

  delete: (id: string, organizationId: string) =>
    api.delete<ApiResponse<void>>(`/branches/${id}`, { params: { organizationId } }),
}

// ─── Users ────────────────────────────────────────────────────────────────────
export const userApi = {
  list: (organizationId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<User>>>('/users', { params: { organizationId, page, size } }),

  get: (id: string) =>
    api.get<ApiResponse<User>>(`/users/${id}`),

  create: (data: Partial<User> & { password: string }) =>
    api.post<ApiResponse<User>>('/users', data),

  update: (id: string, data: Partial<User>) =>
    api.put<ApiResponse<User>>(`/users/${id}`, data),

  deactivate: (id: string) =>
    api.patch<ApiResponse<void>>(`/users/${id}/deactivate`),
}

// ─── Groups ───────────────────────────────────────────────────────────────────
export const groupApi = {
  list: (organizationId: string, branchId: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<Group>>>('/groups', { params: { organizationId, branchId, page, size } }),

  get: (id: string, organizationId: string) =>
    api.get<ApiResponse<Group>>(`/groups/${id}`, { params: { organizationId } }),

  create: (data: Partial<Group>) =>
    api.post<ApiResponse<Group>>('/groups', data),

  update: (id: string, data: Partial<Group>) =>
    api.put<ApiResponse<Group>>(`/groups/${id}`, data),

  delete: (id: string) =>
    api.delete<ApiResponse<void>>(`/groups/${id}`),
}

// ─── Children ─────────────────────────────────────────────────────────────────
export const childApi = {
  list: (organizationId: string, branchId?: string, groupId?: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<Child>>>('/children', {
      params: { organizationId, branchId, groupId, page, size },
    }),

  get: (id: string) =>
    api.get<ApiResponse<Child>>(`/children/${id}`),

  create: (data: Partial<Child>) =>
    api.post<ApiResponse<Child>>('/children', data),

  update: (id: string, data: Partial<Child>) =>
    api.put<ApiResponse<Child>>(`/children/${id}`, data),

  discharge: (id: string, data: { dischargeDate: string; reason: string; orderNum?: string }) =>
    api.patch<ApiResponse<Child>>(`/children/${id}/discharge`, data),
}

// ─── Attendance ───────────────────────────────────────────────────────────────
export const attendanceApi = {
  getSheet: (organizationId: string, branchId: string, groupId: string, year: number, month: number) =>
    api.get<ApiResponse<AttendanceSheet>>('/attendance/sheet', {
      params: { organizationId, branchId, groupId, year, month },
    }),

  setMark: (monthId: string, organizationId: string, childId: string, day: number, mark: string | null) =>
    api.patch<ApiResponse<void>>(`/attendance/mark/${monthId}`, { childId, day, mark }, {
      params: { organizationId },
    }),

  closeMonth: (monthId: string) =>
    api.patch<ApiResponse<void>>(`/attendance/close/${monthId}`),
}

// ─── Cyclogram ────────────────────────────────────────────────────────────────
export const cyclogramApi = {
  list: (organizationId: string, branchId?: string, groupId?: string) =>
    api.get<ApiResponse<Cyclogram[]>>('/cyclograms', {
      params: { organizationId, branchId, groupId },
    }),

  get: (id: string, organizationId: string) =>
    api.get<ApiResponse<Cyclogram>>(`/cyclograms/${id}`, { params: { organizationId } }),

  create: (data: {
    organizationId: string
    branchId: string
    groupId: string
    academicYear: string
    month: number
    week: number
    language?: string
    content?: string
  }) =>
    api.post<ApiResponse<Cyclogram>>('/cyclograms', data),

  update: (id: string, data: {
    organizationId: string
    content?: string
    month?: number
    week?: number
    language?: string
    status?: string
  }) =>
    api.put<ApiResponse<Cyclogram>>(`/cyclograms/${id}`, data),

  approve: (id: string, organizationId: string) =>
    api.patch<ApiResponse<Cyclogram>>(`/cyclograms/${id}/approve`, null, { params: { organizationId } }),

  delete: (id: string, organizationId: string) =>
    api.delete<ApiResponse<void>>(`/cyclograms/${id}`, { params: { organizationId } }),
}

// ─── DailyInfo ────────────────────────────────────────────────────────────────
export const dailyPostApi = {
  getOrCreate: (organizationId: string, branchId: string, groupId: string, date?: string) =>
    api.get<ApiResponse<DailyPost>>('/daily-posts/today', {
      params: { organizationId, branchId, groupId, date },
    }),

  list: (groupId: string, from: string, to: string) =>
    api.get<ApiResponse<DailyPost[]>>('/daily-posts', { params: { groupId, from, to } }),

  listByBranch: (organizationId: string, branchId: string, date: string) =>
    api.get<ApiResponse<DailyPost[]>>('/daily-posts/branch', {
      params: { organizationId, branchId, date },
    }),

  save: (
    id: string,
    organizationId: string,
    data: { branchId: string; groupId: string; postDate: string; theme?: string; description?: string; homeTasks?: string }
  ) =>
    api.put<ApiResponse<DailyPost>>(`/daily-posts/${id}`, data, { params: { organizationId } }),

  publish: (id: string, organizationId: string) =>
    api.patch<ApiResponse<DailyPost>>(`/daily-posts/${id}/publish`, null, { params: { organizationId } }),

  unpublish: (id: string, organizationId: string) =>
    api.patch<ApiResponse<DailyPost>>(`/daily-posts/${id}/unpublish`, null, { params: { organizationId } }),

  delete: (id: string, organizationId: string) =>
    api.delete<ApiResponse<void>>(`/daily-posts/${id}`, { params: { organizationId } }),
}

// ─── Plan (Перспективный план) ─────────────────────────────────────────────────
export const planApi = {
  getOrCreate: (
    organizationId: string,
    branchId: string,
    groupId: string,
    year: string,
    month: number,
    week: number,
    language = 'ru'
  ) =>
    api.get<ApiResponse<ProspectivePlan>>('/plans', {
      params: { organizationId, branchId, groupId, year, month, week, language },
    }),

  list: (organizationId: string, groupId: string, year: string, language = 'ru') =>
    api.get<ApiResponse<ProspectivePlan[]>>('/plans/list', {
      params: { organizationId, groupId, year, language },
    }),

  getSections: (planId: string) =>
    api.get<ApiResponse<PlanSection[]>>(`/plans/${planId}/sections`),

  saveSection: (
    planId: string,
    organizationId: string,
    data: {
      domain: string
      domainNameRu?: string
      domainNameKk?: string
      ownerRole?: string
      content?: string
      objectives?: string
      materials?: string
      sortOrder?: number
    }
  ) =>
    api.put<ApiResponse<PlanSection>>(`/plans/${planId}/sections`, data, {
      params: { organizationId },
    }),

  submitSection: (sectionId: string, organizationId: string) =>
    api.patch<ApiResponse<PlanSection>>(`/plans/sections/${sectionId}/submit`, null, {
      params: { organizationId },
    }),

  approveSection: (sectionId: string, organizationId: string) =>
    api.patch<ApiResponse<PlanSection>>(`/plans/sections/${sectionId}/approve`, null, {
      params: { organizationId },
    }),

  returnSection: (sectionId: string, comment: string) =>
    api.patch<ApiResponse<PlanSection>>(`/plans/sections/${sectionId}/return`, { comment }),

  lockSection: (sectionId: string) =>
    api.post<ApiResponse<void>>(`/plans/sections/${sectionId}/lock`),

  unlockSection: (sectionId: string) =>
    api.delete<ApiResponse<void>>(`/plans/sections/${sectionId}/lock`),
}

// ─── Schedule (Расписание) ─────────────────────────────────────────────────────
export const scheduleApi = {
  get: (organizationId: string, branchId: string, groupId: string, academicYear: string, language = 'ru') =>
    api.get<ApiResponse<ScheduleData>>('/schedules', {
      params: { organizationId, branchId, groupId, academicYear, language },
    }),

  saveEntries: (
    id: string,
    organizationId: string,
    entries: Array<{
      dayOfWeek: number
      startTime: string
      endTime?: string
      subject: string
      educatorRole?: string
      educatorId?: string
      notes?: string
    }>
  ) =>
    api.put<ApiResponse<ScheduleData>>(`/schedules/${id}/entries`, entries, {
      params: { organizationId },
    }),

  publish: (id: string, organizationId: string) =>
    api.patch<ApiResponse<ScheduleData>>(`/schedules/${id}/publish`, null, {
      params: { organizationId },
    }),
}

// ─── Observation (Наблюдение за детьми) ──────────────────────────────────────
export const observationApi = {
  getOrCreate: (
    organizationId: string,
    branchId: string,
    groupId: string,
    childId: string,
    period: string,
    academicYear: string
  ) =>
    api.get<ApiResponse<ObservationData>>('/observations', {
      params: { organizationId, branchId, groupId, childId, period, academicYear },
    }),

  setResult: (
    observationId: string,
    indicatorId: string,
    level: string | null
  ) =>
    api.patch<ApiResponse<void>>(`/observations/${observationId}/result`, {
      indicatorId,
      level,
    }),

  getCard: (childId: string, observationId: string) =>
    api.get<ApiResponse<IndividualCard>>('/observations/card', {
      params: { childId, observationId },
    }),

  saveCard: (
    childId: string,
    observationId: string,
    data: {
      gameName?: string
      gameObjectives?: string
      gameProcedure?: string
      customNotes?: string
      language?: string
    }
  ) =>
    api.post<ApiResponse<IndividualCard>>('/observations/card', data, {
      params: { childId, observationId },
    }),
}
