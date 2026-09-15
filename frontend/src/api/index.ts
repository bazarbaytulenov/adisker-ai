import api from './client'
import type {
  ApiResponse, PageResponse, TokenResponse,
  Organization, Branch, User, Group, Child,
  AttendanceSheet, Cyclogram, DailyPost,
  ProspectivePlan, PlanSection, ScheduleData,
  ObservationData, IndividualCard, MethodistSummaryData,
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

  assignableRoles: () =>
    api.get<ApiResponse<string[]>>('/users/assignable-roles'),

  resetPassword: (id: string, password: string) =>
    api.patch<ApiResponse<void>>(`/users/${id}/reset-password`, { password }),

  orgAdmins: (organizationId: string) =>
    api.get<ApiResponse<User[]>>('/users/org-admins', { params: { organizationId } }),

  changeMyPassword: (currentPassword: string, newPassword: string) =>
    api.patch<ApiResponse<void>>('/users/me/password', { currentPassword, newPassword }),
}

// ─── Groups ───────────────────────────────────────────────────────────────────
export const groupApi = {
  list: (organizationId: string, branchId?: string, page = 0, size = 20) =>
    api.get<ApiResponse<PageResponse<Group>>>('/groups', { params: { organizationId, ...(branchId ? { branchId } : {}), page, size } }),

  byBranch: (branchId: string) =>
    api.get<ApiResponse<Group[]>>(`/groups/by-branch/${branchId}`),

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
  getSheet: (organizationId: string, branchId: string | undefined, groupId: string, year: number, month: number) =>
    api.get<ApiResponse<AttendanceSheet>>('/attendance/sheet', {
      params: { organizationId, ...(branchId ? { branchId } : {}), groupId, year, month },
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

// ─── Methodist Summary ────────────────────────────────────────────────────────
export const methodistSummaryApi = {
  getSummary: (
    organizationId: string,
    branchId: string,
    groupId: string | null,
    period: string,
    academicYear: string
  ) =>
    api.get<ApiResponse<MethodistSummaryData>>('/methodist-summary', {
      params: { organizationId, branchId, ...(groupId ? { groupId } : {}), period, academicYear },
    }),

  recalculate: (
    organizationId: string,
    branchId: string,
    groupId: string | null,
    period: string,
    academicYear: string
  ) =>
    api.post<ApiResponse<MethodistSummaryData>>('/methodist-summary/recalculate', null, {
      params: { organizationId, branchId, ...(groupId ? { groupId } : {}), period, academicYear },
    }),
}

// ─── Payments (P1-1) ────────────────────────────────────────────────────────
export const paymentApi = {
  createCharge: (organizationId: string, data: any) =>
    api.post('/payments/charges', data, { params: { organizationId } }),
  generate: (organizationId: string, groupId: string, year: number, month: number) =>
    api.post('/payments/charges/generate', null, { params: { organizationId, groupId, year, month } }),
  chargesByChild: (organizationId: string, childId: string) =>
    api.get('/payments/charges', { params: { organizationId, childId } }),
  kaspi: (organizationId: string, chargeId: string) =>
    api.get(`/payments/charges/${chargeId}/kaspi`, { params: { organizationId } }),
  pay: (organizationId: string, data: any) =>
    api.post('/payments', data, { params: { organizationId } }),
  confirm: (organizationId: string, id: string) =>
    api.patch(`/payments/${id}/confirm`, null, { params: { organizationId } }),
  cancel: (organizationId: string, id: string) =>
    api.patch(`/payments/${id}/cancel`, null, { params: { organizationId } }),
  registry: (organizationId: string, status = 'pending') =>
    api.get('/payments/registry', { params: { organizationId, status } }),
}

// ─── Protocols (P1-5) ─────────────────────────────────────────────────────────
export const protocolApi = {
  list: (organizationId: string, branchId?: string, type?: string) =>
    api.get('/protocols', { params: { organizationId, branchId, type } }),
  get: (organizationId: string, id: string) =>
    api.get(`/protocols/${id}`, { params: { organizationId } }),
  create: (organizationId: string, data: any) =>
    api.post('/protocols', data, { params: { organizationId } }),
  update: (organizationId: string, id: string, data: any) =>
    api.put(`/protocols/${id}`, data, { params: { organizationId } }),
  delete: (organizationId: string, id: string) =>
    api.delete(`/protocols/${id}`, { params: { organizationId } }),
  exportWordUrl: (organizationId: string, id: string) =>
    `/api/protocols/${id}/export/word?organizationId=${organizationId}`,
}

// ─── Nomenclature (P2-6) ──────────────────────────────────────────────────────
export const nomenclatureApi = {
  list: (organizationId: string, search?: string) =>
    api.get('/nomenclature', { params: { organizationId, search } }),
  create: (organizationId: string, data: any) =>
    api.post('/nomenclature', data, { params: { organizationId } }),
  update: (organizationId: string, id: string, data: any) =>
    api.put(`/nomenclature/${id}`, data, { params: { organizationId } }),
  delete: (organizationId: string, id: string) =>
    api.delete(`/nomenclature/${id}`, { params: { organizationId } }),
}

// ─── Routine — режим дня (P1-6) ────────────────────────────────────────────────
export const routineApi = {
  getOrCreate: (organizationId: string, branchId: string, groupId: string, year: string, language = 'ru') =>
    api.get('/routines', { params: { organizationId, branchId, groupId, year, language } }),
  save: (organizationId: string, id: string, data: any) =>
    api.put(`/routines/${id}`, data, { params: { organizationId } }),
  applyTemplate: (organizationId: string, id: string) =>
    api.patch(`/routines/${id}/template`, null, { params: { organizationId } }),
  publish: (organizationId: string, id: string) =>
    api.patch(`/routines/${id}/publish`, null, { params: { organizationId } }),
}

// ─── Annual / Monthly plans (P1-4) ─────────────────────────────────────────────
export const annualPlanApi = {
  getOrCreate: (organizationId: string, branchId: string, year: string, language = 'ru') =>
    api.get('/annual-plans', { params: { organizationId, branchId, year, language } }),
  addSection: (organizationId: string, planId: string, data: any) =>
    api.post(`/annual-plans/${planId}/sections`, data, { params: { organizationId } }),
  sections: (organizationId: string, planId: string) =>
    api.get(`/annual-plans/${planId}/sections`, { params: { organizationId } }),
  addEvent: (organizationId: string, sectionId: string, data: any) =>
    api.post(`/annual-plans/sections/${sectionId}/events`, data, { params: { organizationId } }),
  propagate: (organizationId: string, planId: string, year: number, month: number) =>
    api.post(`/annual-plans/${planId}/propagate`, null, { params: { organizationId, year, month } }),
  monthly: (organizationId: string, branchId: string) =>
    api.get('/annual-plans/monthly', { params: { organizationId, branchId } }),
}

// ─── Manager orders (P2-1) ──────────────────────────────────────────────────────
export const managerApi = {
  list: (organizationId: string, branchId?: string, type?: string) =>
    api.get('/manager-orders', { params: { organizationId, branchId, type } }),
  create: (organizationId: string, data: any) =>
    api.post('/manager-orders', data, { params: { organizationId } }),
  sign: (organizationId: string, id: string) =>
    api.patch(`/manager-orders/${id}/sign`, null, { params: { organizationId } }),
}

// ─── Janitor (P2-2) ─────────────────────────────────────────────────────────────
export const janitorApi = {
  list: (organizationId: string, branchId?: string, type?: string) =>
    api.get('/janitor-records', { params: { organizationId, branchId, type } }),
  create: (organizationId: string, data: any) =>
    api.post('/janitor-records', data, { params: { organizationId } }),
  delete: (organizationId: string, id: string) =>
    api.delete(`/janitor-records/${id}`, { params: { organizationId } }),
}

// ─── Parent invitations & cabinet (P1-2) ────────────────────────────────────────
export const parentApi = {
  createInvitation: (data: any) => api.post('/parent-invitations', data),
  listInvitations: () => api.get('/parent-invitations'),
  revokeInvitation: (id: string) => api.patch(`/parent-invitations/${id}/revoke`),
  myChildren: () => api.get('/parent/children'),
  register: (data: any) => api.post('/auth/register-parent', data),
}

// ─── AI (P2-3) ───────────────────────────────────────────────────────────────────
export const aiApi = {
  status: () => api.get('/ai/status'),
  cyclogram: (data: any) => api.post('/ai/cyclogram', data),
  recommendations: (data: any) => api.post('/ai/recommendations', data),
  text: (prompt: string) => api.post('/ai/text', { prompt }),
}

// ─── Audit & Backup (P0-3, P3-1) ──────────────────────────────────────────────────
export const auditApi = {
  list: (action?: string, entityType?: string, page = 0, size = 50) =>
    api.get('/audit-logs', { params: { action, entityType, page, size } }),
}
export const backupApi = {
  exportUrl: (organizationId: string) => `/api/backup/export?organizationId=${organizationId}`,
}

// ─── Medical journals ────────────────────────────────────────────────────────
export const medicalApi = {
  list: (organizationId: string, branchId: string, page = 0, size = 20) =>
    api.get('/medical', { params: { organizationId, branchId, page, size } }),
  create: (data: any) => api.post('/medical', data),
  update: (id: string, data: any) => api.put(`/medical/${id}`, data),
  saveData: (id: string, data: string) => api.patch(`/medical/${id}/data`, { data }),
  delete: (id: string) => api.delete(`/medical/${id}`),
}

// ─── Material assets (МТБ) ────────────────────────────────────────────────────
export const assetApi = {
  list: (organizationId: string, branchId?: string, page = 0, size = 20) =>
    api.get('/assets', { params: { organizationId, branchId, page, size } }),
  create: (data: any) => api.post('/assets', data),
  update: (id: string, data: any) => api.put(`/assets/${id}`, data),
  delete: (id: string) => api.delete(`/assets/${id}`),
}

// ─── Library fund ─────────────────────────────────────────────────────────────
export const libraryApi = {
  list: (organizationId: string, branchId?: string, page = 0, size = 20) =>
    api.get('/library', { params: { organizationId, branchId, page, size } }),
  create: (data: any) => api.post('/library', data),
  update: (id: string, data: any) => api.put(`/library/${id}`, data),
  delete: (id: string) => api.delete(`/library/${id}`),
}

// ─── Chat ──────────────────────────────────────────────────────────────────────
export const chatApi = {
  threads: () => api.get('/chat/threads'),
  getOrCreateThread: (childId: string, educatorId: string, parentUserId?: string) =>
    api.get('/chat/thread', { params: { childId, educatorId, parentUserId } }),
  messages: (threadId: string, page = 0, size = 30) =>
    api.get(`/chat/threads/${threadId}/messages`, { params: { page, size } }),
  send: (threadId: string, content: string) =>
    api.post(`/chat/threads/${threadId}/messages`, { content }),
  markRead: (threadId: string) => api.patch(`/chat/threads/${threadId}/read`),
}
